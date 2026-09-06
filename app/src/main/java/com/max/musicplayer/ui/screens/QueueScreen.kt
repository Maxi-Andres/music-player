package com.max.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import kotlin.math.abs

private val ROW_HEIGHT = 64.dp

/**
 * Ancho de la zona que captura el arrastre, medido desde el borde derecho: cubre el asa
 * y el margen de la fila, y termina justo donde arranca la X de quitar.
 */
private val DRAG_ZONE_WIDTH = 48.dp

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

    // El reordenamiento se calcula mientras arrastras y se aplica **una sola vez, al
    // soltar**. Moverlo en cada salto reubicaba la fila dentro de la composicion (la
    // lista usa el uid de clave), y eso desmontaba el nodo que escuchaba el gesto.
    //
    // Lo que se guarda es **donde esta el dedo dentro de la lista**, no cuanto se
    // arrastro. Acumular deltas fallaba en dos frentes: la fila trasladada devuelve
    // diferencias que tienden a cero (Compose informa la posicion relativa al nodo ya
    // movido) y el auto-scroll las contaba dos veces. Una posicion absoluta contra el
    // layout actual no tiene ninguno de los dos problemas.
    var uidArrastrado by remember { mutableStateOf<Long?>(null) }
    var origen by remember { mutableIntStateOf(-1) }
    var yDelDedo by remember { mutableFloatStateOf(0f) }

    val filaArrastrada = estadoLista.layoutInfo.visibleItemsInfo
        .firstOrNull { it.key == uidArrastrado }

    /**
     * A que posicion iria la fila si soltaras ahora: la que ocupa el lugar donde esta el
     * dedo. Fuera de la lista, se pega al extremo mas cercano.
     *
     * Es una funcion y no un valor calculado a proposito. `detectDragGestures` guarda los
     * callbacks que le pasaron al empezar, asi que un `val` de la composicion le llegaria
     * congelado en -1; una funcion vuelve a leer el estado cada vez que se la llama.
     */
    fun destinoActual(): Int {
        if (uidArrastrado == null) return -1
        val visibles = estadoLista.layoutInfo.visibleItemsInfo
        return visibles.firstOrNull { yDelDedo >= it.offset && yDelDedo < it.offset + it.size }
            ?.index
            ?: visibles.minByOrNull { abs(it.offset + it.size / 2f - yDelDedo) }?.index
            ?: -1
    }

    val destino = destinoActual()

    /**
     * Cuanto hay que correr cada fila para dibujar el arrastre.
     *
     * La arrastrada se centra en el dedo, calculado contra su posicion **actual** en el
     * layout: si la lista scrollea, la fila la sigue sin cuentas extra. Las del medio se
     * corren una posicion y dejan a la vista el hueco donde va a caer.
     */
    fun corrimientoDe(indice: Int, esLaArrastrada: Boolean): Float = when {
        esLaArrastrada -> filaArrastrada?.let { yDelDedo - (it.offset + it.size / 2f) } ?: 0f
        destino < 0 || origen < 0 -> 0f
        origen < destino && indice in (origen + 1)..destino -> -alturaFilaPx
        destino < origen && indice in destino until origen -> alturaFilaPx
        else -> 0f
    }

    // Con la cola mas larga que la pantalla, llevar una cancion arriba o abajo de todo
    // obliga a que la lista acompanie al dedo.
    LaunchedEffect(uidArrastrado) {
        if (uidArrastrado == null) return@LaunchedEffect
        while (true) {
            val actual = estadoLista.layoutInfo
            val paso = when {
                yDelDedo < actual.viewportStartOffset + alturaFilaPx -> -AUTO_SCROLL_PX
                yDelDedo > actual.viewportEndOffset - alturaFilaPx -> AUTO_SCROLL_PX
                else -> 0f
            }
            if (paso != 0f) estadoLista.scrollBy(paso)
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        // La fila que se arrastra se dibuja por encima de las demas.
                        .zIndex(if (arrastrando) 1f else 0f),
                ) {
                    QueueRow(
                        entry = entrada,
                        isDragging = arrastrando,
                        dragOffset = corrimientoDe(indice, arrastrando),
                        onClick = { onPlayIndex(baseIndex + indice) },
                        onRemove = { onRemove(baseIndex + indice) },
                    )

                    // Zona invisible que escucha el arrastre, encima del asa.
                    //
                    // Va aca afuera y NO adentro de la fila a proposito: la fila se mueve
                    // con `translationY`, y un nodo que se mueve junto con el dedo informa
                    // posiciones que casi no cambian. Esta zona se queda en el lugar que
                    // el layout le da a la fila. Una vez que el gesto empezo aca, Compose
                    // le sigue mandando ese dedo aunque se vaya lejos.
                    //
                    // Sumarle el `offset` de la fila pasa la posicion local a coordenadas
                    // de la lista, que es donde se compara contra las demas y lo unico
                    // que hace falta saber.
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(DRAG_ZONE_WIDTH)
                            .fillMaxHeight()
                            .pointerInput(entrada.uid) {
                                fun yEnLista(local: Float): Float {
                                    val fila = estadoLista.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == entrada.uid }
                                    return (fila?.offset ?: 0) + local
                                }

                                detectDragGestures(
                                    onDragStart = { posicion ->
                                        uidArrastrado = entrada.uid
                                        origen = queued.indexOfFirst { it.uid == entrada.uid }
                                        yDelDedo = yEnLista(posicion.y)
                                    },
                                    onDragEnd = {
                                        val hasta = destinoActual()
                                        if (origen >= 0 && hasta >= 0 && hasta != origen) {
                                            onMove(baseIndex + origen, baseIndex + hasta)
                                        }
                                        uidArrastrado = null
                                        origen = -1
                                    },
                                    onDragCancel = {
                                        uidArrastrado = null
                                        origen = -1
                                    },
                                    onDrag = { cambio, _ ->
                                        cambio.consume()
                                        yDelDedo = yEnLista(cambio.position.y)
                                    },
                                )
                            },
                    )
                }
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
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
        Box(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.queue_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
