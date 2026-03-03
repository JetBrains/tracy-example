package tracy.example.app

import com.openai.client.OpenAIClient
import com.openai.models.ChatModel
import com.openai.models.responses.Response
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseInputItem
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer

/**
 * Sends [conversation] to the model and returns its response.
 * Creates an "OpenAI-generation" span with gen_ai.* semantic convention attributes,
 * including full prompt and completion content.
 */
fun callModel(client: OpenAIClient, conversation: List<ResponseInputItem>, tracer: Tracer): Response {
    val span = tracer.spanBuilder("OpenAI-generation")
        .setAttribute("gen_ai.system", "openai")
        .setAttribute("gen_ai.request.model", "gpt-4o-mini")
        .setAttribute("gen_ai.operation.name", "response")
        .setAttribute("gen_ai.request.max_tokens", 2048L)
        .setAttribute("gen_ai.api_base", "https://api.openai.com")
        .startSpan()
    val scope = span.makeCurrent()
    try {
        setPromptAttributes(span, conversation)

        val response = client.responses().create(
            ResponseCreateParams.builder().model(ChatModel.GPT_4O_MINI).maxOutputTokens(2048)
                .addTool(GetUserName::class.java).addTool(GetCurrentDateTime::class.java)
                .input(ResponseCreateParams.Input.ofResponse(conversation)).build()
        )

        span.setAttribute("http.status_code", 200L)
        span.setAttribute("gen_ai.response.model", response.model().toString())
        span.setAttribute("gen_ai.response.id", response.id())
        response.usage().ifPresent { usage ->
            span.setAttribute("gen_ai.usage.input_tokens", usage.inputTokens())
            span.setAttribute("gen_ai.usage.output_tokens", usage.outputTokens())
        }
        setCompletionAttributes(span, response)

        return response
    } catch (e: Exception) {
        span.setStatus(StatusCode.ERROR, e.message ?: "error")
        span.recordException(e)
        throw e
    } finally {
        scope.close()
        span.end()
    }
}

/** Records each conversation turn as gen_ai.prompt.{i}.* attributes on [span]. */
private fun setPromptAttributes(span: Span, conversation: List<ResponseInputItem>) {
    conversation.forEachIndexed { i, item ->
        when {
            item.isMessage() -> {
                val msg = item.asMessage()
                span.setAttribute("gen_ai.prompt.$i.role", msg.role().toString().lowercase())
                val contentJson = msg.content().joinToString(prefix = "[", postfix = "]") { c ->
                    when {
                        c.isInputText() ->
                            """{"type":"input_text","text":${jsonString(c.asInputText().text())}}"""
                        else -> """{"type":"unknown"}"""
                    }
                }
                span.setAttribute("gen_ai.prompt.$i.content", contentJson)
            }
            item.isFunctionCall() -> {
                val call = item.asFunctionCall()
                span.setAttribute("gen_ai.prompt.$i.tool_call_type", "function_call")
                span.setAttribute("gen_ai.prompt.$i.tool_name", call.name())
                span.setAttribute("gen_ai.prompt.$i.tool_arguments", call.arguments())
                span.setAttribute("gen_ai.prompt.$i.tool_call_id", call.callId())
            }
            item.isFunctionCallOutput() -> {
                val out = item.asFunctionCallOutput()
                span.setAttribute("gen_ai.prompt.$i.tool_call_type", "function_call_output")
                span.setAttribute("gen_ai.prompt.$i.tool_call_id", out.callId())
                span.setAttribute("gen_ai.prompt.$i.output", out.output().toString())
            }
        }
    }
}

/** Records each response output item as gen_ai.completion.{i}.* attributes on [span]. */
private fun setCompletionAttributes(span: Span, response: Response) {
    response.output().forEachIndexed { i, item ->
        when {
            item.isMessage() -> {
                val msg = item.asMessage()
                span.setAttribute("gen_ai.completion.$i.role", msg._role().toString().lowercase())
                val text = msg.content().firstOrNull { it.isOutputText() }?.asOutputText()?.text() ?: ""
                span.setAttribute("gen_ai.completion.$i.content", text)
            }
            item.isFunctionCall() -> {
                val call = item.asFunctionCall()
                span.setAttribute("gen_ai.completion.$i.tool_call_type", "function_call")
                span.setAttribute("gen_ai.completion.$i.tool_name", call.name())
                span.setAttribute("gen_ai.completion.$i.tool_arguments", call.arguments())
                span.setAttribute("gen_ai.completion.$i.tool_call_id", call.callId())
            }
        }
    }
}

/** Escapes [value] as a JSON string literal including the surrounding quotes. */
private fun jsonString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")}\""
