package client

import io.restassured.RestAssured.given
import model.OtpRequest
import model.PhoneRequest
import model.StatusResponse
import model.TokenResponse

/** Endpoint catalog for /auth. */
object AuthApi {
    fun requestOtp(phone: String): ApiResponse<StatusResponse> =
        given()
            .spec(ApiSpec.request)
            .jsonBody(PhoneRequest(phone), PhoneRequest.serializer())
            .post("/auth/phone")
            .toApiResponse(StatusResponse.serializer())

    fun verifyOtp(
        phone: String,
        code: String,
    ): ApiResponse<TokenResponse> =
        given()
            .spec(ApiSpec.request)
            .jsonBody(OtpRequest(phone, code), OtpRequest.serializer())
            .post("/auth/otp")
            .toApiResponse(TokenResponse.serializer())

    /**
     * POST /auth/phone with the given raw string as the body. The shared spec
     * still declares JSON, so the server runs its JSON parser on whatever the
     * test sends; lets tests probe how a malformed body is handled.
     */
    fun requestPhoneRaw(rawBody: String): ApiResponse<StatusResponse> =
        given()
            .spec(ApiSpec.request)
            .body(rawBody)
            .post("/auth/phone")
            .toApiResponse(StatusResponse.serializer())
}
