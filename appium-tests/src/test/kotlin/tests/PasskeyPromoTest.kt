package tests

import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.AppiumTestCase

@Feature("Onboarding")
class PasskeyPromoTest : AppiumTestCase() {
    override val startAuthorized = false

    @Test
    @DisplayName("Skip on the passkey promo continues to location onboarding")
    @AllureId("1004")
    fun testSkipContinuesToGeo() {
        step("Sign in to the passkey promo") {
            onboarding.reachPasskey()
        }
        step("Assert the promo title copy") {
            onboarding.assertPasskeyPromoTitle()
        }
        step("Skip continues to location onboarding") {
            onboarding.skipPasskeyToGeo()
        }
    }

    @Test
    @DisplayName("Create a passkey continues to location onboarding")
    @AllureId("1005")
    fun testCreatePasskeyContinuesToGeo() {
        step("Sign in to the passkey promo") {
            onboarding.reachPasskey()
        }
        step("Create a passkey continues to location onboarding") {
            onboarding.createPasskeyToGeo()
        }
    }
}
