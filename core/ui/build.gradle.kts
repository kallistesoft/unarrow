plugins {
    alias(libs.plugins.unarrow.android.library)
    alias(libs.plugins.unarrow.android.compose)
}

android {
    namespace = "ca.kallistesoft.unarrow.core.ui"
}

// Every visible string of the app (values: English, values-fr: France, values-fr-rCA: Canada's differences) and the
// UI pieces shared by the screens.
dependencies {
    api(project(":core:designsystem"))
    api(project(":core:game"))
}
