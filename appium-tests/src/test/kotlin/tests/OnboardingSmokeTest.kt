package tests

import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.AppiumTestCase
import testdata.TestData

@Feature("Onboarding")
class OnboardingSmokeTest : AppiumTestCase() {
    // Onboarding IS the subject here: sessions must start on the login
    // screen, not skip past it with the "authenticated" launch extra.
    override val startAuthorized = false

    @Test
    @DisplayName("Full onboarding opens the map")
    @AllureId("1000")
    fun testFullOnboardingOpensMap() {
        step("Valid phone opens OTP") {
            onboarding.reachOtp()
        }
        step("Valid OTP opens passkey promo") {
            onboarding.submitValidOtpToPasskey()
        }
        step("Skip opens location onboarding") {
            onboarding.skipPasskeyToGeo()
        }
        step("Enabling location opens the ride form") {
            onboarding.enableLocationToMap()
        }
        step("Ride form is ready and contains the resolved pickup") {
            map.awaitReady()
            map.assertPickup(TestData.PICKUP)
        }
    }

    @Test
    @DisplayName("Wrong OTP shows an error")
    @AllureId("1001")
    fun testWrongOtpShowsError() {
        step("Enter phone and a wrong OTP, assert the error") {
            onboarding.loginWithWrongOtp()
        }
    }

    @Test
    @DisplayName("Ride options show prices after search")
    @AllureId("1002")
    fun testRideOptionsShowPrices() {
        step("Onboard to the map") {
            onboarding.completeUntilMap()
        }
        step("Search a destination") {
            map.searchDestination(TestData.DESTINATION)
        }
        step("Assert the Yellow tariff price") {
            map.assertRidePrice(1, TestData.YELLOW_PRICE_ON_MAP)
        }
    }
}
