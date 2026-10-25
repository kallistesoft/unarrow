plugins {
    alias(libs.plugins.unarrow.android.feature)
}

android {
    namespace = "ca.kallistesoft.unarrow.feature.history"
}

dependencies {
    implementation(project(":core:ads"))
}
