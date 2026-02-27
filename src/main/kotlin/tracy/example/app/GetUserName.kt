package tracy.example.app

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * Returns the name of the current OS user via the `user.name` system property.
 * Falls back to `"User"` if the property is not set.
 */
@JsonClassDescription("Gets the current user's name from the system")
data class GetUserName(
    @field:JsonPropertyDescription("Set this to true to get the user name")
    val retrieve: Boolean = true
) : Tool<GetUserName.Result> {
    data class Result(val name: String)

    override fun execute(): Result {
        val name = System.getProperty("user.name") ?: "User"
        return Result(name)
    }
}
