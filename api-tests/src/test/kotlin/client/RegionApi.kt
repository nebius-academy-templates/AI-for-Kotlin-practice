package client

import io.restassured.RestAssured.given
import model.RegionStatus

/** Endpoint catalog for /region. */
object RegionApi {
    fun status(): ApiResponse<RegionStatus> =
        given()
            .spec(ApiSpec.request)
            .get("/region/status")
            .toApiResponse(RegionStatus.serializer())
}
