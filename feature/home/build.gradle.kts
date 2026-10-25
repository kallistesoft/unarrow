plugins {
    alias(libs.plugins.unarrow.android.feature)
}

android {
    namespace = "ca.kallistesoft.unarrow.feature.home"
}

dependencies {
    implementation(project(":core:billing"))
}
