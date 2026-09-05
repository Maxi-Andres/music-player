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
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.max.musicplayer.data.ArtworkLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Color dominante de la caratula de una cancion, para tenir la pantalla de
 * reproduccion (ver docs/reference/04-now-playing.jpeg).
 *
 * Devuelve null mientras carga, si la cancion no tiene tapa o si la opcion esta
 * apagada; en ese caso la pantalla usa el fondo normal.
 */
@Composable
fun rememberArtworkTint(songId: Long, enabled: Boolean): Color? {
    val context = LocalContext.current
    var tinte by remember(songId, enabled) { mutableStateOf<Color?>(null) }

    LaunchedEffect(songId, enabled) {
        if (!enabled) {
            tinte = null
            return@LaunchedEffect
        }
        tinte = withContext(Dispatchers.IO) {
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
    }

    return tinte
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
