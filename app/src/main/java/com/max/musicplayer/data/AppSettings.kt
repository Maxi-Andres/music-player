package com.max.musicplayer.data

/**
 * Preferencias de apariencia elegidas por el usuario.
 *
 * Los colores se guardan como enteros ARGB porque es lo que entra en DataStore sin
 * inventar un serializador; la UI los convierte a Color al construir el tema.
 */
data class AppSettings(
    val accentColor: Int = DEFAULT_ACCENT,
    val backgroundColor: Int = DEFAULT_BACKGROUND,
    val folderColor: Int = DEFAULT_FOLDER,
    /** Imagen de fondo elegida por el usuario, o null para usar el color liso. */
    val backgroundImageUri: String? = null,
    /** Cuanto se oscurece la imagen para que el texto siga legible. */
    val backgroundDim: Float = DEFAULT_DIM,
) {
    val usesBackgroundImage: Boolean get() = !backgroundImageUri.isNullOrBlank()

    companion object {
        const val DEFAULT_ACCENT = 0xFFFFB300.toInt()
        const val DEFAULT_BACKGROUND = 0xFF0F1720.toInt()
        const val DEFAULT_FOLDER = 0xFFFFB300.toInt()
        const val DEFAULT_DIM = 0.55f

        /** Colores ofrecidos en la pantalla de personalizacion. */
        val ACCENT_CHOICES = listOf(
            0xFFFFB300.toInt(), // ambar (por defecto)
            0xFFFF7043.toInt(), // naranja
            0xFFEF5350.toInt(), // rojo
            0xFFEC407A.toInt(), // rosa
            0xFFAB47BC.toInt(), // violeta
            0xFF5C6BC0.toInt(), // indigo
            0xFF29B6F6.toInt(), // celeste
            0xFF26A69A.toInt(), // verde agua
            0xFF66BB6A.toInt(), // verde
            0xFFBDBDBD.toInt(), // gris
        )

        val BACKGROUND_CHOICES = listOf(
            0xFF0F1720.toInt(), // azul noche (por defecto)
            0xFF000000.toInt(), // negro
            0xFF121212.toInt(), // gris muy oscuro
            0xFF1A1423.toInt(), // violeta oscuro
            0xFF102019.toInt(), // verde oscuro
            0xFF1B1412.toInt(), // marron oscuro
            0xFFF5F5F5.toInt(), // claro
        )
    }
}
