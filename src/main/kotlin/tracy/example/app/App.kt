package tracy.example.app

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk

/**
 * Creates an OpenAI client, runs the greeting agent inside a root trace span, and prints the result.
 * Set TRACING_ENABLED=false to skip telemetry initialisation and use a no-op tracer instead.
 */
fun main() {
    val otel: OpenTelemetrySdk = initTelemetry()
    val tracer = otel.appTracer()
    val client = createOpenAIClient()
    val span = tracer.spanBuilder("greeting-agent").startSpan()
    val scope = span.makeCurrent()
    try {
        val greeting = runGreetingAgent(client, tracer)
        span.setAttribute("output", greeting)
        println(greeting)
    } finally {
        scope.close()
        span.end()
    }
    otel.sdkTracerProvider.forceFlush()
    Thread.sleep(5000)
}
