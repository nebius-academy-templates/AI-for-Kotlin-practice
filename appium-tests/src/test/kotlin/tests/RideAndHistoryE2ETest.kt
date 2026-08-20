package tests

import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.AppiumTestCase
import testdata.TestData

/**
 * E2E written purely as a sequence of action-backed steps - the testcase reads like
 * a scenario, with all the "how" in actions/ and pages/, and each step in the report.
 */
@Feature("E2E")
class RideAndHistoryE2ETest : AppiumTestCase() {
    @Test
    @DisplayName("Search a ride then check order history")
    @AllureId("1003")
    fun testSearchRideThenCheckHistory() {
        step("Start on the map (authorized)") {
            map.awaitReady()
        }
        step("Search a destination") {
            map.searchDestination(TestData.DESTINATION)
        }
        step("Assert the Yellow tariff price") {
            map.assertRidePrice(1, TestData.YELLOW_PRICE_ON_MAP)
        }
        step("Open order history") {
            drawer.openOrders()
        }
        step("Assert order prices") {
            orders.assertHistoryPrices(TestData.PAST_ORDERS)
        }
    }
}
