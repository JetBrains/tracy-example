package tracy.example.app

import com.openai.models.responses.ResponseFunctionToolCall
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer

/** Dispatches a tool call by name and returns the result. */
fun executeTool(name: String): Any = when (name) {
    "GetUserName" -> GetUserName().execute()
    "GetCurrentDateTime" -> GetCurrentDateTime().execute()
    else -> mapOf("error" to "Unknown tool: $name")
}

/**
 * Create [Span] for every tool invocation
 * Wraps [executeTool] in a "Tool: {name}" span.
 */
fun executeToolWithTrace(functionCall: ResponseFunctionToolCall, tracer: Tracer): Any {
    val name = functionCall.name()
    val span = tracer.spanBuilder("Tool: $name")
        .setAttribute("code.function.name", "tracy.example.app.$name.execute")
        .setAttribute("input", functionCall.arguments())
        .startSpan()
    val scope = span.makeCurrent()
    try {
        val result = executeTool(name)
        span.setAttribute("output", result.toString())
        return result
    } catch (e: Exception) {
        span.setStatus(StatusCode.ERROR, e.message ?: "error")
        span.recordException(e)
        throw e
    } finally {
        scope.close()
        span.end()
    }
}
