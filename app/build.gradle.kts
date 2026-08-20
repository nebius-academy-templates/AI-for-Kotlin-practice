plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sandbox.qa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sandbox.qa"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")
    }

    // "stable" is the baseline UI; "redesign" renames auth-screen test tags.
    // The alternate tags provide a controlled locator-migration target.
    flavorDimensions += "ui"
    productFlavors {
        create("stable") {
            dimension = "ui"
            isDefault = true
        }
        create("redesign") {
            dimension = "ui"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // material3 1.4.0+ dropped the transitive material-icons-core dependency,
    // so the Icons used by the screens (Menu, AutoMirrored ArrowBack) need it explicitly.
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
