plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.hilt)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.billing"
}

// Google Play Billing, kept behind this module's interfaces. No server: purchases are checked with Play on the device.
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(libs.billing.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:testing"))
    testImplementation(libs.mockk)
}
