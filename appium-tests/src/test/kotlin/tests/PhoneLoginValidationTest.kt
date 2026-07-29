package tests

import io.qameta.allure.AllureId
import io.qameta.allure.Feature
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import rule.AppiumTestCase
import testdata.TestData

@Feature("Onboarding")
class PhoneLoginValidationTest : AppiumTestCase() {
    // Phone validation IS the subject here: sessions must start on the login
    // screen, not skip past it with the "authenticated" launch extra.
    override val startAuthorized = false

    @Test
    @DisplayName("Short phone shows a validation error")
    @AllureId("1003")
    fun testShortPhoneShowsValidationError() {
        step("Submit a 7-digit phone, assert the validation error") {
            onboarding.submitInvalidPhoneShowsError(TestData.SHORT_PHONE)
        }
    }

    @Test
    @DisplayName("Phone input keeps digits only")
    @AllureId("1004")
    fun testPhoneInputKeepsDigitsOnly() {
        step("Type mixed input, assert only the digits remain") {
            onboarding.assertPhoneKeepsDigitsOnly(TestData.MIXED_PHONE_INPUT, TestData.MIXED_PHONE_DIGITS)
        }
    }

    @Test
    @DisplayName("Eight digit phone opens the OTP screen")
    @AllureId("1005")
    fun testEightDigitPhoneOpensOtp() {
        step("Submit the 8-digit boundary phone, land on OTP") {
            onboarding.reachOtp(TestData.EIGHT_DIGIT_PHONE)
        }
    }

    @Test
    @DisplayName("Phone form is empty after returning from OTP")
    @AllureId("1006")
    fun testPhoneFormResetsOnReturn() {
        step("Submit a valid phone, land on OTP") {
            onboarding.reachOtp()
        }
        step("System back returns to an empty phone form") {
            onboarding.backFromOtpResetsPhoneForm()
        }
    }
}
