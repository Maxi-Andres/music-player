package com.max.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlin.math.roundToInt

private val ROW_HEIGHT = 64.dp

/** Cuanto scrollea por cuadro cuando arrastras contra un borde de la lista. */
private const val AUTO_SCROLL_PX = 14f

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
    val estadoLista = rememberLazyListState()

    // El reordenamiento se calcula mientras arrastras pero se aplica **una sola vez, al
    // soltar**. Antes se movia en cada salto, y como la lista usa el uid de clave, mover
    // la fila la reubicaba dentro de la composicion: eso desmontaba el nodo que estaba
    // escuchando el gesto y el arrastre se cortaba solo. De ahi que se pudiera correr
    // una posicion por vez y nada mas.
    var uidArrastrado by remember { mutableStateOf<Long?>(null) }
    var origen by remember { mutableIntStateOf(-1) }
    var desplazamiento by remember { mutableFloatStateOf(0f) }

    /** A que posicion iria la fila si soltaras ahora. */
    fun destinoActual(): Int =
        if (origen < 0 || queued.isEmpty()) {
            -1
        } else {
            (origen + (desplazamiento / alturaFilaPx).roundToInt())
                .coerceIn(0, queued.lastIndex)
        }

    val destino = destinoActual()

    // Con la cola mas larga que la pantalla, llevar una cancion arriba de todo o abajo
    // de todo obliga a que la lista acompanie al dedo. Lo que se scrollea se le suma al
    // desplazamiento: asi la fila queda quieta bajo el dedo y lo que avanza es el
    // destino.
    LaunchedEffect(uidArrastrado) {
        val uid = uidArrastrado ?: return@LaunchedEffect
        while (true) {
            val info = estadoLista.layoutInfo
            val fila = info.visibleItemsInfo.firstOrNull { it.key == uid } ?: break
            val y = fila.offset + desplazamiento

            val paso = when {
                y < info.viewportStartOffset + alturaFilaPx -> -AUTO_SCROLL_PX
                y + alturaFilaPx > info.viewportEndOffset - alturaFilaPx -> AUTO_SCROLL_PX
                else -> 0f
            }
            if (paso != 0f) {
                desplazamiento = (desplazamiento + estadoLista.scrollBy(paso))
                    .coerceIn(-origen * alturaFilaPx, (queued.lastIndex - origen) * alturaFilaPx)
            }
            withFrameNanos { }
        }
    }

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
                    Text(stringResource(R.string.queue_clear), color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (queued.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.queue_empty_explained),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
            return@Column
        }

        LazyColumn(state = estadoLista, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(queued, key = { _, entrada -> entrada.uid }) { indice, entrada ->
                val arrastrando = entrada.uid == uidArrastrado
                QueueRow(
                    entry = entrada,
                    isDragging = arrastrando,
                    // La fila arrastrada sigue al dedo; las del medio se corren una
                    // posicion y dejan a la vista el hueco donde va a caer.
                    dragOffset = when {
                        arrastrando -> desplazamiento
                        destino < 0 -> 0f
                        origen < destino && indice in (origen + 1)..destino -> -alturaFilaPx
                        destino < origen && indice in destino until origen -> alturaFilaPx
                        else -> 0f
                    },
                    onClick = { onPlayIndex(baseIndex + indice) },
                    onRemove = { onRemove(baseIndex + indice) },
                    // Arrastre directo desde el asa, sin pulsacion larga: el asa esta
                    // para eso y el long-press hacia que pareciera que no funcionaba.
                    dragModifier = Modifier.pointerInput(entrada.uid) {
                        detectDragGestures(
                            onDragStart = {
                                uidArrastrado = entrada.uid
                                origen = queued.indexOfFirst { it.uid == entrada.uid }
                                desplazamiento = 0f
                            },
                            onDragEnd = {
                                val hasta = destinoActual()
                                if (origen >= 0 && hasta >= 0 && hasta != origen) {
                                    onMove(baseIndex + origen, baseIndex + hasta)
                                }
                                uidArrastrado = null
                                origen = -1
                                desplazamiento = 0f
                            },
                            onDragCancel = {
                                uidArrastrado = null
                                origen = -1
                                desplazamiento = 0f
                            },
                            onDrag = { cambio, delta ->
                                cambio.consume()
                                if (origen < 0) return@detectDragGestures
                                // Se limita a los extremos: mas alla no hay adonde
                                // caer y la fila se despegaria del dedo.
                                desplazamiento = (desplazamiento + delta.y).coerceIn(
                                    -origen * alturaFilaPx,
                                    (queued.lastIndex - origen) * alturaFilaPx,
                                )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.queue_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = dragModifier.padding(start = 4.dp, end = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.queue_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
