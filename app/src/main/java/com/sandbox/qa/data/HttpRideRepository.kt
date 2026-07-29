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

/** Real HTTP client of the deterministic Ktor backend used by the emulator. */
class HttpRideRepository(
    private val baseUrl: String,
    private val sandboxSessionId: String,
) : RideRepository {
    @Volatile
    private var token: String? = null

    override suspend fun requestGeolocation(): String =
        ioCall {
            syncSandboxStates()
            authorizedRequest("POST", "/location/resolve").getString("location")
        }

    override suspend fun getRideOptions(
        from: String,
        to: String,
    ): List<RideOption> =
        ioCall {
            syncSandboxStates()
            val path = "/rides/options?from=${encode(from)}&to=${encode(to)}"
            authorizedRequest("GET", path)
                .getJSONArray("options")
                .mapObjects(::rideOption)
        }

    override suspend fun getOrders(): List<Order> =
        ioCall {
            syncSandboxStates()
            authorizedRequest("GET", "/orders")
                .getJSONArray("orders")
                .mapObjects(::order)
        }

    override suspend fun createRide(
        from: String,
        to: String,
        rideOptionId: Int,
    ): ActiveRide =
        ioCall {
            syncSandboxStates()
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
        ioCall {
            syncSandboxStates()
            order(authorizedRequest("POST", "/rides/$rideId/complete").getJSONObject("order"))
        }

    override suspend fun cancelRide(rideId: Int): ActiveRide =
        ioCall {
            syncSandboxStates()
            activeRide(authorizedRequest("POST", "/rides/$rideId/cancel"))
        }

    override suspend fun resetSandbox() =
        ioCall {
            request("POST", "/sandbox/reset").requireSuccess()
            token = null
            Unit
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

    private fun syncSandboxStates() {
        val snapshot = JSONObject()
        ConditionConfig.snapshot().forEach(snapshot::put)
        request("POST", "/sandbox/state/snapshot", snapshot).requireSuccess()
    }

    private fun authorizedRequest(
        method: String,
        path: String,
        body: JSONObject? = null,
    ): JSONObject {
        var response = request(method, path, body, token ?: obtainToken())
        if (response.status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            token = null
            response = request(method, path, body, obtainToken())
        }
        return response.requireSuccess()
    }

    private fun obtainToken(): String {
        val response =
            request(
                "POST",
                "/auth/otp",
                JSONObject().put("phone", "999999999").put("code", "1234"),
            ).requireSuccess()
        return response.getString("token").also { token = it }
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
                val error = body.optString("error", "Request failed")
                if (status == HttpURLConnection.HTTP_CONFLICT && error == SandboxContract.NO_CARS_FOUND_ERROR) {
                    throw DriverNotFoundException(error)
                }
                throw ApiException("HTTP $status: $error")
            }
            return body
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 25_000
        private const val SANDBOX_SESSION_HEADER = "X-Sandbox-Session"
    }
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = List(length()) { index -> transform(getJSONObject(index)) }
