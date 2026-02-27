package tracy.example.app

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import org.jetbrains.ai.tracy.core.TracingManager
import org.jetbrains.ai.tracy.core.configureOpenTelemetrySdk
import org.jetbrains.ai.tracy.core.exporters.langfuse.LangfuseExporterConfig
import org.jetbrains.ai.tracy.openai.clients.instrument

/**
 * Initializes Tracy and connects it to Langfuse for trace export.
 */
fun setupTracing() {
    val sdk = configureOpenTelemetrySdk(
        exporterConfig = LangfuseExporterConfig(
            langfusePublicKey = System.getenv("LANGFUSE_PUBLIC_KEY")
                ?: error("LANGFUSE_PUBLIC_KEY environment variable is not set"),
            langfuseSecretKey = System.getenv("LANGFUSE_SECRET_KEY")
                ?: error("LANGFUSE_SECRET_KEY environment variable is not set"),
        )
    )
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
