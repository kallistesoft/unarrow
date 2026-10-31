plugins {
    alias(libs.plugins.unarrow.android.feature)
}

android {
    namespace = "ca.kallistesoft.unarrow.feature.settings"
}

dependencies {
    implementation(project(":core:ads"))
    implementation(project(":core:billing"))
    implementation(project(":core:engagement"))
    implementation(libs.androidx.appcompat)
}
