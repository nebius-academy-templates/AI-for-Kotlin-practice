package rule

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Test-side bridge to the app's sandbox state registry ConditionConfig.
 *
 * Two transports, tried in order:
 *
 * 1. Local adb - for tests running next to the emulator, the same channel
 *    documented in the repo README:
 *
 *      adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver \
 *          --es condition <name> --ez enabled true
 *      adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver --ez reset true
 *
 * 2. The live Appium session via mobile: shell - for runners that have no adb
 *    access to the device, such as a remote emulator farm. Requires the server
 *    to run with --allow-insecure=uiautomator2:adb_shell.
 *
 * IMPORTANT: am broadcast reports "Broadcast completed" even when the state was
 * NOT applied - wrong name, app not started and so on. Delivery only proves the
 * command reached a device, never that the state took effect. The OBSERVABLE
 * effect must be asserted by the test, e.g. the region banner showing or hiding.
 * This helper guards only the transport; correctness of the state is the test's job.
 */
object ConditionControl {
    private const val COMPONENT = "com.sandbox.qa/.condition.ConditionReceiver"

    fun enable(condition: String) = broadcast("--es", "condition", condition, "--ez", "enabled", "true")

    fun disable(condition: String) = broadcast("--es", "condition", condition, "--ez", "enabled", "false")

    fun reset() = broadcast("--ez", "reset", "true")

    private fun broadcast(vararg extras: String) {
        val amArgs = listOf("broadcast", "-n", COMPONENT, *extras)
        val adbError = broadcastViaLocalAdb(amArgs) ?: return
        val sessionError = broadcastViaSession(amArgs) ?: return
        error(
            "Cannot reach ConditionReceiver by either transport.\n" +
                "  local adb: $adbError\n" +
                "  appium session mobile: shell: $sessionError",
        )
    }

    /** Returns null on success, otherwise a description of why this transport failed. */
    private fun broadcastViaLocalAdb(amArgs: List<String>): String? {
        val deviceArgs = DriverFactory.currentDeviceUdid()?.let { listOf("-s", it) }.orEmpty()
        val cmd = listOf(adb) + deviceArgs + listOf("shell", "am") + amArgs
        return try {
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            when {
                !process.waitFor(15, TimeUnit.SECONDS) -> {
                    process.destroyForcibly()
                    "timed out: ${cmd.joinToString(" ")}"
                }

                process.exitValue() != 0 -> {
                    "exit ${process.exitValue()}: ${cmd.joinToString(" ")}\n    ${output.trim()}"
                }

                else -> {
                    null
                }
            }
        } catch (e: IOException) {
            "cannot start adb: ${e.message}"
        }
    }

    /** Returns null on success, otherwise a description of why this transport failed. */
    private fun broadcastViaSession(amArgs: List<String>): String? {
        val driver =
            DriverFactory.current()
                ?: return "no live Appium session on this thread"
        return try {
            driver.executeScript("mobile: shell", mapOf("command" to "am", "args" to amArgs))
            null
        } catch (e: RuntimeException) {
            // Most common cause: the server runs without --allow-insecure=uiautomator2:adb_shell.
            e.message?.lineSequence()?.firstOrNull() ?: e.javaClass.simpleName
        }
    }

    /**
     * Resolve adb: prefer the SDK from env, fall back to the conventional macOS
     * and Linux locations, finally trust PATH. Keeps the helper working whether
     * or not ANDROID_HOME is exported in the gradle and test JVM environment.
     */
    private val adb: String by lazy {
        val home = System.getProperty("user.home")
        val sdkRoots =
            sequenceOf(
                System.getenv("ANDROID_HOME"),
                System.getenv("ANDROID_SDK_ROOT"),
                System.getenv("LOCALAPPDATA")?.let { "$it/Android/Sdk" }, // Windows default
                home?.let { "$it/Library/Android/sdk" },
                home?.let { "$it/Android/Sdk" },
            ).filterNotNull()
        // adb on macOS and Linux, adb.exe on Windows.
        sdkRoots
            .flatMap { root -> sequenceOf("adb", "adb.exe").map { File(root, "platform-tools/$it") } }
            .firstOrNull { it.canExecute() }
            ?.absolutePath
            ?: "adb"
    }
}
