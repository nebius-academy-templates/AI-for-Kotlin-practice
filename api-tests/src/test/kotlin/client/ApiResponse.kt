package client

import io.restassured.response.Response
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import model.ErrorResponse

class ApiResponse<T> internal constructor(
    val statusCode: Int,
    private val successBody: T?,
    val error: ErrorResponse?,
    val responseTimeMs: Long,
) {
    val body: T
        get() = checkNotNull(successBody) { "HTTP $statusCode has no success body" }
}

internal fun <T> Response.toApiResponse(serializer: DeserializationStrategy<T>): ApiResponse<T> {
    val rawBody = asString()
    return if (statusCode in 200..299) {
        ApiResponse(
            statusCode = statusCode,
            successBody = Json.decodeFromString(serializer, rawBody),
            error = null,
            responseTimeMs = time,
        )
    } else {
        ApiResponse(
            statusCode = statusCode,
            successBody = null,
            error = Json.decodeFromString(ErrorResponse.serializer(), rawBody),
            responseTimeMs = time,
        )
    }
}
