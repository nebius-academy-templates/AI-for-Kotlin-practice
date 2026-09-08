package rule

import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.openqa.selenium.json.Json
import java.io.File

/**
 * Writes `build/reports/digests/test_log_{method}_{try}.txt` after a failure.
 * The increasing try suffix preserves evidence across repair attempts.
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
        val artifacts = findArtifacts(context.displayName)

        val digest =
            buildString {
                appendLine("test_log $name try $tryNumber")
                appendLine("display : ${context.displayName}")
                appendLine("class   : ${context.testClass.map { it.name }.orElse("?")}")
                appendLine()
                appendLine("FAILURE : ${FailureDigestFormatter.failure(failure)}")
                appendLine()
                appendLine("--- project stack frames ---")
                appendLine(projectFrames(failure))
                appendLine()
                appendLine("--- failure artifacts ---")
                appendLine("screenshot  : ${artifactPath(artifacts.screenshot)}")
                appendLine("logcat      : ${artifactPath(artifacts.logcat)}")
                appendLine("page source : ${artifactPath(artifacts.pageSource)}")
                appendLine()
                appendLine("--- visible UI resource IDs ---")
                appendLine(visibleResourceIds(artifacts.pageSource))
                appendLine()
                appendLine("--- logcat slice ---")
                appendLine(logcatSlice(artifacts.logcat))
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

    /** Finds the newest screenshot/logcat/page-source trio for this display name. */
    private fun findArtifacts(displayName: String): FailureArtifacts {
        val base = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val directory = File("build/reports/failures")
        val newest =
            directory
                .listFiles { file ->
                    file.name.startsWith("$base-") &&
                        file.extension in setOf("png", "logcat", "xml")
                }.orEmpty()
                .maxByOrNull { it.lastModified() }
                ?: return FailureArtifacts()
        val stem = newest.nameWithoutExtension
        return FailureArtifacts(
            screenshot = directory.resolve("$stem.png").takeIf(File::isFile),
            logcat = directory.resolve("$stem.logcat").takeIf(File::isFile),
            pageSource = directory.resolve("$stem.xml").takeIf(File::isFile),
        )
    }

    private fun artifactPath(file: File?): String = file?.absoluteFile?.invariantSeparatorsPath ?: "(missing)"

    private fun visibleResourceIds(pageSource: File?): String {
        if (pageSource == null) return "(no page source: ArtifactsOnFailure left no XML for this test)"
        val ids =
            Regex("""resource-id="([^"]+)"""")
                .findAll(pageSource.readText())
                .map { it.groupValues[1] }
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
                .toList()
        return if (ids.isEmpty()) "(no resource IDs in page source)" else ids.joinToString("\n")
    }

    // Reuse the captured file instead of fetching logcat from the driver
    // again: Appium drains the device log buffer on read.
    private fun logcatSlice(logcatFile: File?): String {
        if (logcatFile == null) return "(no logcat: ArtifactsOnFailure left no file for this test)"

        return FailureDigestFormatter.logcatSlice(logcatFile.readLines())
    }

    private data class FailureArtifacts(
        val screenshot: File? = null,
        val logcat: File? = null,
        val pageSource: File? = null,
    )
}

/** Formats compact digest details without discarding the original evidence. */
object FailureDigestFormatter {
    private const val MAX_LOGCAT_LINES = 120
    private const val TAIL_LOGCAT_LINES = 40
    private const val MAX_LINE_CHARS = 240
    private const val APPIUM_RESPONSE = "AppiumResponse:"
    private const val PNG_BASE64_PREFIX = "iVBOR"
    private const val TRUNCATION_MARKER = " …[truncated middle]… "
    private val pngScreenshotValue = Regex("\"value\"\\s*:\\s*\"$PNG_BASE64_PREFIX")
    private val findElement = Regex("""method:\s*'([^']+)',\s*selector:\s*'([^']+)'""")
    private val verboseOrDebug = Regex("""^\S+\s+\S+\s+\d+\s+\d+\s+[VD]\s+""")

    fun failure(failure: Throwable): String {
        val type = failure.javaClass.simpleName.ifBlank { failure.javaClass.name }
        val message =
            failure.message
                ?.lineSequence()
                ?.map(String::trim)
                ?.firstOrNull(String::isNotEmpty)
        return if (message == null) type else "$type: $message"
    }

    fun logcatSlice(lines: List<String>): String {
        // Anchor only on a crash of the app under test: the uiautomator2
        // server also dies with a FATAL EXCEPTION on session teardown, and
        // that line is exactly the loud-but-irrelevant kind. The crashing
        // process is named on the log line right below the FATAL header.
        val fatalAt =
            lines.indices.lastOrNull { index ->
                lines[index].contains("FATAL EXCEPTION") &&
                    lines.getOrNull(index + 1)?.contains("Process: com.sandbox.qa") == true
            } ?: -1
        val selected =
            if (fatalAt >= 0) {
                lines.drop(fatalAt).take(MAX_LOGCAT_LINES)
            } else {
                lines.takeLast(TAIL_LOGCAT_LINES)
            }

        val summarized = selected.mapNotNull(::summarize)
        val repetitions =
            summarized
                .mapNotNull(SummarizedLine::deduplicationKey)
                .groupingBy { it }
                .eachCount()
        val emitted = mutableSetOf<String>()

        return summarized
            .mapNotNull { line ->
                val key = line.deduplicationKey
                if (key != null && !emitted.add(key)) return@mapNotNull null

                val count = key?.let(repetitions::get) ?: 0
                val suffix = if (count > 1) " [repeated $count times]" else ""
                truncateMiddle(line.text + suffix)
            }.joinToString("\n")
    }

    private fun summarize(line: String): SummarizedLine? {
        val find = findElement.find(line)
        if (find != null) {
            val method = find.groupValues[1]
            val selector = find.groupValues[2]
            return SummarizedLine(
                text = "Appium find: $method=$selector",
                deduplicationKey = "appium-find:$method:$selector",
            )
        }
        if (isFindElementNoise(line)) return null
        if (isExternalVerboseOrDebug(line)) return null

        val markerAt = line.indexOf(APPIUM_RESPONSE)
        if (markerAt < 0) return SummarizedLine(line)

        val payload = line.substring(markerAt + APPIUM_RESPONSE.length).trim()
        if (pngScreenshotValue.containsMatchIn(payload)) return null

        val response =
            runCatching {
                Json().toType<Map<String, Any?>>(payload, Json.MAP_TYPE)
            }.getOrNull()
        val value = response?.get("value")
        val details = (value as? Map<*, *>) ?: response
        val error = details?.get("error") as? String
        if (error != null) {
            val message =
                (details["message"] as? String)
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
            val text =
                if (message.isNullOrBlank()) {
                    "Appium error: $error"
                } else {
                    "Appium error: $error — $message"
                }
            return SummarizedLine(
                text = text,
                deduplicationKey = "appium-error:$error:$message",
            )
        }

        return SummarizedLine(
            text = line,
            deduplicationKey = "appium-response:$payload",
        )
    }

    private fun isFindElementNoise(line: String): Boolean {
        if (!line.contains("appium", ignoreCase = true)) return false

        return line.contains("FindElement command") ||
            (line.contains("Waiting up to") && line.contains("for the device to idle")) ||
            (line.contains("channel read: POST /session/") && line.trimEnd().endsWith("/element"))
    }

    private fun isExternalVerboseOrDebug(line: String): Boolean =
        verboseOrDebug.containsMatchIn(line) &&
            !line.contains("appium", ignoreCase = true) &&
            !line.contains("com.sandbox.qa")

    private fun truncateMiddle(line: String): String {
        if (line.length <= MAX_LINE_CHARS) return line

        val available = MAX_LINE_CHARS - TRUNCATION_MARKER.length
        val headLength = available * 2 / 3
        val tailLength = available - headLength
        return line.take(headLength) + TRUNCATION_MARKER + line.takeLast(tailLength)
    }

    private data class SummarizedLine(
        val text: String,
        val deduplicationKey: String? = null,
    )
}
