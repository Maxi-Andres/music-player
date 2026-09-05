package com.max.musicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OnAmber = Color(0xFF101010)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = OnAmber,
    secondary = AmberDeep,
    onSecondary = OnAmber,
    background = NightBackground,
    onBackground = TextPrimary,
    surface = NightSurface,
    onSurface = TextPrimary,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
)

/**
 * La app es oscura siempre: las capturas de referencia lo son y no hay diseño claro
 * definido. Se expone [colorScheme] como parámetro para poder fijarlo desde los tests.
 */
@Composable
fun MusicPlayerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content,
    )
}
