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
            // Se mide solo la logica. La UI de Compose y el servicio de reproduccion
            // necesitan tests instrumentados, que todavia no existen (ver docs/TESTING.md);
            // incluirlos daria un porcentaje global sin significado.
            includes {
                packages("com.max.musicplayer.data")
            }
        }
        verify {
            // Usa los filtros de arriba: mide solo la logica, no la UI.
            rule("Cobertura minima de la logica") {
                bound {
                    minValue = 85
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                }
            }
        }
    }
}
