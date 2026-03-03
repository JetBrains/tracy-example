package tracy.example.app

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import java.util.Base64

private const val TRACER_NAME = "tracy.example.app"

fun initTelemetry(): OpenTelemetrySdk {
    val host = System.getenv("LANGFUSE_URL") ?: "https://cloud.langfuse.com"
    val publicKey = System.getenv("LANGFUSE_PUBLIC_KEY") ?: error("LANGFUSE_PUBLIC_KEY env var is not set")
    val secretKey = System.getenv("LANGFUSE_SECRET_KEY") ?: error("LANGFUSE_SECRET_KEY env var is not set")

    val credentials = Base64.getEncoder().encodeToString("$publicKey:$secretKey".toByteArray(Charsets.UTF_8))
    val tracesEndpoint = "${host.trimEnd('/')}/api/public/otel/v1/traces"

    val exporter = OtlpHttpSpanExporter.builder()
        .setEndpoint(tracesEndpoint)
        .addHeader("Authorization", "Basic $credentials")
        .build()

    val resource = Resource.getDefault().merge(
        Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "tracy-example"))
    )

    val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
        .setResource(resource)
        .build()

    val sdk = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .build()

    Runtime.getRuntime().addShutdownHook(Thread { sdk.shutdown() })

    return sdk
}

fun OpenTelemetry.appTracer(): Tracer = getTracer(TRACER_NAME)
