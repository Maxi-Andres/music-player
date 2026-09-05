package com.max.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.max.musicplayer.R
import com.max.musicplayer.data.QueueEntry
import com.max.musicplayer.data.Song
import com.max.musicplayer.ui.components.AlbumArt
import com.max.musicplayer.ui.components.MarqueeText
import com.max.musicplayer.ui.components.formatDuration
import com.max.musicplayer.ui.theme.Amber
import com.max.musicplayer.ui.theme.TextSecondary

private const val SEEK_STEP_MS = 10_000L

/** Pantalla de reproduccion. Ver docs/reference/04-now-playing.jpeg. */
@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    /**
     * Las canciones del contexto (la carpeta o la lista desde donde arranco la
     * reproduccion), en el orden configurado. NO es la cola: poner una cancion de una
     * carpeta fija un contexto, no crea una cola.
     */
    contextEntries: List<QueueEntry>,
    currentContextIndex: Int,
    queuedCount: Int,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onContextItemClick: (QueueEntry) -> Unit,
) {
    val tiraState = rememberLazyListState()

    // La tira se posiciona en lo que esta sonando, no arranca siempre del principio.
    LaunchedEffect(currentContextIndex) {
        if (currentContextIndex >= 0) {
            tiraState.animateScrollToItem(currentContextIndex.coerceAtLeast(0))
        }
    }
    // Mientras se arrastra la barra mandan los dedos, no el reproductor: si no, el
    // pulgar "pelea" con las actualizaciones de posicion que llegan cada medio segundo.
    var arrastrando by remember { mutableStateOf(false) }
    var posicionArrastre by remember { mutableFloatStateOf(0f) }

    val duracion = durationMs.coerceAtLeast(1L)
    val posicionMostrada = if (arrastrando) posicionArrastre.toLong() else positionMs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.now_playing),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        AlbumArt(
            songId = song.id,
            cornerRadius = 16,
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = song.displayArtist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onOpenEqualizer) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = stringResource(R.string.equalizer),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Box {
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = stringResource(R.string.cd_queue),
                        tint = if (queuedCount > 0) Amber else MaterialTheme.colorScheme.onBackground,
                    )
                }
                // Contador de temas encolados a mano; si no encolaste nada, no aparece.
                if (queuedCount > 0) {
                    Text(
                        text = queuedCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onSeekBy(-SEEK_STEP_MS) }) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = stringResource(R.string.cd_rewind_10),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "${formatDuration(posicionMostrada)} / ${formatDuration(durationMs)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Slider(
                value = posicionMostrada.toFloat().coerceIn(0f, duracion.toFloat()),
                valueRange = 0f..duracion.toFloat(),
                onValueChange = {
                    arrastrando = true
                    posicionArrastre = it
                },
                onValueChangeFinished = {
                    onSeek(posicionArrastre.toLong())
                    arrastrando = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Amber,
                    activeTrackColor = Amber,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            IconButton(onClick = { onSeekBy(SEEK_STEP_MS) }) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = stringResource(R.string.cd_forward_10),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.action_shuffle),
                    tint = if (shuffleEnabled) Amber else MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(40.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.cd_pause else R.string.cd_play,
                    ),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    },
                    contentDescription = stringResource(R.string.cd_repeat),
                    tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                        TextSecondary
                    } else {
                        Amber
                    },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Tira con las canciones del contexto, como en la referencia.
        if (contextEntries.isNotEmpty()) {
            LazyRow(
                state = tiraState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(contextEntries, key = { _, e -> e.uid }) { indice, entrada ->
                    val esActual = indice == currentContextIndex
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .then(
                                if (esActual) {
                                    Modifier.border(2.dp, Amber, RoundedCornerShape(8.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onContextItemClick(entrada) },
                    ) {
                        AlbumArt(songId = entrada.song.id, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
