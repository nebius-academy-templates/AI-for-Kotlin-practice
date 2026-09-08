plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("io.appium:java-client:10.0.0")
    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Allure: step() reporting + @DisplayName/@AllureId/@Link annotations.
    // Produces build/allure-results; view with `allure serve build/allure-results`.
    testImplementation("io.qameta.allure:allure-junit5:2.29.0")
}

tasks.test {
    useJUnitPlatform()
    systemProperty(
        "appium.url",
        System.getProperty("appium.url") ?: "http://127.0.0.1:4723",
    )
    systemProperty(
        "app.apk",
        System.getProperty("app.apk")
            ?: rootProject.projectDir
                .resolve("app/build/outputs/apk/stable/debug/app-stable-debug.apk")
                .absolutePath,
    )
    systemProperty(
        "ui.variant",
        System.getProperty("ui.variant", "stable"),
    )
    systemProperty(
        "allure.results.directory",
        layout.buildDirectory
            .dir("allure-results")
            .get()
            .asFile.absolutePath,
    )
    val parallel = System.getProperty("appium.parallel", "false")
    val devices = System.getProperty("appium.devices", "")
    val parallelism =
        devices
            .split(",")
            .count { it.isNotBlank() }
            .coerceAtLeast(1)
    val deviceEvidenceDir = layout.buildDirectory.dir("reports/device-execution")
    systemProperty("appium.parallel", parallel)
    systemProperty("appium.devices", devices)
    systemProperty(
        "appium.device.evidence.dir",
        deviceEvidenceDir.get().asFile.absolutePath,
    )
    systemProperty("junit.jupiter.execution.parallel.enabled", parallel)
    systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    systemProperty(
        "junit.jupiter.execution.parallel.config.executor-service",
        "worker_thread_pool",
    )
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty(
        "junit.jupiter.execution.parallel.config.fixed.parallelism",
        parallelism.toString(),
    )
    systemProperty(
        "junit.jupiter.execution.parallel.config.fixed.max-pool-size",
        parallelism.toString(),
    )
    doFirst {
        if (parallel.toBoolean()) {
            deviceEvidenceDir.get().asFile.deleteRecursively()
        }
    }
    testLogging {
        if (parallel.toBoolean()) {
            // Concurrent green events are intentionally summarized by the runner
            // after JUnit XML is complete; failures still stream immediately.
            events("failed", "skipped")
            showStandardStreams = false
        } else {
            events("passed", "failed", "skipped")
            showStandardStreams = true
        }
        // Full stack traces in the console: students slice real failure output
        // (task 1.11), and Gradle's default SHORT format hides the frames.
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

val checkPageObjectBoundary by tasks.registering {
    group = "verification"
    description = "Reject Element construction outside the pages layer."
    val sources =
        fileTree("src/test/kotlin") {
            include("**/*.kt")
            exclude("pages/**")
        }
    inputs.files(sources)
    doLast {
        val constructor = Regex("""\bElement\s*\(""")
        val violations =
            sources.flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val trimmed = line.trimStart()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("*") && constructor.containsMatchIn(line)) {
                        "${file.relativeTo(projectDir)}:${index + 1}"
                    } else {
                        null
                    }
                }
            }
        check(violations.isEmpty()) {
            "Element must be constructed only in pages/: ${violations.joinToString()}"
        }
    }
}

tasks.named("check") {
    dependsOn(checkPageObjectBoundary)
}
