package rule

import java.io.File

/**
 * Writes one assignment log per emulator during a parallel run.
 *
 * JUnit XML and Allure own pass/fail. This small, shell-readable evidence only
 * proves which tests the Kotlin device pool assigned to each emulator without
 * interleaving concurrent green events in the console.
 */
object DeviceExecutionEvidence {
    private val writeLock = Any()

    fun record(
        udid: String,
        testName: String,
    ) {
        val outputDir = System.getProperty("appium.device.evidence.dir") ?: return
        val safeUdid = udid.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val singleLineName = testName.replace(Regex("\\s+"), " ").trim()

        synchronized(writeLock) {
            val directory = File(outputDir)
            check(directory.mkdirs() || directory.isDirectory) {
                "Cannot create device evidence directory: $directory"
            }
            File(directory, "$safeUdid.log")
                .appendText("$singleLineName${System.lineSeparator()}", Charsets.UTF_8)
        }
    }
}
