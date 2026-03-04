package tracy.example.app

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import java.util.Base64

private const val TRACER_NAME = "tracy.example.app"

/**
 * Initializes OpenTelemetry and connects it to Langfuse for trace export.
 * If Langfuse keys are not provided, traces will be exported to the console instead.
 */
fun initTelemetry(): OpenTelemetrySdk {
    val langfusePublicKey = System.getenv("LANGFUSE_PUBLIC_KEY")
    val langfuseSecretKey = System.getenv("LANGFUSE_SECRET_KEY")

    val resource = Resource.getDefault().merge(
        Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "tracy-example"))
    )

    val spanProcessor = if (langfusePublicKey != null && langfuseSecretKey != null) {
        val host = System.getenv("LANGFUSE_URL") ?: "https://cloud.langfuse.com"
        val credentials = Base64.getEncoder().encodeToString("$langfusePublicKey:$langfuseSecretKey".toByteArray(Charsets.UTF_8))
        val tracesEndpoint = "${host.trimEnd('/')}/api/public/otel/v1/traces"

        val exporter = OtlpHttpSpanExporter.builder()
            .setEndpoint(tracesEndpoint)
            .addHeader("Authorization", "Basic $credentials")
            .build()

        BatchSpanProcessor.builder(exporter).build()
    } else {
        System.err.println("Warning: LANGFUSE_PUBLIC_KEY and LANGFUSE_SECRET_KEY are not set. Traces will be exported to console.")
        SimpleSpanProcessor.create(LoggingSpanExporter.create())
    }

    val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(spanProcessor)
        .setResource(resource)
        .build()

    val sdk = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .build()

    Runtime.getRuntime().addShutdownHook(Thread { sdk.shutdown() })

    return sdk
}

fun OpenTelemetry.appTracer(): Tracer = getTracer(TRACER_NAME)
