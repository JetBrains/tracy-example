[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-SDK-f5a800?logo=opentelemetry)](https://opentelemetry.io)
[![OpenAI Java](https://img.shields.io/badge/openai--java-4.16.0-412991)](https://github.com/openai/openai-java)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![JDK](https://img.shields.io/badge/JDK-17%2B-orange?logo=openjdk)](https://adoptium.net)

# Tracy Example — Raw OpenTelemetry SDK

> The same OpenAI tool-calling agent from the [`main`](../../tree/main) branch, re-instrumented by hand using the raw OpenTelemetry Java SDK instead of Tracy.
>
> **This branch exists to show what you are giving up when you skip Tracy.**

---

## Quickstart

```bash
git clone https://github.com/JetBrains/tracy-example.git
cd tracy-example
git checkout otel-sdk-trace

export OPENAI_API_KEY=sk-...
export LANGFUSE_PUBLIC_KEY=pk-lf-...  # optional
export LANGFUSE_SECRET_KEY=sk-lf-...  # optional

./gradlew run
```

Langfuse credentials are optional — if not provided, traces will be exported to the console instead.

---

## What changed vs `main`

|                               | `main` (Tracy)       | this branch (raw OTel SDK)          |
|-------------------------------|----------------------|-------------------------------------|
| Instrumentation files added   | **0**                | **3** new files                     |
| Files touched to add tracing  | **2**                | **5**                               |
| Lines of instrumentation code | **~10**              | **~150**                            |
| New AI client support         | `instrument(client)` | new instrumentation file per client |

---

## Why raw OTel SDK is significantly harder

### 1. Far more code to read and maintain

Tracy instruments the `main` branch with a handful of lines across 2 files.
This branch required **3 entirely new files** and changes to 2 more:

**Tracy (`main`)** — the complete instrumentation surface:
```kotlin
// Setup.kt
TracingManager.setSdk(sdk)
instrument(client) // wraps the OpenAI client

// Tool.kt — one annotation covers every tool
@Trace("Tool Execution", metadataCustomizer = ToolMetadataCustomizer::class)
fun execute(): T

// GreetingAgent.kt
withSpan("Greeting Agent") { ... }
```

**Raw OTel (this branch)** — new files introduced just for tracing:
- [`Telemetry.kt`](src/main/kotlin/tracy/example/app/Telemetry.kt) — SDK init, exporter config, Basic-auth encoding, shutdown hook
- [`OpenAIInstrumentation.kt`](src/main/kotlin/tracy/example/app/OpenAIInstrumentation.kt) — span lifecycle, all `gen_ai.*` attributes, prompt/completion serialization
- [`ToolInstrumentation.kt`](src/main/kotlin/tracy/example/app/ToolInstrumentation.kt) — separate traced wrapper around the tool dispatcher

Every span is opened, populated, and closed manually. Every attribute key is a plain string literal you can misspell. Every forgotten `span.end()` silently leaks a span.

---

### 2. No annotations — tracing logic cannot be written once

With Tracy you annotate the interface method **once**; every implementation is automatically traced without touching it. A `metadataCustomizer` lets you control span names, record inputs and outputs, and attach any extra attributes — defined in one place, applied everywhere:

```kotlin
interface Tool<T> {
    @Trace("Tool Execution", metadataCustomizer = ToolMetadataCustomizer::class)
    fun execute(): T
}
```

With raw OTel there is no concept of customizers. If you want a custom span name, recorded input, recorded output, or error handling on a tool, you write the full span lifecycle by hand — and repeat it for every tool you add. See [`ToolInstrumentation.kt`](src/main/kotlin/tracy/example/app/ToolInstrumentation.kt) for what that looks like: ~25 lines of boilerplate per traced function.

---

### 3. Every AI client needs its own instrumentation

Tracy ships a ready-made adapter for each supported provider:

```kotlin
instrument(openAiClient)
instrument(anthropicClient)
```

With raw OTel you write and maintain a separate instrumentation file for each client.
This branch only covers OpenAI. Supporting Anthropic means a new file like
[`OpenAIInstrumentation.kt`](src/main/kotlin/tracy/example/app/OpenAIInstrumentation.kt),
new model-specific attribute mappings, and a new surface to keep in sync as each SDK evolves.

---

### 4. Media content serialization is your problem

LLM conversations regularly include images, audio, tool result blobs, and mixed content arrays.
Tracy handles all content types automatically according to the semantic conventions.

With raw OTel you own every content type. Even the simple text-only case in this branch requires a hand-rolled serializer:

```kotlin
val contentJson = msg.content().joinToString(prefix = "[", postfix = "]") { c ->
    when {
        c.isInputText() ->
            """{"type":"input_text","text":${jsonString(c.asInputText().text())}}"""
        else -> """{"type":"unknown"}"""   // every other content type ignored
    }
}
```

Add image or audio inputs and this code must grow for every client you support.

---

### 5. LLM client conventions are your problem too

The [OpenTelemetry Gen AI semantic conventions](https://opentelemetry.io/docs/specs/semconv/gen-ai/)
define dozens of attributes (`gen_ai.request.model`, `gen_ai.usage.input_tokens` etc.).
Getting them right and keeping them correct as conventions evolve is ongoing manual work:

```kotlin
span.setAttribute("gen_ai.system", "openai")
span.setAttribute("gen_ai.request.model", "gpt-4o-mini")
span.setAttribute("gen_ai.operation.name", "response")
span.setAttribute("gen_ai.api_base", "https://api.openai.com")
// 10 more attributes to populate after the response arrives
```

Tracy applies the correct conventions automatically for each supported client.

---

### 6. Toggling tracing on/off requires custom code

Tracy exposes a single runtime flag with no restart required:

```kotlin
TracingManager.isTracingEnabled = false
```

With raw OTel there is no built-in toggle. This branch implements one manually:

```kotlin
val otel: OpenTelemetry =
    if (System.getenv("TRACING_ENABLED")?.lowercase() == "false") OpenTelemetry.noop()
    else initTelemetry()
```

It requires a process restart to take effect, and you must ensure every code path
that creates a span receives the right `OpenTelemetry` instance.

---

### 7. Kotlin coroutines can silently drop span context

OTel stores the active span in a **thread-local**. If two `suspend` functions are traced and a coroutine resumes on a different thread between them, the second span loses its parent:

```kotlin
suspend fun first(tracer: Tracer) {
    val span = tracer.spanBuilder("first").startSpan()
    span.makeCurrent()   // thread-local on thread A
    second()             // suspends, may resume on thread B
}

suspend fun second(tracer: Tracer) {
    // thread-local is gone; this span has no parent in the trace
    val span = tracer.spanBuilder("second").startSpan()
}
```

No compile error, no warning, just a broken trace. You must manually carry the context across every suspension point with `asContextElement()`.

With Tracy, annotate and forget — context flows through coroutines automatically:

```kotlin
@Trace("first")
suspend fun first() {
    delay(1)   // may switch threads — context is preserved
    second()   // second is always a child of first in the trace
}

@Trace("second")
suspend fun second() { ... }
```
