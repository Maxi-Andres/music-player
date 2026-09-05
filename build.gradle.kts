plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":app"))
}

// Umbrales de cobertura: el build falla si bajan.
// Se suben a medida que crece la suite (ver docs/TESTING.md).
kover {
    reports {
        filters {
            excludes {
                // La UI de Compose se cubre con tests de UI, no con cobertura de linea.
                packages("*.ui.theme")
                classes(
                    "*ComposableSingletons*",
                    "*_Factory*",
                    "*BuildConfig",
                    "*.databinding.*",
                )
            }
        }
        verify {
            rule("Cobertura minima de lineas") {
                bound {
                    minValue = 60
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                }
            }
        }
    }
}
