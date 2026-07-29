package client

import io.restassured.RestAssured.given
import model.SandboxSnapshot
import model.SandboxStateRequest
import model.StatusResponse

/**
 * Control plane of the sandbox states, the HTTP counterpart of the adb
 * broadcast in the mobile suite (ConditionControl). A 200 from setState only
 * means the flag flipped: tests must verify the observable effect on the data
 * endpoints, the same verification rule used for the broadcast.
 */
object SandboxApi {
    fun setState(
        condition: String,
        enabled: Boolean,
    ): ApiResponse<SandboxStateRequest> =
        given()
            .spec(ApiSpec.request)
            .jsonBody(
                SandboxStateRequest(condition, enabled),
                SandboxStateRequest.serializer(),
            ).post("/sandbox/state")
            .toApiResponse(SandboxStateRequest.serializer())

    fun reset(): ApiResponse<StatusResponse> =
        given()
            .spec(ApiSpec.request)
            .post("/sandbox/reset")
            .toApiResponse(StatusResponse.serializer())

    fun snapshot(): ApiResponse<SandboxSnapshot> =
        given()
            .spec(ApiSpec.request)
            .get("/sandbox/state")
            .toApiResponse(SandboxSnapshot.serializer())
}
