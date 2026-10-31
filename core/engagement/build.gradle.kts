plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.hilt)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.engagement"
}

// Google Play In-App Review, and sharing a result (text with the Play Store link) through Android's share sheet.
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:flags"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.review.ktx)

    testImplementation(project(":core:testing"))
    testImplementation(libs.mockk)
}
