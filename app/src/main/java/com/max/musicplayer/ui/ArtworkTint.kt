package com.max.musicplayer.ui

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.max.musicplayer.data.ArtworkLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Color dominante de la caratula de una cancion, para tenir la pantalla de
 * reproduccion (ver docs/reference/04-now-playing.jpeg).
 *
 * Devuelve null si la cancion no tiene tapa o si la opcion esta apagada; en ese caso
 * la pantalla usa el fondo normal.
 *
 * Al cambiar de cancion NO vuelve a null: conserva el tinte anterior hasta tener el
 * nuevo. Si se limpiara, con imagen de fondo puesta el fondo del tema es transparente
 * y por un instante se veia la foto sin el degrade.
 */
@Composable
fun rememberArtworkTint(songId: Long, enabled: Boolean): Color? {
    val context = LocalContext.current
    var tinte by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(songId, enabled) {
        if (!enabled) {
            tinte = null
            return@LaunchedEffect
        }
        val nuevo = withContext(Dispatchers.IO) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songId,
            )
            // Alcanza con una miniatura: Palette no necesita resolucion y asi es rapido.
            val bitmap = ArtworkLoader.load(context, uri, TINT_SAMPLE_PX)
                ?: return@withContext null

            val paleta = runCatching {
                Palette.from(bitmap).clearFilters().maximumColorCount(16).generate()
            }.getOrNull() ?: return@withContext null

            // Se prefiere un color vivo; si la tapa es apagada, se cae al dominante.
            val rgb = paleta.getVibrantColor(0)
                .takeIf { it != 0 }
                ?: paleta.getMutedColor(0).takeIf { it != 0 }
                ?: paleta.getDominantColor(0)

            if (rgb == 0) null else Color(rgb)
        }
        // Una tapa ilegible o inexistente deja el tinte anterior: mejor un color viejo
        // que un salto brusco al fondo pelado.
        if (nuevo != null) tinte = nuevo
    }

    return tinte
}

/**
 * Sube el color a un brillo y una viveza minimos, conservando el tono.
 *
 * Hace falta porque el fondo de la pantalla de reproduccion ya es el color de la tapa
 * mezclado con el fondo del tema: si el anillo de progreso usara el color crudo, seria
 * de la misma familia que el degrade y se perderia contra el. Forzando la luminosidad y
 * un piso de saturacion, el anillo queda siempre mas claro y mas vivo que el fondo,
 * cualquiera sea la tapa.
 */
fun Color.vivid(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = hsl[1].coerceAtLeast(MIN_SATURATION)
    hsl[2] = TARGET_LIGHTNESS
    return Color(ColorUtils.HSLToColor(hsl))
}

/** Mezcla dos colores; [ratio] 0 devuelve este color y 1 el otro. */
fun Color.blendWith(other: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red = red * (1 - r) + other.red * r,
        green = green * (1 - r) + other.green * r,
        blue = blue * (1 - r) + other.blue * r,
        alpha = 1f,
    )
}

private const val TINT_SAMPLE_PX = 128

/** Valores fijos de [vivid]: claro para destacar sobre el fondo, sin llegar a blanco. */
private const val TARGET_LIGHTNESS = 0.66f
private const val MIN_SATURATION = 0.5f
