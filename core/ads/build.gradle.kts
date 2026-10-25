plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.hilt)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.ads"
}

// AdMob: the Google Mobile Ads SDK and Google's consent SDK (UMP), kept behind this module's interfaces.
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:flags"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.ump)

    testImplementation(project(":core:testing"))
    testImplementation(libs.mockk)
}
