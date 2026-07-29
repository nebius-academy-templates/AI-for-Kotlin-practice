package tests

import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.AppiumTestCase
import rule.ConditionControl
import testdata.TestData

@Feature("Order history")
class OrderHistoryStateTest : AppiumTestCase() {
    @Test
    @DisplayName("Backend error shows the order history error")
    @AllureId("1008")
    fun testBackendErrorShowsHistoryError() {
        step("Start on the map (authorized)") {
            map.awaitReady()
        }
        step("Enable backend_error") {
            ConditionControl.enable("backend_error")
        }
        step("Open Order history from the drawer") {
            drawer.openOrders()
        }
        step("Assert the inline history error") {
            orders.assertHistoryError(TestData.BACKEND_ERROR_MESSAGE)
        }
    }
}
