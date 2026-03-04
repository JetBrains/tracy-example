package tracy.example.app

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import org.jetbrains.ai.tracy.core.TracingManager
import org.jetbrains.ai.tracy.core.configureOpenTelemetrySdk
import org.jetbrains.ai.tracy.core.exporters.ConsoleExporterConfig
import org.jetbrains.ai.tracy.core.exporters.langfuse.LangfuseExporterConfig
import org.jetbrains.ai.tracy.openai.clients.instrument

/**
 * Initializes Tracy and connects it to Langfuse for trace export.
 * If Langfuse keys are not provided, traces will be exported to the console instead.
 */
fun setupTracing() {
    val langfusePublicKey = System.getenv("LANGFUSE_PUBLIC_KEY")
    val langfuseSecretKey = System.getenv("LANGFUSE_SECRET_KEY")

    val exporterConfig = if (langfusePublicKey != null && langfuseSecretKey != null) {
        LangfuseExporterConfig(
            langfusePublicKey = langfusePublicKey,
            langfuseSecretKey = langfuseSecretKey,
        )
    } else {
        System.err.println("Warning: LANGFUSE_PUBLIC_KEY and LANGFUSE_SECRET_KEY are not set. Traces will be exported to console.")
        ConsoleExporterConfig()
    }

    val sdk = configureOpenTelemetrySdk(exporterConfig = exporterConfig)

    TracingManager.apply {
        setSdk(sdk)
        isTracingEnabled = true
        traceSensitiveContent()
    }
}

/**
 * Creates an OpenAI client from the OPENAI_API_KEY env var and wraps it with Tracy.
 */
fun createOpenAIClient(): OpenAIClient = OpenAIOkHttpClient.fromEnv().also { instrument(it) }
