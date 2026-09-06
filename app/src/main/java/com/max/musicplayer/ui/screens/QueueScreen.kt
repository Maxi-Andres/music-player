package com.max.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.max.musicplayer.R
import com.max.musicplayer.data.QueueEntry
import com.max.musicplayer.ui.components.AlbumArt
import kotlin.math.roundToInt
import kotlin.math.floor

private val ROW_HEIGHT = 64.dp

/**
 * Ancho de la zona que captura el arrastre, medido desde el borde derecho: cubre el asa
 * y el margen de la fila, y termina justo donde arranca la X de quitar.
 */
private val DRAG_ZONE_WIDTH = 48.dp

/**
 * Zona sensible del auto-scroll en cada punta, medida en filas. Ancha a proposito: con un
 * borde finito hay que apuntar con el dedo y parece que no funcionara.
 */
private const val AUTO_SCROLL_ZONE_ROWS = 1.5f

/** Velocidad maxima del auto-scroll, en pixeles por cuadro, al fondo de la zona. */
private const val AUTO_SCROLL_MAX_PX = 26f

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

    // El reordenamiento se calcula mientras arrastras y se aplica una sola vez al soltar:
    // mover en cada salto reubicaba la fila en la composicion (la lista usa el uid de
    // clave) y eso desmontaba el nodo que escuchaba el gesto.
    //
    // Todo se calcula con **posiciones**, no acumulando distancias. Acumular fallaba por
    // partida doble: la fila trasladada informa diferencias que tienden a cero (Compose
    // da la posicion relativa al nodo ya movido) y el auto-scroll las contaba dos veces.
    // Como todas las filas miden lo mismo, alcanza con aritmetica sobre el layout.
    // `detectDragGestures` se queda con los callbacks del momento en que arranco el
    // gesto, y la cola llega del reproductor como una **lista nueva** en cada cambio. Sin
    // esto, despues del primer movimiento el gesto seguia calculando posiciones sobre el
    // orden viejo: mover dos veces la misma fila daba cualquier cosa.
    val colaActual by rememberUpdatedState(queued)
    val baseActual by rememberUpdatedState(baseIndex)
    val moverActual by rememberUpdatedState(onMove)

    var uidArrastrado by remember { mutableStateOf<Long?>(null) }
    var indiceOrigen by remember { mutableIntStateOf(-1) }
    // Posicion que hay que dejar a la vista despues de soltar.
    var mostrarFila by remember { mutableIntStateOf(-1) }
    // Donde dentro de la fila la agarraste. Sin esto la fila salta para centrarse en el
    // dedo apenas empieza el gesto.
    var agarreEnFila by remember { mutableFloatStateOf(0f) }
    // Posicion del dedo en coordenadas de la lista (no de la fila, que se mueve).
    var yDelDedo by remember { mutableFloatStateOf(0f) }

    /** Donde deja el layout a la fila [indice]. Vale tambien fuera de la pantalla. */
    fun offsetDe(indice: Int): Float {
        val primera = estadoLista.layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0f
        return primera.offset + (indice - primera.index) * alturaFilaPx
    }

    /** Borde de arriba de la fila arrastrada, pegado al dedo. */
    fun topeArrastrado(): Float = yDelDedo - agarreEnFila

    /**
     * A que posicion iria si soltaras ahora: la ranura que ocupa el centro de la fila.
     *
     * Es funcion y no un valor de la composicion porque `detectDragGestures` se queda con
     * los callbacks del momento en que empezo el gesto; un `val` le llegaria congelado.
     */
    fun destinoActual(): Int {
        if (indiceOrigen < 0 || colaActual.isEmpty()) return -1
        val primera = estadoLista.layoutInfo.visibleItemsInfo.firstOrNull()
            ?: return indiceOrigen
        val centro = topeArrastrado() + alturaFilaPx / 2f
        val ranura = primera.index + floor((centro - primera.offset) / alturaFilaPx).toInt()
        return ranura.coerceIn(0, colaActual.lastIndex)
    }

    val destino = destinoActual()

    /**
     * Cuanto correr una fila que NO es la que se arrastra, para dejar a la vista el hueco
     * donde va a caer.
     */
    fun corrimientoDe(indice: Int): Float = when {
        destino < 0 || indiceOrigen < 0 -> 0f
        indiceOrigen < destino && indice in (indiceOrigen + 1)..destino -> -alturaFilaPx
        destino < indiceOrigen && indice in destino until indiceOrigen -> alturaFilaPx
        else -> 0f
    }

    // Al soltar, la fila tiene que quedar a la vista. Sin esto, mandarla arriba de todo
    // dejaba la lista un renglon corrida y habia que scrollear para verla.
    LaunchedEffect(mostrarFila) {
        val objetivo = mostrarFila
        if (objetivo < 0) return@LaunchedEffect
        val info = estadoLista.layoutInfo
        val fila = info.visibleItemsInfo.firstOrNull { it.index == objetivo }
        val entera = fila != null &&
            fila.offset >= info.viewportStartOffset &&
            fila.offset + fila.size <= info.viewportEndOffset
        if (!entera) estadoLista.animateScrollToItem(objetivo)
        mostrarFila = -1
    }

    // Con la cola mas larga que la pantalla, la lista acompania al dedo. La zona sensible
    // es ancha a proposito: con un borde finito hay que apuntar y se siente que no anda.
    // La velocidad crece cuanto mas adentro de la zona estas.
    LaunchedEffect(uidArrastrado) {
        if (uidArrastrado == null) return@LaunchedEffect
        while (true) {
            val info = estadoLista.layoutInfo
            val zona = alturaFilaPx * AUTO_SCROLL_ZONE_ROWS
            val tope = topeArrastrado()
            val faltaArriba = (info.viewportStartOffset + zona) - tope
            val faltaAbajo = (tope + alturaFilaPx) - (info.viewportEndOffset - zona)

            val paso = when {
                faltaArriba > 0f -> -(faltaArriba / zona).coerceIn(0f, 1f) * AUTO_SCROLL_MAX_PX
                faltaAbajo > 0f -> (faltaAbajo / zona).coerceIn(0f, 1f) * AUTO_SCROLL_MAX_PX
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

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = estadoLista, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(queued, key = { _, entrada -> entrada.uid }) { indice, entrada ->
                    if (entrada.uid == uidArrastrado) {
                        // Su lugar queda vacio: la fila se dibuja flotando mas abajo.
                        Spacer(modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT))
                    } else {
                        QueueRow(
                            entry = entrada,
                            isDragging = false,
                            dragOffset = corrimientoDe(indice),
                            onClick = { onPlayIndex(baseActual + indice) },
                            onRemove = { onRemove(baseActual + indice) },
                        )
                    }
                }
            }

            // La fila que se esta arrastrando, dibujada aparte y encima de la lista.
            //
            // No puede ir dentro del LazyColumn: al arrastrarla lejos su posicion se sale
            // de la pantalla, la lista deja de componer ese lugar y la fila desaparecia a
            // mitad del gesto (volvia a aparecer recien al soltar). Aca no depende de que
            // la lista la tenga a la vista.
            val enArrastre = colaActual.firstOrNull { it.uid == uidArrastrado }
            if (enArrastre != null) {
                QueueRow(
                    entry = enArrastre,
                    isDragging = true,
                    dragOffset = 0f,
                    onClick = {},
                    onRemove = {},
                    modifier = Modifier.offset {
                        IntOffset(0, topeArrastrado().roundToInt())
                    },
                )
            }

            // Franja invisible sobre las asas que escucha el arrastre.
            //
            // Vive **afuera** de la lista, no dentro de cada fila. Dos razones, las dos
            // aprendidas rompiendo esto:
            //
            //  - Adentro de la fila, el nodo se mueve con `translationY` y Compose informa
            //    la posicion relativa al nodo ya movido: la distancia medida tiende a cero.
            //  - Y al arrastrar lejos, la ranura original se iba de la pantalla, LazyColumn
            //    reciclaba la fila y con ella el nodo del gesto: el arrastre se cortaba solo
            //    y la fila volvia a su lugar.
            //
            // Aca la franja no se mueve ni se recicla nunca, y sus coordenadas coinciden
            // con las del viewport de la lista, que es contra lo que se compara todo.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(DRAG_ZONE_WIDTH)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { toque ->
                                val fila = estadoLista.layoutInfo.visibleItemsInfo
                                    .firstOrNull {
                                        toque.y >= it.offset && toque.y < it.offset + it.size
                                    }
                                if (fila != null) {
                                    indiceOrigen = fila.index
                                    agarreEnFila = toque.y - fila.offset
                                    yDelDedo = toque.y
                                    uidArrastrado = fila.key as? Long
                                }
                            },
                            onDragEnd = {
                                val hasta = destinoActual()
                                if (indiceOrigen >= 0 && hasta >= 0 && hasta != indiceOrigen) {
                                    moverActual(baseActual + indiceOrigen, baseActual + hasta)
                                    mostrarFila = hasta
                                }
                                uidArrastrado = null
                                indiceOrigen = -1
                            },
                            onDragCancel = {
                                uidArrastrado = null
                                indiceOrigen = -1
                            },
                            onDrag = { cambio, _ ->
                                cambio.consume()
                                if (indiceOrigen >= 0) yDelDedo = cambio.position.y
                            },
                        )
                    },
            )
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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
