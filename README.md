[![OpenAI Java](https://img.shields.io/badge/openai--java-4.16.0-412991)](https://github.com/openai/openai-java)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![JDK](https://img.shields.io/badge/JDK-17%2B-orange?logo=openjdk)](https://adoptium.net)

# Tracy Example — plain app (no instrumentation)

> OpenAI tool-calling agent **without** Tracy or Langfuse

This branch shows the baseline app with no observability added.
Switch to the [`main`](../../tree/main) branch to see the same app fully instrumented with Tracy + Langfuse.

---

## Quickstart

```bash
git clone https://github.com/JetBrains/tracy-example.git
cd tracy-example
git checkout without-tracy

export OPENAI_API_KEY=sk-...

./gradlew run
```

---

## What it does

Asks GPT-4o-mini to greet you. The model calls two tools to get your name and the current time, then writes the greeting.

---

## Structure

| File                                                                               |                     |
|------------------------------------------------------------------------------------|---------------------|
| [`App.kt`](src/main/kotlin/tracy/example/app/App.kt)                               | `main()`            |
| [`Setup.kt`](src/main/kotlin/tracy/example/app/Setup.kt)                           | OpenAI client       |
| [`Tool.kt`](src/main/kotlin/tracy/example/app/Tool.kt)                             | `Tool<T>` interface |
| [`GetUserName.kt`](src/main/kotlin/tracy/example/app/GetUserName.kt)               | tool: OS username   |
| [`GetCurrentDateTime.kt`](src/main/kotlin/tracy/example/app/GetCurrentDateTime.kt) | tool: date & time   |
| [`GreetingAgent.kt`](src/main/kotlin/tracy/example/app/GreetingAgent.kt)           | agent loop          |
