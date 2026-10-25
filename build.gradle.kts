// Plugins are declared here once so every module loads them from the same class loader.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.unarrow.kover)
}

// Unit test coverage of the app's modules, merged (see KoverConventionPlugin): ./gradlew koverVerifyUnit.
dependencies {
    listOf(
        ":core:ads",
        ":core:common",
        ":core:data",
        ":core:flags",
        ":core:designsystem",
        ":core:game",
        ":core:ui",
        ":core:update",
    ).forEach { kover(project(it)) }
}
