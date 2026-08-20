package actions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import pages.Device
import pages.MapPage

object MapActions {
    /** Waits for the destination form and pull-to-refresh surface. */
    fun awaitReady() {
        MapPage.pickupField.waitFor(20)
        MapPage.destinationField.waitFor(20)
        MapPage.pullToRefresh.waitFor()
    }

    fun assertPickup(expected: String) {
        assertEquals(expected, MapPage.pickupField.text, "pickup location")
    }

    fun assertDestination(expected: String) {
        assertEquals(expected, MapPage.destinationField.text, "destination")
    }

    fun replacePickup(text: String) {
        MapPage.pickupField.clear()
        MapPage.pickupField.sendKeys(text)
        Device.hideKeyboard()
    }

    /** Types a destination; tariffs load automatically after the input debounce. */
    fun searchDestination(destination: String) {
        awaitReady()
        MapPage.destinationField.sendKeys(destination)
        Device.hideKeyboard()
        MapPage.ridesList.waitFor()
    }

    /** Map prices use the tilde form ("~29.70 €") - assert as a string. */
    fun assertRidePrice(
        rideId: Int,
        expected: String,
    ) {
        assertEquals(expected, MapPage.ridePrice(rideId).text, "ride $rideId price")
    }

    /** Types into the destination field (no search) and hides the keyboard. */
    fun typeDestination(text: String) {
        MapPage.destinationField.sendKeys(text)
        Device.hideKeyboard()
    }

    /** Clears the destination field and types [text] instead. */
    fun replaceDestination(text: String) {
        MapPage.destinationField.clear()
        typeDestination(text)
    }

    /** Types a destination without waiting for the automatic load to finish. */
    fun startSearch(destination: String) {
        awaitReady()
        typeDestination(destination)
    }

    fun assertDestinationHint(expected: String) {
        assertEquals(expected, MapPage.pullHint.text, "tariff refresh hint")
    }

    fun assertFindOffersRemoved() {
        assertFalse(MapPage.removedSearchButton.isPresent(), "Find offers button should not exist")
    }

    fun assertTariffsNotLoaded() {
        assertFalse(MapPage.ridesList.isPresent(), "tariffs should wait for a real destination")
    }

    /** The slow search keeps its spinner visible first; the list must still arrive. */
    fun assertLoadingThenRides() {
        MapPage.ridesLoading.waitFor()
        MapPage.ridesList.waitFor(15)
    }

    /** Asserts the failed search's inline error and that its Retry control is offered. */
    fun assertSearchErrorWithRetry(expectedError: String) {
        assertEquals(expectedError, MapPage.ridesError.text, "rides error message")
        MapPage.ridesRetryButton.waitFor()
    }

    /** Taps Retry; the re-run search renders the rides list. */
    fun retrySearch() {
        MapPage.ridesRetryButton.click()
        MapPage.ridesList.waitFor()
    }

    fun pullToRefresh() {
        Device.pullDownToRefresh()
        // A normal refresh may finish before Appium observes its transient spinner.
        // Callers assert the stable refreshed state; spinner coverage uses slow_backend_response.
        MapPage.ridesList.waitFor(20)
    }

    fun pullToRefreshExpectingError(expectedError: String) {
        Device.pullDownToRefresh()
        assertSearchErrorWithRetry(expectedError)
    }

    /** Asserts each offered tariff: name plus map price (tilde form) per ride id. */
    fun assertTariffs(
        names: Map<Int, String>,
        prices: Map<Int, String>,
    ) {
        names.forEach { (id, name) ->
            assertEquals(name, MapPage.rideName(id).text, "ride $id name")
            assertEquals(prices.getValue(id), MapPage.ridePrice(id).text, "ride $id price")
        }
    }

    /** Asserts one whole ride card is on the rendered list. */
    fun assertRideOptionShown(rideId: Int) {
        MapPage.rideOption(rideId).waitFor()
    }

    /** The unavailable tariff stays on the list but is disabled (grayed); the others stay enabled. */
    fun assertTariffUnavailable(
        rideId: Int,
        remainingNames: Map<Int, String>,
    ) {
        assertFalse(
            MapPage.rideOption(rideId).waitFor().isEnabled,
            "ride $rideId should be disabled while unavailable",
        )
        remainingNames.forEach { (id, name) ->
            assertEquals(name, MapPage.rideName(id).text, "ride $id name")
            assertTrue(MapPage.rideOption(id).waitFor().isEnabled, "ride $id should stay enabled")
        }
    }

    /** The tariff is offered and selectable again (e.g. after the state resets). */
    fun assertTariffAvailable(rideId: Int) {
        assertTrue(MapPage.rideOption(rideId).waitFor().isEnabled, "ride $rideId should be enabled")
    }

    fun selectTariff(rideId: Int) {
        MapPage.rideOption(rideId).click()
        MapPage.rideSelected(rideId).waitFor()
    }

    fun assertOnlyTariffSelected(
        selectedRideId: Int,
        allRideIds: Set<Int>,
    ) {
        MapPage.rideSelected(selectedRideId).waitFor()
        (allRideIds - selectedRideId).forEach { rideId ->
            assertFalse(MapPage.rideSelected(rideId).isPresent(), "ride $rideId should not be selected")
        }
    }

    fun assertNoSelectedTariff(rideIds: Set<Int>) {
        rideIds.forEach { rideId ->
            assertFalse(MapPage.rideSelected(rideId).isPresent(), "ride $rideId should not be selected")
        }
    }

    fun selectAndOrderRide(rideId: Int) {
        MapPage.rideOption(rideId).click()
        MapPage.orderRideButton.click()
        MapPage.driverSearchingTitle.waitFor()
        MapPage.driverFoundTitle.waitFor(20)
    }

    fun selectAndOrderWithoutDriver(
        rideId: Int,
        expectedStatus: String,
    ) {
        MapPage.rideOption(rideId).click()
        MapPage.orderRideButton.click()
        MapPage.driverSearchingTitle.waitFor()
        assertEquals(expectedStatus, MapPage.statusMessage.text, "driver search result")
        MapPage.ridesList.waitFor()
    }

    fun assertDriverFound() {
        assertEquals("Driver found", MapPage.driverFoundTitle.text, "ride lifecycle title")
        MapPage.driverCard.waitFor()
        MapPage.completeRideButton.waitFor()
    }

    fun assertDriverRoute(expected: String) {
        assertEquals(expected, MapPage.driverRoute.text, "driver route")
    }

    fun completeRide(expectedPrice: String) {
        MapPage.completeRideButton.click()
        assertEquals("Ride completed", MapPage.completedTitle.text, "completed ride title")
        assertEquals(expectedPrice, MapPage.completedPrice.text, "completed ride price")
    }

    fun cancelRide() {
        MapPage.cancelRideButton.click()
        assertEquals("Ride cancelled", MapPage.statusMessage.text, "cancelled ride status")
        MapPage.ridesList.waitFor()
    }

    fun bookAnotherRideAndFindDriver() {
        MapPage.bookAnotherButton.click()
        MapPage.driverSearchingTitle.waitFor()
        MapPage.driverFoundTitle.waitFor(20)
    }

    fun returnHomeAfterCompletion() {
        MapPage.backHomeButton.click()
        MapPage.ridesList.waitFor()
    }
}
