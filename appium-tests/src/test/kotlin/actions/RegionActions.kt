package actions

import org.junit.jupiter.api.Assertions.assertTrue
import pages.MapPage
import rule.ConditionControl

/** Drives the region_unavailable sandbox state and asserts the banner's observable effect. */
object RegionActions {
    fun showRegionUnavailable() {
        ConditionControl.enable("region_unavailable")
        assertTrue(
            MapPage.regionBanner.waitFor().isDisplayed,
            "Region banner should appear when region_unavailable is enabled",
        )
    }

    fun hideRegionUnavailable() {
        ConditionControl.disable("region_unavailable")
        assertTrue(
            MapPage.regionBanner.waitForGone(),
            "Region banner should disappear after disabling the state",
        )
    }
}
