package client

import io.restassured.RestAssured.given
import model.OrdersResponse

/** Endpoint catalog for /orders. A null token means "send the request without auth". */
object OrdersApi {
    fun orders(token: String?): ApiResponse<OrdersResponse> {
        val request = given().spec(ApiSpec.request)
        token?.let { request.header("Authorization", "Bearer $it") }
        return request
            .get("/orders")
            .toApiResponse(OrdersResponse.serializer())
    }
}
