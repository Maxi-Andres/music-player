package com.max.musicplayer.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Reordenar el contexto (lo que hace el boton de aleatorio).
 *
 * Estos tests existen por un bug concreto: el aleatorio se delegaba a
 * `shuffleModeEnabled` de ExoPlayer, que lleva un orden interno que la app no ve. La
 * pantalla mostraba un orden y el reproductor seguia otro; al terminarse el orden
 * interno la reproduccion frenaba y "siguiente" no hacia nada. Ahora se reordena la
 * lista real, y esto lo verifica.
 */
class PlayQueueReorderTest {

    private val contexto = (1..5).map { song(id = it.toLong(), title = "Tema $it") }
    private val extra = song(id = 99, title = "Encolada")

    private fun PlayQueue.titulos() = entries.map { it.song.title }

    @Test
    fun `reordenar no corta la cancion que esta sonando`() {
        val q = PlayQueue.fromContext(contexto, 2)
        val alReves = q.contextEntries.reversed()

        val nueva = q.withContextOrder(alReves)

        assertThat(nueva.current?.song?.title).isEqualTo("Tema 3")
    }

    @Test
    fun `el orden nuevo es exactamente el que se pidio`() {
        val q = PlayQueue.fromContext(contexto, 0)
        val alReves = q.contextEntries.reversed()

        val nueva = q.withContextOrder(alReves)

        assertThat(nueva.titulos())
            .containsExactly("Tema 5", "Tema 4", "Tema 3", "Tema 2", "Tema 1").inOrder()
    }

    @Test
    fun `siempre queda algo despues salvo que la actual sea la ultima`() {
        val q = PlayQueue.fromContext(contexto, 0)
        val nueva = q.withContextOrder(q.contextEntries.shuffled())

        // El bug hacia que el reproductor se quedara sin siguiente aunque en pantalla
        // hubiera temas mas adelante: aca se comprueba que ambas cosas coinciden.
        val quedanDespues = nueva.entries.size - 1 - nueva.currentIndex
        assertThat(quedanDespues).isEqualTo(nueva.upNext.size)
    }

    @Test
    fun `los encolados a mano siguen pegados detras de la actual`() {
        val q = PlayQueue.fromContext(contexto, 1).playNext(extra, uid = 100)

        val nueva = q.withContextOrder(q.contextEntries.reversed())

        val posActual = nueva.currentIndex
        assertThat(nueva.entries[posActual].song.title).isEqualTo("Tema 2")
        assertThat(nueva.entries[posActual + 1].song.title).isEqualTo("Encolada")
    }

    @Test
    fun `si lo que suena es un encolado, queda arriba y el contexto detras`() {
        val q = PlayQueue.fromContext(contexto, 0)
            .playNext(extra, uid = 100)
            .moveToIndex(1)

        val nueva = q.withContextOrder(q.contextEntries)

        assertThat(nueva.currentIndex).isEqualTo(0)
        assertThat(nueva.current?.song?.title).isEqualTo("Encolada")
        assertThat(nueva.upNext.map { it.song.title })
            .containsExactly("Tema 1", "Tema 2", "Tema 3", "Tema 4", "Tema 5").inOrder()
    }

    @Test
    fun `reordenar no pierde ni duplica canciones`() {
        val q = PlayQueue.fromContext(contexto, 3).playNext(extra, uid = 100)

        val nueva = q.withContextOrder(q.contextEntries.shuffled())

        assertThat(nueva.entries.map { it.uid })
            .containsExactlyElementsIn(q.entries.map { it.uid })
    }

    @Test
    fun `volver al orden original deja el contexto como estaba`() {
        val original = PlayQueue.fromContext(contexto, 0)
        val ordenOriginal = original.contextEntries

        val mezclada = original.withContextOrder(ordenOriginal.shuffled())
        val restaurada = mezclada.withContextOrder(ordenOriginal)

        assertThat(restaurada.titulos())
            .containsExactly("Tema 1", "Tema 2", "Tema 3", "Tema 4", "Tema 5").inOrder()
    }
}
