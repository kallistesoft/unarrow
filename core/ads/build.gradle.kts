plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.android.compose)
    alias(libs.plugins.unarrow.hilt)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.ads"
    // The banner's Compose test runs in an activity declared by the test manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

// AdMob: the Google Mobile Ads SDK and Google's consent SDK (UMP), kept behind this module's interfaces. Compose for
// the banner, which the game screen places under the board.
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:flags"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.ump)

    testImplementation(project(":core:testing"))
    testImplementation(libs.mockk)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
