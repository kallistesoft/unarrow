import java.util.Properties

plugins {
    alias(libs.plugins.unarrow.android.application)
    alias(libs.plugins.unarrow.android.compose)
    alias(libs.plugins.unarrow.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase (Remote Config: feature flags and force update; Crashlytics) is set up only when the project's config file
// is present, so the app also builds without it (CI before the secret exists, forks): flags keep their defaults, no
// update is forced and no crash is reported.
if (listOf("", "src/debug/", "src/release/").any { file("${it}google-services.json").exists() }) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

// Release signing comes from keystore.properties (local, never committed) or environment variables (CI).
val keystoreProperties = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun signingValue(key: String, env: String): String? = keystoreProperties.getProperty(key) ?: System.getenv(env)

// AdMob IDs (PROGRESS.md, "Needs you at release"). Public identifiers, not secrets. Until the Unarrow app exists in
// AdMob, every build uses Google's sample IDs, which serve test ads only: replace admobRelease's values then.
val admobTest = mapOf(
    "appId" to "ca-app-pub-3940256099942544~3347511713",
    "interstitial" to "ca-app-pub-3940256099942544/1033173712",
    "rewarded" to "ca-app-pub-3940256099942544/5224354917",
)
val admobRelease = admobTest

/** The AdMob App ID goes in the manifest (`${admobAppId}`), the ad units in BuildConfig. */
fun com.android.build.api.dsl.ApplicationBuildType.admob(ids: Map<String, String>) {
    manifestPlaceholders["admobAppId"] = ids.getValue("appId")
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${ids.getValue("interstitial")}\"")
    buildConfigField("String", "ADMOB_REWARDED_ID", "\"${ids.getValue("rewarded")}\"")
}

android {
    namespace = "ca.kallistesoft.unarrow"

    defaultConfig {
        applicationId = "ca.kallistesoft.unarrow"
        // Every Play upload needs a higher versionCode; CI passes -PversionCode=<run number>.
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Play listing opened when in-app updates aren't available (also used by the debug build).
        buildConfigField("String", "STORE_PACKAGE", "\"ca.kallistesoft.unarrow\"")
    }

    signingConfigs {
        val storeFile = signingValue("storeFile", "UA_KEYSTORE_FILE")
        if (storeFile != null) {
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                storePassword = signingValue("storePassword", "UA_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "UA_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "UA_KEY_PASSWORD")
            }
        }
    }

    // Code that differs by build type lives in src/debug and src/release (never `if (BuildConfig.DEBUG)`).
    buildTypes {
        debug {
            // Installs next to the Play version, with its own name.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["crashlyticsEnabled"] = "false"
            admob(admobTest)
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Unsigned when no keystore is configured (e.g. pull requests); Play App Signing re-signs uploads.
            signingConfig = signingConfigs.findByName("release")
            manifestPlaceholders["crashlyticsEnabled"] = "true"
            admob(admobRelease)
        }
    }

    testOptions {
        // Robolectric tests (src/test) run the real app with its resources.
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:ads"))
    implementation(project(":core:billing"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:engagement"))
    implementation(project(":core:flags"))
    implementation(project(":core:ui"))
    implementation(project(":core:update"))
    implementation(project(":feature:game"))
    implementation(project(":feature:home"))
    implementation(project(":feature:settings"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.androidx.datastore.preferences)
}
