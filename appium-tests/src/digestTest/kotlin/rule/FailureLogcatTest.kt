package rule

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FailureLogcatTest {
    @Test
    fun `keeps only the first meaningful failure line`() {
        val failure =
            IllegalStateException(
                "\nExpected condition failed: id=otp_title\n" +
                    "Build info: this belongs in JUnit and Allure",
            )

        val result = FailureDigestFormatter.failure(failure)

        assertEquals(
            "IllegalStateException: Expected condition failed: id=otp_title",
            result,
        )
    }

    @Test
    fun `summarizes and deduplicates polling errors`() {
        val first = appiumErrorLine("08-17 19:49:28.643", "first stack")
        val second = appiumErrorLine("08-17 19:49:29.181", "second stack")

        val result = FailureDigestFormatter.logcatSlice(listOf("before", first, "between", second))

        assertTrue(
            result.contains(
                "Appium error: no such element — " +
                    "An element could not be located on the page using the given search parameters " +
                    "[repeated 2 times]",
            ),
        )
        assertEquals(1, result.lineSequence().count { it.startsWith("Appium error:") })
        assertFalse(result.contains("stack"))
    }

    @Test
    fun `omits base64 screenshot responses`() {
        val screenshot =
            "08-17 I appium: AppiumResponse: " +
                "{\"sessionId\":\"session\",\"value\":\"iVBOR${"A".repeat(500)}\"}"

        val result = FailureDigestFormatter.logcatSlice(listOf("before", screenshot, "after"))

        assertEquals("before\nafter", result)
    }

    @Test
    fun `deduplicates identical non-error responses`() {
        val payload = "{\"sessionId\":\"session\",\"value\":null}"
        val first = "08-17 19:49:28.643 I appium: AppiumResponse: $payload"
        val second = "08-17 19:49:29.181 I appium: AppiumResponse: $payload"

        val result = FailureDigestFormatter.logcatSlice(listOf(first, second))

        assertEquals(1, result.lineSequence().count())
        assertTrue(result.contains("[repeated 2 times]"))
    }

    @Test
    fun `collapses repeated find-element polling`() {
        val lines =
            listOf(
                "I appium: channel read: POST /session/one/element",
                "I appium: FindElement command",
                "I appium: method: 'id', selector: 'otp_title'",
                "I appium: Waiting up to 10000ms for the device to idle",
                "I appium: channel read: POST /session/one/element",
                "I appium: FindElement command",
                "I appium: method: 'id', selector: 'otp_title'",
                "I appium: Waiting up to 10000ms for the device to idle",
            )

        val result = FailureDigestFormatter.logcatSlice(lines)

        assertEquals("Appium find: id=otp_title [repeated 2 times]", result)
    }

    @Test
    fun `drops external verbose chatter but keeps higher priority and app lines`() {
        val result =
            FailureDigestFormatter.logcatSlice(
                listOf(
                    "08-17 19:49:28.309 726 888 D WifiDataStall: noisy",
                    "08-17 19:49:28.310 726 888 V WifiConfigManager: noisy",
                    "08-17 19:49:28.311 726 888 I Network: useful info",
                    "08-17 19:49:28.312 726 888 W Runtime: useful warning",
                    "08-17 19:49:28.313 726 888 D App: com.sandbox.qa state",
                ),
            )

        assertFalse(result.contains("WifiDataStall"))
        assertFalse(result.contains("WifiConfigManager"))
        assertTrue(result.contains("useful info"))
        assertTrue(result.contains("useful warning"))
        assertTrue(result.contains("com.sandbox.qa state"))
    }

    @Test
    fun `preserves both ends of other long lines`() {
        val line = "important-start-" + "x".repeat(400) + "-important-end"

        val result = FailureDigestFormatter.logcatSlice(listOf(line))

        assertTrue(result.startsWith("important-start-"))
        assertTrue(result.endsWith("-important-end"))
        assertTrue(result.contains("[truncated middle]"))
        assertEquals(240, result.length)
    }

    @Test
    fun `keeps app crash slice behavior`() {
        val lines =
            buildList {
                repeat(50) { add("old-$it") }
                add("FATAL EXCEPTION: main")
                add("Process: com.sandbox.qa, PID: 123")
                add("java.lang.IllegalStateException: broken")
            }

        val result = FailureDigestFormatter.logcatSlice(lines)

        assertFalse(result.contains("old-49"))
        assertTrue(result.startsWith("FATAL EXCEPTION: main"))
        assertTrue(result.endsWith("java.lang.IllegalStateException: broken"))
    }

    private fun appiumErrorLine(
        timestamp: String,
        stacktrace: String,
    ): String =
        "$timestamp I appium: AppiumResponse: " +
            "{\"sessionId\":\"session\",\"value\":{" +
            "\"error\":\"no such element\"," +
            "\"message\":\"An element could not be located on the page " +
            "using the given\\nsearch parameters\"," +
            "\"stacktrace\":\"$stacktrace\"}}"
}
