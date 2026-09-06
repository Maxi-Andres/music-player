import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kover)
}

/**
 * Datos de la clave con la que se firman los APK publicados.
 *
 * En esta compu salen de `keystore.properties` (ignorado por git); en GitHub Actions,
 * de variables de entorno cargadas desde los secrets del repo. Si no hay ninguna de las
 * dos, `release` queda sin firmar: sirve igual para compilar y para el lint, pero el
 * APK no se puede instalar. Es a proposito, asi un clon del repo compila sin la clave.
 */
val propiedadesDeFirma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

fun datoDeFirma(clave: String, variable: String): String? =
    propiedadesDeFirma.getProperty(clave) ?: System.getenv(variable)

val almacenDeFirma = datoDeFirma("storeFile", "SIGNING_STORE_FILE")?.let(::file)
    ?.takeIf { it.exists() }

/**
 * La version sale del tag de git cuando se publica (la calcula el workflow de release)
 * y cae a estos valores en cualquier build local.
 */
val versionPublicada = System.getenv("VERSION_NAME") ?: "0.1.0"
val codigoPublicado = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1

android {
    namespace = "com.max.musicplayer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.max.musicplayer"
        minSdk = 26
        targetSdk = 37
        versionCode = codigoPublicado
        versionName = versionPublicada

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (almacenDeFirma != null) {
                storeFile = almacenDeFirma
                storePassword = datoDeFirma("storePassword", "SIGNING_STORE_PASSWORD")
                keyAlias = datoDeFirma("keyAlias", "SIGNING_KEY_ALIAS")
                keyPassword = datoDeFirma("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Cobertura de los tests instrumentados (Kover aun no la mide, la mide AGP/JaCoCo).
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = false // la de unit tests la maneja Kover
        }
        // Build para medir rendimiento: igual que release pero sin ofuscar y firmado
        // con la clave de debug. Un build debug de Compose es varias veces mas lento,
        // asi que medir jank sobre debug no dice nada util.
        create("profile") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
        release {
            // Sin clave no se firma: el build igual sale, pero el APK no instala.
            signingConfig = signingConfigs.getByName("release").takeIf { almacenDeFirma != null }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Robolectric necesita los recursos reales; sin esto los tests de UI/recursos fallan.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
}

/**
 * Cobertura.
 *
 * Se mide solo la logica (`data`). La UI de Compose y el servicio de reproduccion
 * necesitan tests instrumentados, que todavia no existen: incluirlos daria un
 * porcentaje global sin significado y obligaria a bajar el umbral hasta volverlo inutil.
 * Cuando esos tests existan, se amplia el filtro.
 */
kover {
    reports {
        filters {
            includes {
                packages("com.max.musicplayer.data")
            }
        }
        verify {
            rule("Cobertura minima de la logica") {
                bound {
                    minValue = 85
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                }
            }
        }
    }
}

// Se fija explicitamente porque el plugin de Kotlin no hereda solo el target de Java.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)

    implementation(libs.coil.compose)

    // --- Unit tests (JVM, sin dispositivo) ---
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.media3.test.utils)
    testImplementation(libs.media3.test.utils.robolectric)
    debugImplementation(libs.compose.ui.test.manifest)

    // --- Tests instrumentados (celular / emulador) ---
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
