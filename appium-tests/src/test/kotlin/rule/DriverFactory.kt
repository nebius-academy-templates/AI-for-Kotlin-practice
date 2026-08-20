package rule

import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import java.io.File
import java.net.URL
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

object DriverFactory {
    // Holds the driver for the test running on the current thread so JUnit
    // extensions (e.g. ArtifactsOnFailure) can grab the live session without the
    // test having to hand it over. Set on every createDriver(); overwritten each test.
    private val currentDriver = ThreadLocal<AndroidDriver?>()
    private val currentSlot = ThreadLocal<DeviceSlot?>()

    // Install once per device per JVM run. The FIRST session on each emulator
    // keeps enforceAppInstall=true:
    // both flavors share applicationId/versionCode, so without a forced install
    // Appium silently keeps whatever build is already on the device (the
    // wrong-build trap). Once this run has installed its APK, later sessions
    // skip the reinstall - same build, no per-test install cost.
    private val installedDevices = ConcurrentHashMap.newKeySet<String>()

    private val configuredSlots: List<DeviceSlot> by lazy {
        System
            .getProperty("appium.devices", "")
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .mapIndexed { index, udid ->
                DeviceSlot(udid = udid, systemPort = 8200 + index, pooled = true)
            }
    }

    private val availableSlots: LinkedBlockingQueue<DeviceSlot> by lazy {
        LinkedBlockingQueue<DeviceSlot>().apply { addAll(configuredSlots) }
    }

    /** The driver created for the current thread's test, or null if none exists yet. */
    fun current(): AndroidDriver? = currentDriver.get()

    /** Device selected for the current JUnit worker thread, if a pool is configured. */
    fun currentDeviceUdid(): String? = currentSlot.get()?.udid

    /**
     * Creates a session against the APK from the `app.apk` system property.
     *
     * @param startAuthorized launch the app already authorized: passes the boolean
     * launch extra "authenticated" (read by MainActivity, NavHost starts at the map)
     * via `appium:optionalIntentArguments`, so post-auth suites skip onboarding.
     */
    fun createDriver(startAuthorized: Boolean = false): AndroidDriver {
        val apkPath =
            System.getProperty("app.apk")
                ?: error("System property 'app.apk' is not set")
        val appiumUrl = System.getProperty("appium.url", "http://127.0.0.1:4723")

        val slot = acquireSlot()
        val installKey = slot.udid ?: "default-device"
        val forceInstall = installKey !in installedDevices

        return try {
            createSessionWithHealthRetry(
                appiumUrl = appiumUrl,
                apkPath = apkPath,
                slot = slot,
                forceInstall = forceInstall,
                startAuthorized = startAuthorized,
            ).also {
                currentDriver.set(it)
                // Mark only after session creation and APK installation
                // succeeded on this specific emulator.
                installedDevices += installKey
            }
        } catch (failure: RuntimeException) {
            releaseCurrentDevice()
            throw failure
        }
    }

    /** Return the current emulator to the pool after driver.quit(). */
    fun releaseCurrentDevice() {
        currentDriver.remove()
        val slot = currentSlot.get()
        currentSlot.remove()
        if (slot?.pooled == true) {
            availableSlots.put(slot)
        }
    }

    private fun acquireSlot(): DeviceSlot {
        val slot =
            if (configuredSlots.isEmpty()) {
                DeviceSlot(udid = null, systemPort = null, pooled = false)
            } else {
                availableSlots.take()
            }
        currentSlot.set(slot)
        return slot
    }

    private fun createSessionWithHealthRetry(
        appiumUrl: String,
        apkPath: String,
        slot: DeviceSlot,
        forceInstall: Boolean,
        startAuthorized: Boolean,
    ): AndroidDriver {
        var lastFailure: RuntimeException? = null
        repeat(2) { attempt ->
            awaitDeviceReady(slot)
            val options =
                UiAutomator2Options()
                    .setApp(apkPath)
                    .setNewCommandTimeout(Duration.ofSeconds(120))
                    .setFullReset(false)
                    // A cold Windows emulator can need more than Appium's 30-second
                    // default to start the UiAutomator2 instrumentation process.
                    .amend("appium:uiautomator2ServerLaunchTimeout", 60_000)
                    // Compose testTags are plain resource-ids ("phone_title"); without this
                    // flag the driver rewrites them to "com.sandbox.qa:id/phone_title".
                    .amend("appium:disableIdLocatorAutocompletion", true)
                    // Required on the first session - see appium-tests/README.md,
                    // Troubleshooting.
                    .amend("appium:enforceAppInstall", forceInstall)

            slot.udid?.let(options::setUdid)
            slot.systemPort?.let(options::setSystemPort)
            if (startAuthorized) {
                options.amend("appium:optionalIntentArguments", "--ez authenticated true")
            }

            try {
                return AndroidDriver(URL(appiumUrl), options)
            } catch (failure: RuntimeException) {
                lastFailure = failure
                if (attempt == 1 || !failure.isTransientDeviceFailure()) {
                    throw failure
                }
            }
        }
        throw lastFailure ?: error("Appium session creation failed without an exception")
    }

    private fun awaitDeviceReady(slot: DeviceSlot) {
        val udid = slot.udid ?: return
        val process =
            ProcessBuilder(adbExecutable(), "-s", udid, "wait-for-device")
                .redirectErrorStream(true)
                .start()
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Device $udid did not become ready within 30 seconds")
        }
        val output =
            process.inputStream
                .bufferedReader()
                .use { it.readText() }
                .trim()
        check(process.exitValue() == 0) {
            "adb wait-for-device failed for $udid: $output"
        }
    }

    private fun adbExecutable(): String {
        val sdkRoot =
            System.getenv("ANDROID_HOME")
                ?: System.getenv("ANDROID_SDK_ROOT")
                ?: error("ANDROID_HOME or ANDROID_SDK_ROOT is required for the device pool")
        val executable = if (System.getProperty("os.name").startsWith("Windows")) "adb.exe" else "adb"
        return File(sdkRoot, "platform-tools/$executable").absolutePath
    }

    private fun RuntimeException.isTransientDeviceFailure(): Boolean {
        val details = message.orEmpty().lowercase()
        return "device offline" in details ||
            "not in the list of connected devices" in details ||
            "adbexec" in details
    }

    private data class DeviceSlot(
        val udid: String?,
        val systemPort: Int?,
        val pooled: Boolean,
    )
}
