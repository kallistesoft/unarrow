import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.hilt)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.flags"
    // Robolectric tests start Firebase, whose components are declared in the merged manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    // All callers are Kotlin: no DefaultImpls bridge for FeatureFlags.isEnabled (Java-only, never called).
    compilerOptions { jvmDefault = JvmDefaultMode.NO_COMPATIBILITY }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(project(":core:testing"))
    testImplementation(libs.mockk)
}
