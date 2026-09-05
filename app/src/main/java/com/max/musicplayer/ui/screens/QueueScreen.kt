package com.max.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.max.musicplayer.R
import com.max.musicplayer.data.QueueEntry
import com.max.musicplayer.ui.components.AlbumArt
import com.max.musicplayer.ui.theme.Amber
import com.max.musicplayer.ui.theme.TextSecondary
import kotlin.math.roundToInt

private val ROW_HEIGHT = 64.dp

/**
 * Cola de reproduccion: solo lo que el usuario encolo a mano.
 *
 * A proposito NO lista las canciones de la carpeta que se esta escuchando. Poner una
 * cancion de una carpeta fija un *contexto*, no crea una cola; el contexto se ve en la
 * tira de la pantalla de reproduccion. La cola es aparte y, si no encolaste nada, esta
 * vacia.
 *
 * [baseIndex] es la posicion real, dentro de la cola completa del reproductor, de la
 * primera entrada de esta lista. Los encolados van siempre juntos y justo despues de la
 * cancion actual, asi que alcanza con sumar la posicion mostrada.
 */
@Composable
fun QueueScreen(
    queued: List<QueueEntry>,
    baseIndex: Int,
    onClose: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClearAll: () -> Unit,
) {
    val alturaFilaPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }

    // Se sigue por uid y no por posicion: al mover la fila su indice cambia, y guardar
    // el indice hacia que el arrastre se perdiera apenas cruzaba una fila.
    var uidArrastrado by remember { mutableStateOf<Long?>(null) }
    var desplazamiento by remember { mutableFloatStateOf(0f) }

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
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.queue_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (queued.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text(stringResource(R.string.queue_clear), color = Amber)
                }
            }
        }

        if (queued.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.queue_empty_explained),
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(queued, key = { _, entrada -> entrada.uid }) { indice, entrada ->
                val arrastrando = entrada.uid == uidArrastrado
                QueueRow(
                    entry = entrada,
                    isDragging = arrastrando,
                    dragOffset = if (arrastrando) desplazamiento else 0f,
                    onClick = { onPlayIndex(baseIndex + indice) },
                    onRemove = { onRemove(baseIndex + indice) },
                    // Arrastre directo desde el asa, sin pulsacion larga: el asa esta
                    // para eso y el long-press hacia que pareciera que no funcionaba.
                    dragModifier = Modifier.pointerInput(entrada.uid) {
                        detectDragGestures(
                            onDragStart = {
                                uidArrastrado = entrada.uid
                                desplazamiento = 0f
                            },
                            onDragEnd = {
                                uidArrastrado = null
                                desplazamiento = 0f
                            },
                            onDragCancel = {
                                uidArrastrado = null
                                desplazamiento = 0f
                            },
                            onDrag = { cambio, delta ->
                                cambio.consume()
                                desplazamiento += delta.y

                                val actual = queued.indexOfFirst { it.uid == entrada.uid }
                                if (actual < 0) return@detectDragGestures

                                val saltos = (desplazamiento / alturaFilaPx).roundToInt()
                                if (saltos == 0) return@detectDragGestures

                                val destino = (actual + saltos).coerceIn(0, queued.lastIndex)
                                if (destino == actual) return@detectDragGestures

                                onMove(baseIndex + actual, baseIndex + destino)
                                desplazamiento -= (destino - actual) * alturaFilaPx
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    isDragging: Boolean,
    dragOffset: Float,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    dragModifier: Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragOffset
                if (isDragging) shadowElevation = 12f
            }
            .background(
                if (isDragging) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.background
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(songId = entry.song.id, modifier = Modifier.size(44.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = entry.song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.song.displayArtist,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.queue_remove),
                tint = TextSecondary,
            )
        }
        Box(
            modifier = dragModifier.padding(start = 4.dp, end = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.queue_reorder),
                tint = TextSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
