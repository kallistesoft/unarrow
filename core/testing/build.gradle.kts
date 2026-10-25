plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.android.compose)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.testing"
}

// Shared test doubles and rules (testImplementation in the modules that use them).
dependencies {
    api(project(":core:ads"))
    api(project(":core:billing"))
    api(project(":core:data"))
    api(project(":core:flags"))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
    api(libs.turbine)
}
