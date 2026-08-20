package pages

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By
import org.openqa.selenium.ElementClickInterceptedException
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.WebDriverWait
import rule.DriverFactory
import java.time.Duration

/**
 * A single UI element addressed by its Compose testTag (exposed to UiAutomator2 as
 * resource-id: the app sets testTagsAsResourceId = true on the root surface).
 *
 * Every operation resolves the element fresh through the current thread's driver
 * (DriverFactory.current(), already a ThreadLocal), so Element handles are stateless,
 * never stale, and safe to keep as vals on singleton page objects. Every operation
 * waits for visibility first, except
 * [isPresent], which is the deliberate no-wait probe.
 */
class Element(
    val testTag: String,
    private val defaultTimeoutSec: Long = 10,
) {
    private val driver: AndroidDriver
        get() = requireNotNull(DriverFactory.current()) { "No active Appium session" }

    private fun by(): By = AppiumBy.id(testTag)

    private fun locate(timeoutSec: Long): WebElement =
        WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
            .until(ExpectedConditions.visibilityOfElementLocated(by()))

    fun waitFor(timeoutSec: Long = defaultTimeoutSec): WebElement = locate(timeoutSec)

    /** Returns true once the element is invisible/absent; throws on timeout. */
    fun waitForGone(timeoutSec: Long = defaultTimeoutSec): Boolean =
        WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
            .until(ExpectedConditions.invisibilityOfElementLocated(by()))

    fun click(timeoutSec: Long = defaultTimeoutSec) {
        locate(timeoutSec).click()
    }

    /**
     * Clicks with a retry loop for the transient failures a click can hit
     * mid-transition (element not there yet, went stale, or briefly covered).
     * Deliberately narrow: only those three exception types are retried, so
     * real failures (assertion errors, dead session, interrupts) surface
     * immediately instead of being retried into a timeout.
     */
    fun retryClick(
        timeoutSec: Long = defaultTimeoutSec,
        message: String,
    ) {
        FluentWait(Unit)
            .withTimeout(Duration.ofSeconds(timeoutSec))
            .pollingEvery(Duration.ofMillis(300))
            .withMessage(message)
            .ignoring(NoSuchElementException::class.java)
            .ignoring(StaleElementReferenceException::class.java)
            .ignoring(ElementClickInterceptedException::class.java)
            .until {
                driver.findElement(by()).click()
                true
            }
    }

    fun sendKeys(
        text: CharSequence,
        timeoutSec: Long = defaultTimeoutSec,
    ) {
        locate(timeoutSec).sendKeys(text)
    }

    /** Clears the field's current value (behind the same visibility wait as every operation). */
    fun clear(timeoutSec: Long = defaultTimeoutSec) {
        locate(timeoutSec).clear()
    }

    val text: String
        get() = locate(defaultTimeoutSec).text

    /** Cheap, non-blocking presence check (NO wait) - for polling loops. */
    fun isPresent(): Boolean = driver.findElements(by()).isNotEmpty()
}
