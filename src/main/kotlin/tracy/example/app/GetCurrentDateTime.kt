package tracy.example.app

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Returns the current local date and time.
 * Date is formatted as `"MMMM d"` (e.g. `"February 27"`), time as `"HH:mm"`.
 */
@JsonClassDescription("Gets the current date and time")
data class GetCurrentDateTime(
    @field:JsonPropertyDescription("Set this to true to get the current date and time") val retrieve: Boolean = true
) : Tool<GetCurrentDateTime.Result> {
    data class Result(
        @field:JsonPropertyDescription("The current date in format 'Month Day'")
        val date: String,
        @field:JsonPropertyDescription("The current time in format 'HH:mm'")
        val time: String
    )

    override fun execute(): Result {
        val now = LocalDateTime.now()
        return Result(
            date = now.format(DateTimeFormatter.ofPattern("MMMM d")),
            time = now.format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }
}
