package rule

import client.ApiSession
import client.ApiSpec
import client.AuthApi
import client.SandboxApi
import io.qameta.allure.Allure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import testdata.ApiTestData

/** Per-test API session isolation and Allure steps. */
abstract class ApiTestCase {
    @BeforeEach
    fun resetSandboxStates() {
        ApiSession.start()
        val response =
            runCatching { SandboxApi.reset() }
                .getOrElse { e ->
                    ApiSession.clear()
                    throw IllegalStateException(
                        "fake-api is not reachable at ${ApiSpec.baseUri}. Start it first: ./gradlew :fake-api:run",
                        e,
                    )
                }
        check(response.statusCode == 200) { "Sandbox reset failed: HTTP ${response.statusCode}" }
    }

    @AfterEach
    fun releaseSandboxSession() {
        if (!ApiSession.isStarted()) {
            return
        }
        try {
            val response = SandboxApi.reset()
            check(response.statusCode == 200) { "Sandbox reset failed: HTTP ${response.statusCode}" }
        } finally {
            ApiSession.clear()
        }
    }

    /** Full happy-path login; the returned bearer token opens the data endpoints. */
    protected fun obtainToken(): String =
        AuthApi
            .verifyOtp(ApiTestData.PHONE, ApiTestData.VALID_OTP)
            .body
            .token

    /**
     * Wraps [body] as a named Allure step, same reporter as the mobile suite:
     * the testcase reads like a scenario and the report shows a node per step.
     */
    protected fun step(
        name: String,
        body: () -> Unit,
    ) = Allure.step(name, Allure.ThrowableRunnableVoid { body() })
}
