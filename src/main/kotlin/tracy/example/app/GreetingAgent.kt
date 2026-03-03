package tracy.example.app

import com.openai.client.OpenAIClient
import com.openai.models.responses.Response
import com.openai.models.responses.ResponseFunctionToolCall
import com.openai.models.responses.ResponseInputItem
import com.openai.models.responses.ResponseOutputMessage
import io.opentelemetry.api.trace.Tracer

/** Calls GPT-4o-mini in a loop, executing tools until the model returns a text reply. */
fun runGreetingAgent(client: OpenAIClient, tracer: Tracer): String {
    var greeting = ""
    val conversation = buildInitialConversation()

    while (greeting.isEmpty()) {
        val response = callModel(client, conversation, tracer)
        greeting = processResponse(response, conversation, tracer)
    }

    return greeting
}

/** Builds the opening message that tells the model to produce a greeting. */
private fun buildInitialConversation(): MutableList<ResponseInputItem> {
    val prompt = """
        Create a warm, personal greeting for the user that includes their name and the current date and time.
        Be creative and vary your greetings - don't use the same format every time.
        Make it feel friendly and welcoming, as if greeting a colleague or friend.
        Always mention the user's name, today's date, and current time in a natural way.
        Use the available tools to get the user's name and current date/time information.
    """.trimIndent()

    return mutableListOf(
        ResponseInputItem.ofMessage(
            ResponseInputItem.Message
                .builder()
                .role(ResponseInputItem.Message.Role.USER)
                .addInputTextContent(prompt)
                .build()
        )
    )
}

/**
 * Processes each item in the model's response:
 * - Tool call  → execute the tool and append its result to [conversation]
 * - Text reply → return it as the final greeting
 *
 * Returns the greeting text, or an empty string if the model is still calling tools.
 */
private fun processResponse(response: Response, conversation: MutableList<ResponseInputItem>, tracer: Tracer): String {
    for (outputItem in response.output()) {
        when {
            outputItem.isFunctionCall() -> handleToolCall(outputItem.asFunctionCall(), conversation, tracer)
            outputItem.isMessage() -> {
                val text = extractText(outputItem.asMessage())
                if (text.isNotEmpty()) return text
            }
        }
    }
    return ""
}

/**
 * Executes the tool the model requested and records both the call and its result
 * in [conversation] so the model can read what the tool returned.
 */
private fun handleToolCall(
    functionCall: ResponseFunctionToolCall,
    conversation: MutableList<ResponseInputItem>,
    tracer: Tracer,
) {
    conversation.add(ResponseInputItem.ofFunctionCall(functionCall))

    val result = executeToolWithTrace(functionCall, tracer)

    conversation.add(
        ResponseInputItem.ofFunctionCallOutput(
            ResponseInputItem.FunctionCallOutput
                .builder()
                .callId(functionCall.callId())
                .outputAsJson(result)
                .build()
        )
    )
}

/** Extracts the plain text from a model message output item. */
private fun extractText(message: ResponseOutputMessage): String =
    message.content().firstOrNull { it.isOutputText() }?.asOutputText()?.text() ?: ""
