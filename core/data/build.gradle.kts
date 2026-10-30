plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.data"
}

dependencies {
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
}
