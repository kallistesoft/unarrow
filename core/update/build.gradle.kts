plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.android.compose)
    alias(libs.plugins.unarrow.android.screenshot)
    alias(libs.plugins.unarrow.hilt)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.update"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:flags"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.app.update.ktx)
}
