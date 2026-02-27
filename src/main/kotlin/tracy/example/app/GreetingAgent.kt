package tracy.example.app

import com.openai.client.OpenAIClient
import com.openai.models.ChatModel
import com.openai.models.responses.Response
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseFunctionToolCall
import com.openai.models.responses.ResponseInputItem
import com.openai.models.responses.ResponseOutputMessage
/** Calls GPT-4o-mini in a loop, executing tools until the model returns a text reply. */
fun runGreetingAgent(client: OpenAIClient): String {
    var greeting = ""

    val conversation = buildInitialConversation()

    while (greeting.isEmpty()) {
        val response = callModel(client, conversation)
        greeting = processResponse(response, conversation)
    }

    return greeting
}

/** Builds the opening message that tells the model to produce greeting. */
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
            ResponseInputItem.Message.builder().role(ResponseInputItem.Message.Role.USER).addInputTextContent(prompt)
                .build()
        )
    )
}

/** Sends the current conversation to the model and returns its response. */
private fun callModel(client: OpenAIClient, conversation: List<ResponseInputItem>): Response =
    client.responses().create(
        ResponseCreateParams.builder().model(ChatModel.GPT_4O_MINI).maxOutputTokens(2048)
            .addTool(GetUserName::class.java).addTool(GetCurrentDateTime::class.java)
            .input(ResponseCreateParams.Input.ofResponse(conversation)).build()
    )

/**
 * Processes each item in the model's response:
 * - Tool call  → execute the tool and append its result to [conversation]
 * - Text reply → return it as the final greeting
 *
 * Returns the greeting text, or an empty string if the model is still calling tools.
 */
private fun processResponse(response: Response, conversation: MutableList<ResponseInputItem>): String {
    for (outputItem in response.output()) {
        when {
            outputItem.isFunctionCall() -> handleToolCall(outputItem.asFunctionCall(), conversation)
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
    functionCall: ResponseFunctionToolCall, conversation: MutableList<ResponseInputItem>
) {
    conversation.add(ResponseInputItem.ofFunctionCall(functionCall))

    val result = executeTool(functionCall.name())

    conversation.add(
        ResponseInputItem.ofFunctionCallOutput(
            ResponseInputItem.FunctionCallOutput.builder().callId(functionCall.callId()).outputAsJson(result).build()
        )
    )
}

/** Dispatches a tool call by name and returns the result. */
private fun executeTool(name: String): Any = when (name) {
    "GetUserName" -> GetUserName().execute()
    "GetCurrentDateTime" -> GetCurrentDateTime().execute()
    else -> mapOf("error" to "Unknown tool: $name")
}

/** Extracts the plain text from a model message output item. */
private fun extractText(message: ResponseOutputMessage): String =
    message.content().firstOrNull { it.isOutputText() }?.asOutputText()?.text() ?: ""
