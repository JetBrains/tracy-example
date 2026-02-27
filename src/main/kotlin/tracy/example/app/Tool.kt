package tracy.example.app

/**
 * Every tool the AI can call must implement this interface.
 */
interface Tool<T> {
    fun execute(): T
}
