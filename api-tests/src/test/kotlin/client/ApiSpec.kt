package client

import io.qameta.allure.restassured.AllureRestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import java.util.UUID

/**
 * Shared request spec for every client: base URI from the api.url system
 * property (defaults to the local fake-api server), JSON content type, and
 * the Allure filter that attaches each request and response to the report.
 */
object ApiSpec {
    val baseUri: String = System.getProperty("api.url") ?: "http://localhost:8080"

    val request: RequestSpecification
        get() =
            RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .addHeader("X-Sandbox-Session", ApiSession.current())
                .addFilter(AllureRestAssured())
                .build()
}

/** Per-test backend session, isolated even when JUnit executes tests concurrently. */
object ApiSession {
    private val current = ThreadLocal<String>()

    fun start() {
        current.set("api-${UUID.randomUUID()}")
    }

    fun current(): String = current.get() ?: error("ApiSession.start() must run before an API request")

    fun isStarted(): Boolean = current.get() != null

    fun clear() {
        current.remove()
    }
}
