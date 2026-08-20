package tests

import client.AuthApi
import client.OrdersApi
import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.ApiTestCase
import testdata.ApiTestData

/**
 * The authentication smoke shipped in the Week 1 student template.
 *
 * Broader endpoint coverage is intentionally left for student exercises.
 */
@Feature("API: Smoke")
class ApiSmokeTest : ApiTestCase() {
    @Test
    @DisplayName("Valid phone and OTP issue a token that opens the data endpoints")
    @AllureId("2000")
    fun testValidLoginIssuesWorkingToken() {
        lateinit var token: String
        step("Request an OTP for a valid phone") {
            val actual = AuthApi.requestOtp(ApiTestData.PHONE)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.status).isEqualTo(ApiTestData.OTP_SENT_STATUS)
        }
        step("Exchange the OTP for a bearer token") {
            val actual = AuthApi.verifyOtp(ApiTestData.PHONE, ApiTestData.VALID_OTP)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.token).isNotBlank()
            token = actual.body.token
        }
        step("The token opens the order history") {
            val actual = OrdersApi.orders(token)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.orders).isNotEmpty()
        }
    }
}
