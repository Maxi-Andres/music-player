package com.max.musicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.max.musicplayer.data.AppSettings

/**
 * Color de los iconos de carpeta. Va por CompositionLocal porque no encaja en ningun
 * rol de Material, pero el usuario puede cambiarlo desde Personalizacion.
 */
val LocalFolderColor = staticCompositionLocalOf { FolderAmber }

/** Un fondo claro necesita texto oscuro; si no, no se lee nada. */
private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF101010) else TextPrimary

private fun secondaryContentColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF5F6368) else TextSecondary

/** Las superficies se derivan del fondo para que el tema siga siendo coherente. */
private fun surfaceFor(background: Color, amount: Float): Color =
    if (background.luminance() > 0.5f) {
        background.copy(
            red = (background.red - amount).coerceAtLeast(0f),
            green = (background.green - amount).coerceAtLeast(0f),
            blue = (background.blue - amount).coerceAtLeast(0f),
        )
    } else {
        background.copy(
            red = (background.red + amount).coerceAtMost(1f),
            green = (background.green + amount).coerceAtMost(1f),
            blue = (background.blue + amount).coerceAtMost(1f),
        )
    }

/**
 * Tema de la app, armado con los colores que el usuario eligio.
 *
 * Con imagen de fondo el color de fondo pasa a ser transparente: la imagen se dibuja
 * por detras de todo en MainActivity.
 */
@Composable
fun MusicPlayerTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val acento = Color(settings.accentColor)
    val fondoElegido = Color(settings.backgroundColor)
    // Con imagen, el "fondo" real para calcular contrastes es oscuro por el velo.
    val fondoParaContraste = if (settings.usesBackgroundImage) NightBackground else fondoElegido

    val alFrente = contentColorFor(fondoParaContraste)
    val secundario = secondaryContentColorFor(fondoParaContraste)
    val superficie = surfaceFor(fondoParaContraste, 0.04f)
    val superficieVariante = surfaceFor(fondoParaContraste, 0.09f)

    val colores = darkColorScheme(
        primary = acento,
        onPrimary = contentColorFor(acento),
        secondary = acento,
        onSecondary = contentColorFor(acento),
        background = if (settings.usesBackgroundImage) Color.Transparent else fondoElegido,
        onBackground = alFrente,
        surface = superficie,
        onSurface = alFrente,
        surfaceVariant = superficieVariante,
        onSurfaceVariant = secundario,
    ).takeIf { fondoParaContraste.luminance() <= 0.5f } ?: lightColorScheme(
        primary = acento,
        onPrimary = contentColorFor(acento),
        secondary = acento,
        onSecondary = contentColorFor(acento),
        background = fondoElegido,
        onBackground = alFrente,
        surface = superficie,
        onSurface = alFrente,
        surfaceVariant = superficieVariante,
        onSurfaceVariant = secundario,
    )

    CompositionLocalProvider(LocalFolderColor provides Color(settings.folderColor)) {
        MaterialTheme(
            colorScheme = colores,
            typography = Typography(),
            content = content,
        )
    }
}
