package client

import io.restassured.specification.RequestSpecification
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

internal fun <T> RequestSpecification.jsonBody(
    value: T,
    serializer: SerializationStrategy<T>,
): RequestSpecification = body(Json.encodeToString(serializer, value))
