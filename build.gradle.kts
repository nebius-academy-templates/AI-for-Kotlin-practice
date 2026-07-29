plugins {
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    // Kotlin formatter/linter (the project's "ruff"). Plugin 14.2.0 supports
    // Gradle 9.x; the ktlint engine itself is pinned in the subprojects block.
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

// Apply ktlint to both modules from one place. Engine 1.8.0 parses the
// project's Kotlin (2.3.20) sources; style knobs live in /.editorconfig.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.8.0")
    }
}
