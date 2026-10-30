import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.unarrow.jvm.library)
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}
