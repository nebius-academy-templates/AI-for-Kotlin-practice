package actions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import pages.Device
import pages.GeoPage
import pages.MapPage
import pages.OtpPage
import pages.PasskeyPage
import pages.PhoneLoginPage
import testdata.TestData

/** Auth + onboarding steps. */
object OnboardingActions {
    /** Valid phone + OTP, skip passkey, enable geo → lands on the map screen. */
    fun completeUntilMap() {
        reachOtp()
        submitValidOtpToPasskey()
        skipPasskeyToGeo()
        enableLocationToMap()
    }

    /** Enters a wrong OTP and asserts the inline error is shown. */
    fun loginWithWrongOtp() {
        PhoneLoginPage.title.waitFor()
        PhoneLoginPage.phoneInput.sendKeys(TestData.PHONE)
        Device.hideKeyboard()
        PhoneLoginPage.continueButton.click()

        OtpPage.title.waitFor()
        OtpPage.codeInput.sendKeys(TestData.WRONG_OTP)
        Device.hideKeyboard()
        OtpPage.confirmButton.click()
        assertEquals(TestData.WRONG_OTP_ERROR, OtpPage.errorLabel.text)
    }

    /** Enters a valid [phone] (>= 8 digits) and taps Next; lands on the OTP screen. */
    fun reachOtp(phone: String = TestData.PHONE) {
        PhoneLoginPage.title.waitFor()
        PhoneLoginPage.phoneInput.sendKeys(phone)
        Device.hideKeyboard()
        PhoneLoginPage.continueButton.click()
        OtpPage.title.waitFor()
    }

    /** Valid phone + OTP; lands on the passkey promo. */
    fun reachPasskey() {
        reachOtp()
        submitValidOtpToPasskey()
    }

    /** Enters the valid OTP and confirms; lands on the passkey promo. */
    fun submitValidOtpToPasskey() {
        OtpPage.codeInput.sendKeys(TestData.VALID_OTP)
        Device.hideKeyboard()
        OtpPage.confirmButton.click()
        PasskeyPage.title.waitFor()
    }

    /** Valid phone + OTP, skip passkey; lands on location onboarding. */
    fun reachGeo() {
        reachPasskey()
        PasskeyPage.skipButton.click()
        GeoPage.title.waitFor()
    }

    /** Submits a too-short [phone] and asserts the inline validation error. */
    fun submitInvalidPhoneShowsError(phone: String) {
        PhoneLoginPage.title.waitFor()
        PhoneLoginPage.phoneInput.sendKeys(phone)
        Device.hideKeyboard()
        PhoneLoginPage.continueButton.click()
        assertEquals(TestData.PHONE_VALIDATION_ERROR, PhoneLoginPage.errorLabel.text)
    }

    /** Types a mixed string into the phone field and asserts only its digits were kept. */
    fun assertPhoneKeepsDigitsOnly(
        raw: String,
        expectedDigits: String,
    ) {
        PhoneLoginPage.title.waitFor()
        PhoneLoginPage.phoneInput.sendKeys(raw)
        assertEquals(expectedDigits, PhoneLoginPage.phoneInput.text, "phone field after typing mixed input")
    }

    /** System-backs from the OTP screen and asserts the phone form came back empty. */
    fun backFromOtpResetsPhoneForm() {
        Device.pressBack()
        PhoneLoginPage.title.waitFor()
        assertEquals("", PhoneLoginPage.phoneInput.text, "phone field should reset on re-entry")
    }

    /** Types more digits than the code length; the field caps at "1234", which signs in. */
    fun confirmOverflowOtpSignsIn() {
        OtpPage.codeInput.sendKeys(TestData.OTP_OVERFLOW_INPUT)
        Device.hideKeyboard()
        OtpPage.confirmButton.click()
        PasskeyPage.title.waitFor()
    }

    /** Clears the wrong code, enters the valid one, and confirms; lands on the passkey promo. */
    fun recoverWithValidOtp() {
        OtpPage.codeInput.clear()
        OtpPage.codeInput.sendKeys(TestData.VALID_OTP)
        Device.hideKeyboard()
        OtpPage.confirmButton.click()
        PasskeyPage.title.waitFor()
    }

    /**
     * Asserts the resend button is disabled while the cooldown ticks. The disabled state is
     * the observable lock; the label text lives in a child node of the TextButton container
     * (same Compose semantics as the region banner), so the container's own text is empty
     * and is deliberately not asserted.
     */
    fun assertOtpResendLocked() {
        val resend = OtpPage.resendButton.waitFor()
        assertFalse(resend.isEnabled, "resend should be disabled while the cooldown ticks")
    }

    /** Asserts the passkey promo headline copy. */
    fun assertPasskeyPromoTitle() {
        assertEquals(TestData.PASSKEY_PROMO_TITLE, PasskeyPage.title.text, "passkey promo title")
    }

    /** Taps Skip on the passkey promo; lands on location onboarding. */
    fun skipPasskeyToGeo() {
        PasskeyPage.skipButton.click()
        GeoPage.title.waitFor()
    }

    /** Taps Create a passkey; in the sandbox it continues to location onboarding too. */
    fun createPasskeyToGeo() {
        PasskeyPage.createButton.click()
        GeoPage.title.waitFor()
    }

    /** Starts the location request and asserts the processing overlay covers the screen. */
    fun requestLocationShowsProcessing() {
        GeoPage.enableButton.click()
        GeoPage.processingOverlay.waitFor(5)
    }

    /** Starts the location request and asserts the inline backend error (the user stays on geo). */
    fun requestLocationFailsInline() {
        GeoPage.enableButton.click()
        assertEquals(TestData.BACKEND_ERROR_MESSAGE, GeoPage.errorLabel.text, "geo error message")
    }

    /** Enables location; lands on the map ride form. */
    fun enableLocationToMap() {
        GeoPage.enableButton.click()
        MapPage.destinationField.waitFor(20)
    }

    /** Retries the location request; lands on the map. */
    fun retryLocationReachesMap() {
        enableLocationToMap()
    }
}
