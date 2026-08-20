package client

import io.restassured.RestAssured.given
import model.ActiveRide
import model.CreateRideRequest
import model.RideCompletionResponse
import model.RideOptionsResponse

/** Endpoint catalog for /rides. Null token or route parts mean "send the request without them". */
object RidesApi {
    fun options(
        token: String?,
        from: String?,
        to: String?,
    ): ApiResponse<RideOptionsResponse> {
        val request = given().spec(ApiSpec.request)
        token?.let { request.header("Authorization", "Bearer $it") }
        from?.let { request.queryParam("from", it) }
        to?.let { request.queryParam("to", it) }
        return request
            .get("/rides/options")
            .toApiResponse(RideOptionsResponse.serializer())
    }

    fun active(token: String?): ApiResponse<ActiveRide> {
        val request = given().spec(ApiSpec.request)
        token?.let { request.header("Authorization", "Bearer $it") }
        return request
            .get("/rides/active")
            .toApiResponse(ActiveRide.serializer())
    }

    fun create(
        token: String?,
        from: String,
        to: String,
        rideOptionId: Int,
    ): ApiResponse<ActiveRide> {
        val request = given().spec(ApiSpec.request)
        token?.let { request.header("Authorization", "Bearer $it") }
        return request
            .jsonBody(
                CreateRideRequest(from, to, rideOptionId),
                CreateRideRequest.serializer(),
            ).post("/rides")
            .toApiResponse(ActiveRide.serializer())
    }

    fun complete(
        token: String?,
        rideId: Int,
    ): ApiResponse<RideCompletionResponse> {
        val request = given().spec(ApiSpec.request)
        token?.let { request.header("Authorization", "Bearer $it") }
        return request
            .post("/rides/$rideId/complete")
            .toApiResponse(RideCompletionResponse.serializer())
    }

    fun cancel(
        token: String?,
        rideId: Int,
    ): ApiResponse<ActiveRide> {
        val request = given().spec(ApiSpec.request)
        token?.let { request.header("Authorization", "Bearer $it") }
        return request
            .post("/rides/$rideId/cancel")
            .toApiResponse(ActiveRide.serializer())
    }
}
