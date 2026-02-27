package tracy.example.app

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient

/**
 * Creates an OpenAI client from the OPENAI_API_KEY env var.
 */
fun createOpenAIClient(): OpenAIClient = OpenAIOkHttpClient.fromEnv()
