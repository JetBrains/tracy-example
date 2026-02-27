package tracy.example.app

import org.jetbrains.ai.tracy.core.instrumentation.Trace
import org.jetbrains.ai.tracy.core.instrumentation.customizers.PlatformMethod
import org.jetbrains.ai.tracy.core.instrumentation.customizers.SpanMetadataCustomizer

/**
 * Tells Tracy to label each tool's trace span name as "Tool: <ClassName>",
 * e.g. "Tool: GetUserName".
 */
object ToolMetadataCustomizer : SpanMetadataCustomizer {
    override fun resolveSpanName(method: PlatformMethod, args: Array<Any?>): String =
        "Tool: ${method.declaringClass.simpleName}"
}

/**
 * Every tool the AI can call must implement this interface.
 * Tracy automatically creates a trace span each time [execute] is called.
 */
interface Tool<T> {
    @Trace("Tool Execution", metadataCustomizer = ToolMetadataCustomizer::class)
    fun execute(): T
}
