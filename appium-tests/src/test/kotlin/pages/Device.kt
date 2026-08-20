package pages

import io.appium.java_client.android.nativekey.AndroidKey
import io.appium.java_client.android.nativekey.KeyEvent
import rule.DriverFactory

/**
 * Device-level UI primitives that are not tied to one element.
 * Same model as [Element]: resolves the current thread's driver per call,
 * holds no state.
 */
object Device {
    /**
     * Hides the soft keyboard if it is shown. The hide call itself is allowed
     * to fail quietly (it throws when no keyboard is present, which is fine),
     * but a missing driver is a real error and is not swallowed.
     */
    fun hideKeyboard() {
        val driver =
            DriverFactory.current()
                ?: error("Cannot hide keyboard: no active Appium session")
        runCatching { driver.hideKeyboard() }
    }

    /**
     * Presses the system Back key. A device navigation event, not an in-app
     * back button - which is why it lives here and not on a page catalog.
     */
    fun pressBack() {
        val driver =
            DriverFactory.current()
                ?: error("Cannot press Back: no active Appium session")
        driver.pressKey(KeyEvent(AndroidKey.BACK))
    }

    /** Pulls the current scrollable screen downward to trigger pull-to-refresh. */
    fun pullDownToRefresh() {
        val driver =
            DriverFactory.current()
                ?: error("Cannot pull to refresh: no active Appium session")
        val size = driver.manage().window().size
        driver.executeScript(
            "mobile: swipeGesture",
            mapOf(
                "left" to size.width / 10,
                "top" to size.height / 6,
                "width" to size.width * 8 / 10,
                "height" to size.height * 2 / 3,
                "direction" to "down",
                "percent" to 0.85,
            ),
        )
    }
}
