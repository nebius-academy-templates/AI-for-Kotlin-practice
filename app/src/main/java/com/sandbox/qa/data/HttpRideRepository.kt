package com.sandbox.qa.data

import com.sandbox.qa.condition.ConditionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Real auth and ride HTTP client of the deterministic Ktor backend used by the emulator. */
class HttpRideRepository(
    private val baseUrl: String,
    private val sandboxSessionId: String,
    private val tokenProvider: () -> String? = { null },
    private val onTokenChanged: (String?) -> Unit = {},
) : RideRepository,
    AuthRepository {
    @Volatile
    private var testAuthenticationEnabled: Boolean = false

    override suspend fun requestOtp(phone: String) {
        ioCall {
            request(
                "POST",
                "/auth/phone",
                JSONObject().put("phone", phone),
            ).requireSuccess()
        }
    }

    override suspend fun verifyOtp(
        phone: String,
        code: String,
    ) {
        ioCall {
            val body =
                request(
                    "POST",
                    "/auth/otp",
                    JSONObject().put("phone", phone).put("code", code),
                ).requireSuccess()
            onTokenChanged(body.requiredToken())
        }
    }

    /**
     * Enables token bootstrap only for the Appium `authenticated=true` launch
     * seam. Normal users never fall back to the sandbox test account.
     */
    fun enableTestAuthentication() {
        testAuthenticationEnabled = true
    }

    override suspend fun requestGeolocation(): String =
        sandboxCall {
            authorizedRequest("POST", "/location/resolve").getString("location")
        }

    override suspend fun getRideOptions(
        from: String,
        to: String,
    ): List<RideOption> =
        sandboxCall {
            val path = "/rides/options?from=${encode(from)}&to=${encode(to)}"
            authorizedRequest("GET", path)
                .getJSONArray("options")
                .mapObjects(::rideOption)
        }

    override suspend fun getOrders(): List<Order> =
        sandboxCall {
            authorizedRequest("GET", "/orders")
                .getJSONArray("orders")
                .mapObjects(::order)
        }

    override suspend fun getActiveRide(): ActiveRide? =
        sandboxCall {
            val response = authorizedResult("GET", "/rides/active")
            when (response.status) {
                HttpURLConnection.HTTP_OK -> activeRide(response.body)
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> response.throwFailure()
            }
        }

    override suspend fun createRide(
        from: String,
        to: String,
        rideOptionId: Int,
    ): ActiveRide =
        sandboxCall {
            authorizedRequest(
                "POST",
                "/rides",
                JSONObject()
                    .put("from", from)
                    .put("to", to)
                    .put("rideOptionId", rideOptionId),
            ).let(::activeRide)
        }

    override suspend fun completeRide(rideId: Int): Order =
        sandboxCall {
            order(authorizedRequest("POST", "/rides/$rideId/complete").getJSONObject("order"))
        }

    override suspend fun cancelRide(rideId: Int): ActiveRide =
        sandboxCall {
            activeRide(authorizedRequest("POST", "/rides/$rideId/cancel"))
        }

    override suspend fun resetSandbox() {
        ioCall {
            request("POST", "/sandbox/reset").requireSuccess()
            onTokenChanged(null)
        }
    }

    private suspend fun <T> ioCall(block: () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: ApiException) {
                throw e
            } catch (e: Exception) {
                throw ApiException(
                    "API is unavailable at $baseUrl. Start the local fake-api on port 8080.",
                    e,
                )
            }
        }

    private suspend fun <T> sandboxCall(block: () -> T): T {
        // ConditionConfig is Compose snapshot state. Read it on the main
        // thread, then pass an immutable value into the blocking I/O section.
        val sandboxStates = withContext(Dispatchers.Main.immediate) { ConditionConfig.snapshot() }
        return ioCall {
            syncSandboxStates(sandboxStates)
            block()
        }
    }

    private fun syncSandboxStates(sandboxStates: Map<String, Boolean>) {
        val snapshot = JSONObject()
        sandboxStates.forEach(snapshot::put)
        request("POST", "/sandbox/state/snapshot", snapshot).requireSuccess()
    }

    private fun authorizedRequest(
        method: String,
        path: String,
        body: JSONObject? = null,
    ): JSONObject = authorizedResult(method, path, body).requireSuccess()

    private fun authorizedResult(
        method: String,
        path: String,
        body: JSONObject? = null,
    ): HttpResult {
        val activeToken =
            tokenProvider()
                ?: if (testAuthenticationEnabled) {
                    obtainTestToken()
                } else {
                    throw ApiException("Authentication required")
                }
        var response = request(method, path, body, activeToken)
        if (response.status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            onTokenChanged(null)
            if (testAuthenticationEnabled) {
                response = request(method, path, body, obtainTestToken())
            }
        }
        return response
    }

    /** Issues credentials for the explicit Appium entrance, never for a normal user session. */
    private fun obtainTestToken(): String {
        val body =
            request(
                "POST",
                "/auth/otp",
                JSONObject()
                    .put("phone", SandboxContract.TEST_PHONE_NUMBER)
                    .put("code", SandboxContract.VALID_OTP),
            ).requireSuccess()
        val token = body.requiredToken()
        onTokenChanged(token)
        return token
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        bearerToken: String? = null,
    ): HttpResult {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(SANDBOX_SESSION_HEADER, sandboxSessionId)
            bearerToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return HttpResult(status, if (text.isBlank()) JSONObject() else JSONObject(text))
        } finally {
            connection.disconnect()
        }
    }

    private fun rideOption(json: JSONObject) =
        RideOption(
            id = json.getInt("id"),
            name = json.getString("name"),
            seats = json.getInt("seats"),
            price = json.getInt("price"),
            category = json.getString("category"),
            available = json.optBoolean("available", true),
        )

    private fun order(json: JSONObject) =
        Order(
            id = json.getInt("id"),
            date = json.getString("date"),
            from = json.getString("from"),
            to = json.getString("to"),
            price = json.getInt("price"),
        )

    private fun activeRide(json: JSONObject) =
        ActiveRide(
            id = json.getInt("id"),
            status = json.getString("status"),
            from = json.getString("from"),
            to = json.getString("to"),
            option = rideOption(json.getJSONObject("option")),
            driver =
                json.getJSONObject("driver").let { driver ->
                    Driver(
                        name = driver.getString("name"),
                        car = driver.getString("car"),
                        plate = driver.getString("plate"),
                        etaMinutes = driver.getInt("etaMinutes"),
                    )
                },
        )

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private data class HttpResult(
        val status: Int,
        val body: JSONObject,
    ) {
        fun requireSuccess(): JSONObject {
            if (status !in 200..299) {
                throwFailure()
            }
            return body
        }

        fun throwFailure(): Nothing {
            val error = body.optString("error", "Request failed")
            if (status == HttpURLConnection.HTTP_CONFLICT && error == SandboxContract.NO_CARS_FOUND_ERROR) {
                throw DriverNotFoundException(error)
            }
            if (
                status == HttpURLConnection.HTTP_CONFLICT &&
                body.optString("code") == SandboxContract.ACTIVE_RIDE_EXISTS_CODE
            ) {
                throw ActiveRideExistsException(error)
            }
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED && error == SandboxContract.INVALID_OTP_ERROR) {
                throw ApiException(error)
            }
            throw ApiException("HTTP $status: $error")
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 25_000
        private const val SANDBOX_SESSION_HEADER = "X-Sandbox-Session"
    }
}

private fun JSONObject.requiredToken(): String =
    optString("token").takeIf(String::isNotBlank)
        ?: throw ApiException("Authentication response is missing token")

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = List(length()) { index -> transform(getJSONObject(index)) }
