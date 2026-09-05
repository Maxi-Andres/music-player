plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kover) apply false
}

// Kover se aplica y se configura solo en :app, que es donde vive el codigo.
// Tenerlo tambien aca creaba dos reportes distintos (uno filtrado y otro no) y
// `koverVerify` corria los dos, asi que la verificacion daba resultados contradictorios.
