package rule

import io.qameta.allure.Allure
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.openqa.selenium.OutputType
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * On a FAILED test, captures a screenshot, the device logcat, and the UI page
 * source so a red run leaves evidence to triage from instead of just a stacktrace.
 * Every artifact is both saved as a raw file and attached to the failed Allure
 * test, so it is available from the report without browsing the build directory.
 * Wired onto the base test class with `@ExtendWith(ArtifactsOnFailure::class)`.
 *
 * TestExecutionExceptionHandler runs at the point of failure, before @AfterEach
 * calls driver.quit(). After collecting best-effort evidence, this extension
 * always rethrows the original failure so it cannot hide or replace the cause.
 *
 * The driver comes from DriverFactory.current() (set on every createDriver()),
 * so tests don't have to hand it over. Artifacts land in
 * appium-tests/build/reports/failures/ (build/ is gitignored).
 */
class ArtifactsOnFailure : TestExecutionExceptionHandler {
    override fun handleTestExecutionException(
        context: ExtensionContext,
        throwable: Throwable,
    ) {
        runCatching { captureFailureArtifacts(context) }
            .onFailure { println("ArtifactsOnFailure: unexpected capture failure: ${it.message}") }
        throw throwable
    }

    private fun captureFailureArtifacts(context: ExtensionContext) {
        val driver =
            DriverFactory.current()
                ?: run {
                    println("ArtifactsOnFailure: no live Appium session; failure artifacts were not captured")
                    return
                }

        val dir = File("build/reports/failures").apply { mkdirs() }
        val base = context.displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val name = "$base-${System.currentTimeMillis()}"
        val savedFiles = mutableListOf<String>()

        capture(
            label = "screenshot",
            file = File(dir, "$name.png"),
            attachmentName = "Failure screenshot",
            mediaType = "image/png",
            extension = ".png",
            savedFiles = savedFiles,
        ) {
            driver.getScreenshotAs(OutputType.BYTES)
        }

        capture(
            label = "logcat",
            file = File(dir, "$name.logcat"),
            attachmentName = "Device logcat",
            mediaType = "text/plain",
            extension = ".logcat",
            savedFiles = savedFiles,
        ) {
            driver
                .manage()
                .logs()
                .get("logcat")
                .all
                .joinToString("\n") { it.message }
                .toByteArray(StandardCharsets.UTF_8)
        }

        capture(
            label = "page source",
            file = File(dir, "$name.xml"),
            attachmentName = "UI page source",
            mediaType = "application/xml",
            extension = ".xml",
            savedFiles = savedFiles,
        ) {
            driver.pageSource.orEmpty().toByteArray(StandardCharsets.UTF_8)
        }

        if (savedFiles.isEmpty()) {
            println("ArtifactsOnFailure: no raw failure artifacts were saved")
        } else {
            println("ArtifactsOnFailure: saved ${savedFiles.joinToString()} to ${dir.absolutePath}")
        }
    }

    /**
     * Produces an artifact once, then sends the same bytes to both destinations.
     * File persistence and Allure attachment are isolated so one failure does not
     * prevent the other evidence channel, or the remaining artifacts, from working.
     */
    private fun capture(
        label: String,
        file: File,
        attachmentName: String,
        mediaType: String,
        extension: String,
        savedFiles: MutableList<String>,
        produce: () -> ByteArray,
    ) {
        val bytes =
            runCatching(produce).getOrElse {
                println("ArtifactsOnFailure: $label capture failed: ${it.message}")
                return
            }

        runCatching {
            file.writeBytes(bytes)
            savedFiles += file.name
        }.onFailure {
            println("ArtifactsOnFailure: $label file write failed: ${it.message}")
        }

        runCatching {
            ByteArrayInputStream(bytes).use {
                Allure.addAttachment(attachmentName, mediaType, it, extension)
            }
        }.onFailure {
            println("ArtifactsOnFailure: $label Allure attachment failed: ${it.message}")
        }
    }
}
