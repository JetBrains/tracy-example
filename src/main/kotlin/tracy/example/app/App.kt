package tracy.example.app

/**
 * Creates an OpenAI client, runs the greeting agent, and prints the result.
 */
fun main() {
    val client = createOpenAIClient()
    val greeting = runGreetingAgent(client)
    println(greeting)
}
