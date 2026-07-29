package rule

import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

/**
 * On a FAILED test, writes ONE small text digest with everything needed to
 * pick the failure cause without opening the raw artifacts: the assertion
 * message, the project-level stack frames, and a logcat slice. The digest is
 * what an agent (or a human) reads first in a fix loop; the raw artifacts
 * from ArtifactsOnFailure stay the full evidence.
 *
 * Files land in appium-tests/build/reports/digests/ named
 * `test_log_{method}_{try}.txt`. The try number is derived from the files
 * already present, so reruns of the same test within one debugging session
 * accumulate a history (try 1, try 2, ...) that can be diffed between fixes.
 *
 * ArtifactsOnFailure handles the thrown test exception before JUnit invokes
 * after-test-execution callbacks, so this digest can reuse the logcat file it
 * just saved. Fetching logcat from the driver twice is not an option: the
 * driver log buffer drains on read, and a second fetch would return nothing.
 */
class FailureDigest : AfterTestExecutionCallback {
    override fun afterTestExecution(context: ExtensionContext) {
        val failure = context.executionException.orElse(null) ?: return // test passed

        val dir = File("build/reports/digests").apply { mkdirs() }
        val name =
            context.testMethod
                .map { it.name }
                .orElse(context.displayName)
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
        val tryNumber = nextTryNumber(dir, name)

        val digest =
            buildString {
                appendLine("test_log $name try $tryNumber")
                appendLine("display : ${context.displayName}")
                appendLine("class   : ${context.testClass.map { it.name }.orElse("?")}")
                appendLine()
                appendLine("FAILURE : $failure")
                appendLine()
                appendLine("--- project stack frames ---")
                appendLine(projectFrames(failure))
                appendLine()
                appendLine("--- logcat slice ---")
                appendLine(logcatSlice(context.displayName))
            }

        val file = File(dir, "test_log_${name}_$tryNumber.txt")
        runCatching { file.writeText(digest) }
            .onSuccess { println("FailureDigest: wrote ${file.absolutePath}") }
            .onFailure { println("FailureDigest: write failed: ${it.message}") }
    }

    /** try N = max suffix of the existing test_log_{name}_*.txt files, plus one. */
    private fun nextTryNumber(
        dir: File,
        name: String,
    ): Int {
        val prefix = "test_log_${name}_"
        val last =
            dir
                .listFiles { f -> f.name.startsWith(prefix) && f.name.endsWith(".txt") }
                .orEmpty()
                .mapNotNull {
                    it.name
                        .removePrefix(prefix)
                        .removeSuffix(".txt")
                        .toIntOrNull()
                }.maxOrNull() ?: 0
        return last + 1
    }

    /**
     * Only the frames from this module's own layers (tests/actions/pages/rule):
     * the framework frames below them never explain a failure, they just bury it.
     * Falls back to the top of the raw stack when nothing matches (e.g. a
     * failure inside the Appium client before any project code).
     */
    private fun projectFrames(failure: Throwable): String {
        val projectPackages = listOf("tests.", "actions.", "pages.", "rule.", "testdata.")
        val own =
            failure.stackTrace
                .filter { frame -> projectPackages.any { frame.className.startsWith(it) } }
                .map { "  at $it" }
        val frames = own.ifEmpty { failure.stackTrace.take(8).map { "  at $it" } }
        return frames.joinToString("\n")
    }

    /**
     * Reuses the .logcat file ArtifactsOnFailure saved for this test moments
     * ago (see the class KDoc for why it is not re-fetched from the driver).
     * Slices from the last FATAL EXCEPTION when there is one, otherwise keeps
     * the tail: the cause of a UI failure is almost always in the last screens'
     * worth of device log, not in the boot noise above it.
     */
    private fun logcatSlice(displayName: String): String {
        val base = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val logcatFile =
            File("build/reports/failures")
                .listFiles { f -> f.name.startsWith(base) && f.name.endsWith(".logcat") }
                .orEmpty()
                .maxByOrNull { it.lastModified() }
                ?: return "(no logcat: ArtifactsOnFailure left no file for this test)"

        val lines = logcatFile.readLines()
        // Anchor only on a crash of the app under test: the uiautomator2
        // server also dies with a FATAL EXCEPTION on session teardown, and
        // that line is exactly the loud-but-irrelevant kind. The crashing
        // process is named on the log line right below the FATAL header.
        val fatalAt =
            lines.indices.lastOrNull { i ->
                lines[i].contains("FATAL EXCEPTION") &&
                    lines.getOrNull(i + 1)?.contains("Process: com.sandbox.qa") == true
            } ?: -1
        val slice =
            if (fatalAt >= 0) {
                lines.drop(fatalAt).take(MAX_LOGCAT_LINES)
            } else {
                lines.takeLast(TAIL_LOGCAT_LINES)
            }
        // Cap the line length: Appium echoes response payloads (base64
        // screenshots) into logcat, and those characters explain nothing.
        return slice.joinToString("\n") {
            if (it.length > MAX_LINE_CHARS) it.take(MAX_LINE_CHARS) + " …[truncated]" else it
        }
    }

    private companion object {
        const val MAX_LOGCAT_LINES = 120
        const val TAIL_LOGCAT_LINES = 40
        const val MAX_LINE_CHARS = 240
    }
}
