package actions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import pages.Device
import pages.DriverSignupPage
import pages.MapPage
import testdata.TestData

/**
 * Steps and checks for the classic View "Become a driver" signup screen.
 * Stateless singleton, same model as the page catalogs: no constructor,
 * no test-runtime dependency; UI primitives come from Element and Device.
 */
object DriverActions {
    /**
     * Opens the side drawer and taps "Become a driver".
     * retryClick guards the tap against transient drawer-transition misses.
     * Screen readiness = the signup title is visible.
     */
    fun openDriverSignup() {
        MapPage.menuButton.click()
        MapPage.drawerBecomeDriverButton.retryClick(message = "could not open Become a driver from the drawer")
        DriverSignupPage.title.waitFor()
    }

    /** Submits the untouched form and asserts the inline validation error. */
    fun submitEmptyFormShowsError() {
        DriverSignupPage.submitButton.click()
        assertEquals(
            TestData.DRIVER_SIGNUP_EMPTY_ERROR,
            DriverSignupPage.errorText.waitFor().text,
            "Empty driver signup form should show the validation error",
        )
    }

    /** Fills both fields, submits, and asserts the success message. */
    fun submitFilledFormShowsSuccess(
        name: String,
        car: String,
    ) {
        DriverSignupPage.nameInput.sendKeys(name)
        DriverSignupPage.carInput.sendKeys(car)
        Device.hideKeyboard()
        DriverSignupPage.submitButton.click()
        assertEquals(
            TestData.DRIVER_SIGNUP_SUCCESS,
            DriverSignupPage.successText.waitFor().text,
            "Filled driver signup form should show the success message",
        )
    }

    /**
     * Fills both fields with input the submit should reject (it trims whitespace,
     * so whitespace-only counts as empty) and asserts the validation error.
     */
    fun submitFormShowsValidationError(
        name: String,
        car: String,
    ) {
        DriverSignupPage.nameInput.sendKeys(name)
        DriverSignupPage.carInput.sendKeys(car)
        Device.hideKeyboard()
        DriverSignupPage.submitButton.click()
        assertEquals(
            TestData.DRIVER_SIGNUP_EMPTY_ERROR,
            DriverSignupPage.errorText.waitFor().text,
            "Rejected driver signup input should show the validation error",
        )
    }

    /**
     * Fills both fields and submits after an earlier failed attempt: the success
     * message shows and the validation error leaves (they toggle exclusively).
     */
    fun submitSuccessClearsError(
        name: String,
        car: String,
    ) {
        submitFilledFormShowsSuccess(name, car)
        assertTrue(
            DriverSignupPage.errorText.waitForGone(),
            "Validation error should disappear after a successful submit",
        )
    }

    /**
     * Taps Back and asserts we returned to the map: the signup screen is gone
     * and the plain ride form is visible. The drawer closes when "Become a
     * driver" is picked, so it must NOT be on screen after the return.
     */
    fun backReturnsToMap() {
        DriverSignupPage.backButton.click()
        DriverSignupPage.title.waitForGone()
        MapPage.destinationField.waitFor(20)
    }
}
