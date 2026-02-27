package tracy.example.app

/**
 * Initializes tracing, runs the greeting agent, and prints the result.
 */
fun main() {
    setupTracing()
    val client = createOpenAIClient()
    val greeting = runGreetingAgent(client)
    println(greeting)
}
