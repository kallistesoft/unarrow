import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A feature module: Compose screens with Hilt ViewModels, on top of the core modules. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("unarrow.android.library")
            pluginManager.apply("unarrow.android.compose")
            pluginManager.apply("unarrow.hilt")
            pluginManager.apply("unarrow.android.screenshot")
            dependencies {
                add("implementation", project(":core:game"))
                add("implementation", project(":core:data"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.lib("androidx-compose-material-icons-extended"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
            }
        }
    }
}
