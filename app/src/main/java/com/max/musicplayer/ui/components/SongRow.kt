package com.max.musicplayer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.max.musicplayer.ui.AudioArtwork
import com.max.musicplayer.R
import com.max.musicplayer.data.Song

/**
 * Fila de cancion, igual que en docs/reference/01-tab-canciones.jpeg:
 * caratula, titulo, "artista - album", fecha corta y menu de tres puntos.
 *
 * En modo seleccion el menu se reemplaza por una casilla y tocar la fila marca o
 * desmarca en vez de reproducir. Manteniendo apretada una fila se entra al modo, que
 * es como se hace en el resto de las apps.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    showDate: Boolean = true,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        // El fondo de seleccion va ANTES del modifier de afuera. En la pestania de
        // canciones ese modifier reserva a la derecha el ancho de la barra de letras,
        // y aplicado primero el resaltado terminaba cortado justo ahi, mientras que
        // dentro de una carpeta (que no reserva nada) llegaba hasta el borde.
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    Modifier
                },
            )
            .then(modifier)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // El menu va a la izquierda: a la derecha chocaba con la barra de letras.
        // La casilla ocupa ese mismo lugar para que la fila no se mueva al entrar
        // y salir del modo seleccion.
        if (selectionMode) {
            Checkbox(
                checked = selected,
                // La fila entera ya alterna la seleccion; que la casilla haga lo mismo
                // por su cuenta duplicaria el evento al tocarla justo encima.
                onCheckedChange = null,
                modifier = Modifier.size(28.dp),
            )
        } else {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.action_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AlbumArt(
            songId = song.id,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(48.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitleFor(song),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showDate) {
            Text(
                text = formatShortDate(song.dateModifiedSeconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** "artista - album", cayendo a la carpeta cuando no hay album taggeado. */
private fun subtitleFor(song: Song): String {
    val segundo = song.album.ifBlank { song.folderName }
    return if (segundo.isBlank()) song.displayArtist else "${song.displayArtist} - $segundo"
}

/**
 * Caratula de la cancion.
 *
 * El placeholder se dibuja *detras* y la imagen encima: asi no hace falta
 * `SubcomposeAsyncImage` (que crea una subcomposicion por fila y hace tironear el
 * scroll en listas largas). Si el archivo no tiene tapa, simplemente sigue viendose
 * el placeholder.
 */
@Composable
fun AlbumArt(
    songId: Long,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        AsyncImage(
            model = AudioArtwork(songId),
            contentDescription = stringResource(R.string.cd_album_art),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
