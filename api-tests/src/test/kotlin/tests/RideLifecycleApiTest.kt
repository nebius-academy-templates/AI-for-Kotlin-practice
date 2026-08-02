package tests

import client.OrdersApi
import client.RidesApi
import client.SandboxApi
import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import model.ErrorResponse
import model.Order
import model.SandboxStateRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.ApiTestCase
import testdata.ApiTestData

@Feature("API: Ride lifecycle")
class RideLifecycleApiTest : ApiTestCase() {
    @Test
    @DisplayName("A completed ride becomes the newest order")
    @AllureId("2001")
    fun testCompletedRideAppearsInHistory() {
        val token = obtainToken()
        var rideId = 0
        lateinit var completedOrder: Order

        step("Order the Yellow tariff: a driver is found") {
            val actual = RidesApi.create(token, ApiTestData.FROM, ApiTestData.TO, ApiTestData.YELLOW_TARIFF.id)
            assertThat(actual.statusCode).isEqualTo(201)
            assertThat(actual.body.status).isEqualTo(ApiTestData.DRIVER_FOUND_STATUS)
            assertThat(actual.body.driver).isEqualTo(ApiTestData.DRIVER)
            rideId = actual.body.id
        }
        step("Complete the active ride") {
            val actual = RidesApi.complete(token, rideId)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.ride.status).isEqualTo(ApiTestData.COMPLETED_STATUS)
            assertThat(actual.body.order.priceCents).isEqualTo(ApiTestData.YELLOW_TARIFF.priceCents)
            completedOrder = actual.body.order
        }
        step("Order history contains the completed ride first") {
            val actual = OrdersApi.orders(token)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.orders).hasSize(ApiTestData.SEEDED_ORDERS.size + 1)
            assertThat(actual.body.orders.first()).isEqualTo(completedOrder)
        }
    }

    @Test
    @DisplayName("A cancelled ride is not added to order history")
    @AllureId("2002")
    fun testCancelledRideDoesNotAppearInHistory() {
        val token = obtainToken()
        var rideId = 0

        step("Order the Yellow tariff") {
            val actual = RidesApi.create(token, ApiTestData.FROM, ApiTestData.TO, ApiTestData.YELLOW_TARIFF.id)
            assertThat(actual.statusCode).isEqualTo(201)
            rideId = actual.body.id
        }
        step("Cancel the active ride") {
            val actual = RidesApi.cancel(token, rideId)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.status).isEqualTo(ApiTestData.CANCELLED_STATUS)
        }
        step("Order history still contains only the three seeded rides") {
            val actual = OrdersApi.orders(token)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.orders).containsExactlyElementsOf(ApiTestData.SEEDED_ORDERS)
        }
    }

    @Test
    @DisplayName("driver_not_found rejects an order without changing history")
    @AllureId("2003")
    fun testDriverNotFoundRejectsOrder() {
        val token = obtainToken()

        step("Enable driver_not_found") {
            val actual = SandboxApi.setState(ApiTestData.DRIVER_NOT_FOUND, true)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body).isEqualTo(SandboxStateRequest(ApiTestData.DRIVER_NOT_FOUND, true))
        }
        step("Order the Yellow tariff: no car can be assigned") {
            val actual = RidesApi.create(token, ApiTestData.FROM, ApiTestData.TO, ApiTestData.YELLOW_TARIFF.id)
            assertThat(actual.statusCode).isEqualTo(409)
            assertThat(actual.error).isEqualTo(ErrorResponse(ApiTestData.DRIVER_NOT_FOUND_ERROR))
        }
        step("No active ride was created") {
            val actual = RidesApi.active(token)
            assertThat(actual.statusCode).isEqualTo(404)
            assertThat(actual.error).isEqualTo(ErrorResponse(ApiTestData.NO_ACTIVE_RIDE_ERROR))
        }
        step("Order history still contains only the three seeded rides") {
            val actual = OrdersApi.orders(token)
            assertThat(actual.statusCode).isEqualTo(200)
            assertThat(actual.body.orders).containsExactlyElementsOf(ApiTestData.SEEDED_ORDERS)
        }
    }
}
