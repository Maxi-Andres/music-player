package com.max.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import coil3.compose.SubcomposeAsyncImage
import com.max.musicplayer.R
import com.max.musicplayer.data.Song
import com.max.musicplayer.ui.theme.TextSecondary

/**
 * Fila de cancion, igual que en docs/reference/01-tab-canciones.jpeg:
 * caratula, titulo, "artista - album", fecha corta y menu de tres puntos.
 */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    showDate: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            uri = song.albumArtUri,
            modifier = Modifier.size(48.dp),
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
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showDate) {
            Text(
                text = formatShortDate(song.dateModifiedSeconds),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.action_select),
                tint = TextSecondary,
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
 * Caratula del album. Muchos archivos no tienen ninguna, asi que siempre hay
 * un placeholder con nota musical detras (como en la referencia).
 */
@Composable
fun AlbumArt(
    uri: String,
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
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = stringResource(R.string.cd_album_art),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ArtPlaceholder() },
            error = { ArtPlaceholder() },
        )
    }
}

@Composable
private fun ArtPlaceholder() {
    Box(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}
