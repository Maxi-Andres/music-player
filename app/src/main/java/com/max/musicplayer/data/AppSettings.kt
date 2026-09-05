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
    /**
     * Tenir la pantalla de reproduccion con el color dominante de la caratula,
     * como hace la app de referencia.
     */
    val tintFromArtwork: Boolean = true,
) {
    val usesBackgroundImage: Boolean get() = !backgroundImageUri.isNullOrBlank()

    companion object {
        const val DEFAULT_ACCENT = 0xFFF9E2AF.toInt() // yellow pastel
        const val DEFAULT_BACKGROUND = 0xFF0F1720.toInt()
        const val DEFAULT_FOLDER = 0xFFF9E2AF.toInt()
        const val DEFAULT_DIM = 0.55f

        /** Paleta pastel ofrecida en la pantalla de personalizacion. */
        val ACCENT_CHOICES = listOf(
            0xFFFFFFFF.toInt(), // white
            0xFFF5E0DC.toInt(), // rosewater
            0xFFF2CDCD.toInt(), // flamingo
            0xFFF5C2E7.toInt(), // pink
            0xFFCBA6F7.toInt(), // mauve
            0xFFF38BA8.toInt(), // red
            0xFFEBA0AC.toInt(), // maroon
            0xFFFAB387.toInt(), // peach
            0xFFF9E2AF.toInt(), // yellow
            0xFFA6E3A1.toInt(), // green
            0xFF94E2D5.toInt(), // teal
            0xFF89DCEB.toInt(), // sky
            0xFF74C7EC.toInt(), // sapphire
            0xFF89B4FA.toInt(), // blue
            0xFFB4BEFE.toInt(), // lavender
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
