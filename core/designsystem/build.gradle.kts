plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.android.compose)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material.icons.extended)
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    testImplementation(project(":core:testing"))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
