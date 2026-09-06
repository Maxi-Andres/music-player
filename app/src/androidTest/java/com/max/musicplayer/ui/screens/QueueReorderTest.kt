package com.max.musicplayer.ui.screens

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.max.musicplayer.data.QueueEntry
import com.max.musicplayer.data.Song
import org.junit.Rule
import org.junit.Test

/**
 * Reordenar la cola arrastrando, sobre un dispositivo real.
 *
 * Existe porque este gesto se rompio tres veces seguidas y ningun unit test lo agarraba:
 * las fallas estaban en como Compose entrega las posiciones del dedo a un nodo que se
 * mueve, algo que solo se ve ejecutando la UI de verdad.
 *
 * La pantalla se monta con una lista mutable propia y [PlayQueue] no participa: lo que se
 * verifica es que **el gesto pida el movimiento correcto**, que es donde estaba el error.
 */
class QueueReorderTest {

    @get:Rule
    val compose = createComposeRule()

    /** Alto de fila de QueueScreen, en dp. Tiene que coincidir con ROW_HEIGHT. */
    private val altoFilaDp = 64f

    private fun cancion(n: Int) = Song(
        id = n.toLong(),
        title = "Tema $n",
        artist = "Artista",
        album = "Album",
        albumId = 1L,
        durationMs = 180_000L,
        filePath = "/musica/tema$n.mp3",
        dateModifiedSeconds = 0L,
    )

    /** Lo que el gesto pidio mover, para poder distinguir "no se detecto" de "dio mal". */
    private val movimientos = mutableListOf<Pair<Int, Int>>()

    /** Monta la pantalla con [cantidad] temas y devuelve la lista viva. */
    private fun montar(cantidad: Int): MutableList<QueueEntry> {
        val cola = mutableStateListOf<QueueEntry>().apply {
            addAll((1..cantidad).map { QueueEntry(it.toLong(), cancion(it), ephemeral = true) })
        }
        compose.setContent {
            QueueScreen(
                queued = cola,
                baseIndex = 0,
                onClose = {},
                onPlayIndex = {},
                onRemove = {},
                onMove = { desde, hasta ->
                    movimientos += desde to hasta
                    // Lo mismo que hace PlayQueue.move con la cola real.
                    if (desde in cola.indices && hasta in cola.indices) {
                        cola.add(hasta, cola.removeAt(desde))
                    }
                },
                onClearAll = {},
            )
        }
        return cola
    }

    private fun titulos(cola: List<QueueEntry>) = cola.map { it.song.title }

    /**
     * Arrastra la fila que esta en [desde] hasta la posicion [hasta].
     *
     * El gesto se hace sobre el asa, que esta contra el borde derecho de la fila.
     */
    private fun arrastrar(desde: Int, hasta: Int) {
        val asa: SemanticsNodeInteraction = compose.onAllNodesWithTag(QUEUE_DRAG_TAG)[desde]
        val altoPx = with(compose.density) { altoFilaDp.dp.toPx() }
        val recorrido = (hasta - desde) * altoPx

        asa.performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x, center.y + recorrido),
                durationMillis = 400,
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun el_gesto_se_detecta_y_pide_el_movimiento_correcto() {
        montar(6)

        arrastrar(desde = 0, hasta = 1)

        // Si queda vacio, el arrastre ni se detecto; si trae otros numeros, la cuenta
        // de la posicion destino esta mal.
        assertThat(movimientos).containsExactly(0 to 1)
    }

    @Test
    fun mueve_una_posicion_para_abajo() {
        val cola = montar(6)

        arrastrar(desde = 0, hasta = 1)

        assertThat(titulos(cola))
            .containsExactly("Tema 2", "Tema 1", "Tema 3", "Tema 4", "Tema 5", "Tema 6")
            .inOrder()
    }

    @Test
    fun mueve_una_posicion_para_arriba() {
        val cola = montar(6)

        arrastrar(desde = 3, hasta = 2)

        assertThat(titulos(cola))
            .containsExactly("Tema 1", "Tema 2", "Tema 4", "Tema 3", "Tema 5", "Tema 6")
            .inOrder()
    }

    @Test
    fun mueve_varias_posiciones_de_una() {
        val cola = montar(6)

        arrastrar(desde = 0, hasta = 4)

        assertThat(titulos(cola))
            .containsExactly("Tema 2", "Tema 3", "Tema 4", "Tema 5", "Tema 1", "Tema 6")
            .inOrder()
    }

    @Test
    fun sube_hasta_arriba_de_todo_y_despues_puede_volver_a_bajar() {
        // Es el caso que reporto el usuario: subir funcionaba, pero despues no se podia
        // bajar la misma fila.
        val cola = montar(6)

        arrastrar(desde = 4, hasta = 0)
        assertThat(titulos(cola).first()).isEqualTo("Tema 5")

        arrastrar(desde = 0, hasta = 3)
        assertThat(titulos(cola))
            .containsExactly("Tema 1", "Tema 2", "Tema 3", "Tema 5", "Tema 4", "Tema 6")
            .inOrder()
    }

    @Test
    fun aguanta_una_seguidilla_de_movimientos() {
        // La secuencia que pidio el usuario: mover varias veces distintas filas, para
        // arriba y para abajo, y que el orden acompanie en cada paso.
        val cola = montar(8)

        arrastrar(desde = 0, hasta = 2)
        assertThat(titulos(cola).take(3)).containsExactly("Tema 2", "Tema 3", "Tema 1").inOrder()

        arrastrar(desde = 7, hasta = 0)
        assertThat(titulos(cola).first()).isEqualTo("Tema 8")

        arrastrar(desde = 0, hasta = 7)
        assertThat(titulos(cola).last()).isEqualTo("Tema 8")

        // Queda [2, 3, 1, 4, 5, 6, 7, 8]: mover la 4 (posicion 3) a la 1 da [2, 4, 3, 1, ...].
        arrastrar(desde = 3, hasta = 1)
        assertThat(titulos(cola))
            .containsExactly(
                "Tema 2", "Tema 4", "Tema 3", "Tema 1", "Tema 5", "Tema 6", "Tema 7", "Tema 8",
            )
            .inOrder()
    }

    @Test
    fun la_zona_de_arrastre_cubre_el_asa_que_se_ve() {
        // El test agarra la zona por etiqueta, pero el dedo agarra el asa dibujada. Si la
        // zona no la cubriera, el gesto andaria en el test y no en la mano.
        montar(6)

        val zona = compose.onAllNodesWithTag(QUEUE_DRAG_TAG)[0].fetchSemanticsNode()
            .boundsInRoot
        val asa = compose.onAllNodes(hasContentDescription("Reordenar"), useUnmergedTree = true)[0]
            .fetchSemanticsNode()
            .boundsInRoot

        assertThat(asa.left).isAtLeast(zona.left)
        assertThat(asa.right).isAtMost(zona.right)
        assertThat(asa.top).isAtLeast(zona.top)
        assertThat(asa.bottom).isAtMost(zona.bottom)
    }

    @Test
    fun tambien_arrastra_agarrando_el_asa_dibujada() {
        // El mismo movimiento pero apuntando donde apunta el dedo.
        val cola = montar(6)
        val asa = compose.onAllNodes(hasContentDescription("Reordenar"), useUnmergedTree = true)[0]
        val altoPx = with(compose.density) { altoFilaDp.dp.toPx() }

        asa.performTouchInput {
            swipe(start = center, end = Offset(center.x, center.y + altoPx), durationMillis = 400)
        }
        compose.waitForIdle()

        assertThat(titulos(cola).take(2)).containsExactly("Tema 2", "Tema 1").inOrder()
    }

    @Test
    fun arrastrar_y_volver_al_mismo_lugar_no_cambia_nada() {
        val cola = montar(6)
        val antes = titulos(cola)

        arrastrar(desde = 2, hasta = 2)

        assertThat(titulos(cola)).isEqualTo(antes)
    }
}
