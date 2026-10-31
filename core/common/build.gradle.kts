plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.hilt)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.common"
}

dependencies {
    testImplementation(project(":core:testing"))
    testImplementation(libs.mockk)
}
