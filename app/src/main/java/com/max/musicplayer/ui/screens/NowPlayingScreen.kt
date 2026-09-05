package com.max.musicplayer.ui.screens

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.max.musicplayer.R
import com.max.musicplayer.data.QueueEntry
import com.max.musicplayer.data.Song
import com.max.musicplayer.ui.blendWith
import com.max.musicplayer.ui.rememberArtworkTint
import com.max.musicplayer.ui.components.AlbumArt
import com.max.musicplayer.ui.components.MarqueeText
import com.max.musicplayer.ui.components.formatDuration

private const val SEEK_STEP_MS = 10_000L

/** Linea fina, como en la app de referencia. */
private val TRACK_HEIGHT = 3.dp

/** Pantalla de reproduccion. Ver docs/reference/04-now-playing.jpeg. */
// El slider con thumb propio todavia es API experimental de Material 3.
@OptIn(ExperimentalMaterial3Api::class)
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
    /** Tenir el fondo con el color de la caratula (opcion de Personalizacion). */
    tintFromArtwork: Boolean = false,
) {
    // El tinte se calcula del color dominante de la tapa y se mezcla con el fondo del
    // tema: sin mezclar, un color vivo dejaria el texto ilegible.
    val tinte = rememberArtworkTint(song.id, tintFromArtwork)
    val fondoTema = MaterialTheme.colorScheme.background
    val pincel = if (tinte != null) {
        Brush.verticalGradient(
            listOf(
                tinte.blendWith(fondoTema, 0.45f),
                tinte.blendWith(fondoTema, 0.80f),
                fondoTema,
            ),
        )
    } else {
        SolidColor(fondoTema)
    }

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
            .background(pincel)
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

        // Espacios flexibles arriba y abajo: la caratula queda mas abajo y el panel
        // de control mas separado de ella, sin dejar un hueco muerto al final.
        Spacer(modifier = Modifier.weight(0.5f))

        AlbumArt(
            songId = song.id,
            cornerRadius = 16,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
        )

        Spacer(modifier = Modifier.height(36.dp))

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        tint = if (queuedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    )
                }
                // Contador de temas encolados a mano; si no encolaste nada, no aparece.
                if (queuedCount > 0) {
                    Text(
                        text = queuedCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }

        Row(
            // Casi sin margen lateral: la barra tiene que llegar hasta los botones de
            // -10/+10. Cuanto mas larga, mas fino es el control al arrastrar.
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onSeekBy(-SEEK_STEP_MS) }) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = stringResource(R.string.cd_rewind_10),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            SeekBar(
                positionMs = posicionMostrada,
                durationMs = durationMs,
                onScrub = {
                    arrastrando = true
                    posicionArrastre = it.toFloat()
                },
                onScrubEnd = {
                    onSeek(posicionArrastre.toLong())
                    arrastrando = false
                },
                modifier = Modifier.weight(1f),
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
                    tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
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
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Tira con las canciones del contexto, como en la referencia.
        if (contextEntries.isNotEmpty()) {
            LazyRow(
                state = tiraState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 72.dp),
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
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
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
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

/**
 * Barra de progreso con la pastilla del tiempo como cursor.
 *
 * Es propia y no un [androidx.compose.material3.Slider] porque este reserva a los
 * costados el ancho del thumb: con una pastilla tan ancha la linea quedaba mucho mas
 * corta que el espacio disponible y no llegaba a los botones de -10/+10. Aca la linea
 * ocupa todo el ancho y la pastilla se posiciona sola, lo que ademas da un control mas
 * fino al arrastrar.
 */
@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onScrub: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val duracion = durationMs.coerceAtLeast(1L)
    var anchoPx by remember { mutableIntStateOf(0) }
    var anchoPastillaPx by remember { mutableIntStateOf(0) }

    fun posicionEn(x: Float): Long {
        if (anchoPx <= 0) return 0L
        return ((x / anchoPx).coerceIn(0f, 1f) * duracion).toLong()
    }

    val fraccion = (positionMs.toFloat() / duracion).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(26.dp)
            .onSizeChanged { anchoPx = it.width }
            .pointerInput(duracion) {
                detectTapGestures { offset ->
                    onScrub(posicionEn(offset.x))
                    onScrubEnd()
                }
            }
            .pointerInput(duracion) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onScrub(posicionEn(offset.x)) },
                    onDragEnd = { onScrubEnd() },
                    onDragCancel = { onScrubEnd() },
                    onHorizontalDrag = { cambio, _ ->
                        cambio.consume()
                        onScrub(posicionEn(cambio.position.x))
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TRACK_HEIGHT)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraccion)
                .height(TRACK_HEIGHT)
                .clip(CircleShape)
                .background(Color.White),
        )

        // La pastilla se corre con el progreso, sin salirse por los bordes.
        val recorrido = (anchoPx - anchoPastillaPx).coerceAtLeast(0)
        Box(
            modifier = Modifier
                .offset { IntOffset((fraccion * recorrido).toInt(), 0) }
                .onSizeChanged { anchoPastillaPx = it.width }
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${formatDuration(positionMs)} / ${formatDuration(durationMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF101010),
                maxLines = 1,
            )
        }
    }
}
