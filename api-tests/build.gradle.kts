plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("io.rest-assured:rest-assured:5.5.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Allure: step() reporting plus the REST Assured filter that attaches
    // every request/response pair to the report as evidence.
    testImplementation("io.qameta.allure:allure-junit5:2.29.0")
    testImplementation("io.qameta.allure:allure-rest-assured:2.29.0")
}

tasks.test {
    useJUnitPlatform()
    val allureResultsDir = layout.buildDirectory.dir("allure-results")
    // The target of these tests is the fake-api server; start it first: ./gradlew :fake-api:run
    systemProperty(
        "api.url",
        System.getProperty("api.url") ?: "http://localhost:8080",
    )
    systemProperty(
        "allure.results.directory",
        allureResultsDir.get().asFile.absolutePath,
    )
    doFirst {
        allureResultsDir.get().asFile.deleteRecursively()
    }
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
        // Full stack traces in the console, same rationale as appium-tests:
        // students slice real failure output, SHORT hides the frames.
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
