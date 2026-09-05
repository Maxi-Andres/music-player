package com.max.musicplayer.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayQueueTest {

    private val contexto = (1..5).map { song(id = it.toLong(), title = "Tema $it") }
    private val extra = song(id = 99, title = "Encolada")
    private val otraExtra = song(id = 98, title = "Encolada 2")

    private fun PlayQueue.titulos() = entries.map { it.song.title }

    // --- construccion ---

    @Test
    fun `fromContext arranca en la posicion pedida y nada es efimero`() {
        val q = PlayQueue.fromContext(contexto, startIndex = 2)

        assertThat(q.entries).hasSize(5)
        assertThat(q.currentIndex).isEqualTo(2)
        assertThat(q.current?.song?.title).isEqualTo("Tema 3")
        assertThat(q.entries.none { it.ephemeral }).isTrue()
    }

    @Test
    fun `fromContext con lista vacia deja la cola vacia y sin actual`() {
        val q = PlayQueue.fromContext(emptyList(), startIndex = 0)

        assertThat(q.isEmpty).isTrue()
        assertThat(q.currentIndex).isEqualTo(-1)
        assertThat(q.current).isNull()
    }

    @Test
    fun `fromContext acota un indice fuera de rango`() {
        assertThat(PlayQueue.fromContext(contexto, startIndex = 99).currentIndex).isEqualTo(4)
        assertThat(PlayQueue.fromContext(contexto, startIndex = -3).currentIndex).isEqualTo(0)
    }

    @Test
    fun `las entradas tienen uid distinto aunque se repita la cancion`() {
        val repetida = listOf(extra, extra, extra)

        val uids = PlayQueue.fromContext(repetida, 0).entries.map { it.uid }

        assertThat(uids.toSet()).hasSize(3)
    }

    // --- reproducir a continuacion ---

    @Test
    fun `playNext deja la cancion justo despues de la actual`() {
        val q = PlayQueue.fromContext(contexto, 1).playNext(extra, uid = 100)

        assertThat(q.titulos()).containsExactly(
            "Tema 1", "Tema 2", "Encolada", "Tema 3", "Tema 4", "Tema 5",
        ).inOrder()
    }

    @Test
    fun `playNext no mueve la cancion que esta sonando`() {
        val q = PlayQueue.fromContext(contexto, 1).playNext(extra, uid = 100)

        assertThat(q.currentIndex).isEqualTo(1)
        assertThat(q.current?.song?.title).isEqualTo("Tema 2")
    }

    @Test
    fun `dos playNext seguidos dejan primero al ultimo pedido`() {
        val q = PlayQueue.fromContext(contexto, 0)
            .playNext(extra, uid = 100)
            .playNext(otraExtra, uid = 101)

        assertThat(q.upNext.take(2).map { it.song.title })
            .containsExactly("Encolada 2", "Encolada").inOrder()
    }

    @Test
    fun `lo encolado a mano queda marcado como efimero`() {
        val q = PlayQueue.fromContext(contexto, 0).playNext(extra, uid = 100)

        assertThat(q.entries[1].ephemeral).isTrue()
        assertThat(q.pendingEphemeral.map { it.song.title }).containsExactly("Encolada")
    }

    // --- agregar a la cola ---

    @Test
    fun `addToQueue se ubica despues de los ya encolados, no pisa el orden`() {
        val q = PlayQueue.fromContext(contexto, 0)
            .addToQueue(extra, uid = 100)
            .addToQueue(otraExtra, uid = 101)

        assertThat(q.upNext.take(2).map { it.song.title })
            .containsExactly("Encolada", "Encolada 2").inOrder()
    }

    @Test
    fun `addToQueue sin nada encolado equivale a reproducir a continuacion`() {
        val q = PlayQueue.fromContext(contexto, 2).addToQueue(extra, uid = 100)

        assertThat(q.entries[3].song.title).isEqualTo("Encolada")
    }

    @Test
    fun `addToQueue no toca las canciones del contexto que vienen despues`() {
        val q = PlayQueue.fromContext(contexto, 0).addToQueue(extra, uid = 100)

        val delContexto = q.entries.filter { !it.ephemeral }.map { it.song.title }
        assertThat(delContexto)
            .containsExactly("Tema 1", "Tema 2", "Tema 3", "Tema 4", "Tema 5").inOrder()
    }

    @Test
    fun `encolar sobre una cola vacia hace sonar esa cancion`() {
        val q = PlayQueue().addToQueue(extra, uid = 100)

        assertThat(q.currentIndex).isEqualTo(0)
        assertThat(q.current?.song?.title).isEqualTo("Encolada")
    }

    // --- consumo: lo efimero desaparece despues de sonar ---

    @Test
    fun `al pasar de largo, la cancion efimera se va de la cola`() {
        val q = PlayQueue.fromContext(contexto, 0)
            .playNext(extra, uid = 100) // queda en indice 1

        val despues = q.moveToIndex(2) // avanzamos mas alla de la encolada

        assertThat(despues.titulos()).doesNotContain("Encolada")
    }

    @Test
    fun `despues de consumir lo efimero se retoma el contexto donde estaba`() {
        val q = PlayQueue.fromContext(contexto, 0)
            .playNext(extra, uid = 100)
            .moveToIndex(2) // la siguiente del contexto era Tema 2

        assertThat(q.current?.song?.title).isEqualTo("Tema 2")
        assertThat(q.titulos())
            .containsExactly("Tema 1", "Tema 2", "Tema 3", "Tema 4", "Tema 5").inOrder()
    }

    @Test
    fun `mientras suena la efimera todavia sigue en la cola`() {
        val q = PlayQueue.fromContext(contexto, 0)
            .playNext(extra, uid = 100)
            .moveToIndex(1) // ahora suena la encolada

        assertThat(q.current?.song?.title).isEqualTo("Encolada")
        assertThat(q.titulos()).contains("Encolada")
    }

    @Test
    fun `las canciones del contexto no se consumen al avanzar`() {
        val q = PlayQueue.fromContext(contexto, 0).moveToIndex(3)

        assertThat(q.entries).hasSize(5)
        assertThat(q.current?.song?.title).isEqualTo("Tema 4")
    }

    @Test
    fun `moveToIndex con un indice invalido no cambia nada`() {
        val q = PlayQueue.fromContext(contexto, 1)

        assertThat(q.moveToIndex(99)).isEqualTo(q)
        assertThat(q.moveToIndex(-1)).isEqualTo(q)
    }

    // --- quitar y reordenar ---

    @Test
    fun `removeAt de una posterior no mueve la actual`() {
        val q = PlayQueue.fromContext(contexto, 1).removeAt(3)

        assertThat(q.current?.song?.title).isEqualTo("Tema 2")
        assertThat(q.entries).hasSize(4)
    }

    @Test
    fun `removeAt de una anterior corrige el indice actual`() {
        val q = PlayQueue.fromContext(contexto, 3).removeAt(0)

        assertThat(q.currentIndex).isEqualTo(2)
        assertThat(q.current?.song?.title).isEqualTo("Tema 4")
    }

    @Test
    fun `borrar la que suena deja apuntando a la que ocupa su lugar`() {
        val q = PlayQueue.fromContext(contexto, 1).removeAt(1)

        assertThat(q.current?.song?.title).isEqualTo("Tema 3")
    }

    @Test
    fun `borrar la ultima cuando era la actual no se pasa de rango`() {
        val q = PlayQueue.fromContext(contexto, 4).removeAt(4)

        assertThat(q.currentIndex).isEqualTo(3)
        assertThat(q.current?.song?.title).isEqualTo("Tema 4")
    }

    @Test
    fun `vaciar la cola entera deja el indice en -1`() {
        var q = PlayQueue.fromContext(listOf(contexto.first()), 0)
        q = q.removeAt(0)

        assertThat(q.isEmpty).isTrue()
        assertThat(q.currentIndex).isEqualTo(-1)
    }

    @Test
    fun `move reordena y la actual sigue siendo la misma cancion`() {
        val q = PlayQueue.fromContext(contexto, 0).move(from = 4, to = 1)

        assertThat(q.titulos())
            .containsExactly("Tema 1", "Tema 5", "Tema 2", "Tema 3", "Tema 4").inOrder()
        assertThat(q.current?.song?.title).isEqualTo("Tema 1")
    }

    @Test
    fun `move que arrastra la actual la sigue apuntando`() {
        val q = PlayQueue.fromContext(contexto, 0).move(from = 0, to = 3)

        assertThat(q.current?.song?.title).isEqualTo("Tema 1")
        assertThat(q.currentIndex).isEqualTo(3)
    }

    @Test
    fun `move con indices invalidos no cambia nada`() {
        val q = PlayQueue.fromContext(contexto, 0)

        assertThat(q.move(0, 0)).isEqualTo(q)
        assertThat(q.move(-1, 2)).isEqualTo(q)
        assertThat(q.move(0, 99)).isEqualTo(q)
    }

    // --- limpiar la cola efimera ---

    @Test
    fun `clearEphemeral saca lo encolado pendiente y deja el contexto intacto`() {
        val q = PlayQueue.fromContext(contexto, 1)
            .addToQueue(extra, uid = 100)
            .addToQueue(otraExtra, uid = 101)
            .clearEphemeral()

        assertThat(q.titulos())
            .containsExactly("Tema 1", "Tema 2", "Tema 3", "Tema 4", "Tema 5").inOrder()
        assertThat(q.current?.song?.title).isEqualTo("Tema 2")
    }

    @Test
    fun `clearEphemeral no corta la cancion efimera que esta sonando`() {
        val q = PlayQueue.fromContext(contexto, 0)
            .playNext(extra, uid = 100)
            .moveToIndex(1) // suena la encolada
            .clearEphemeral()

        assertThat(q.current?.song?.title).isEqualTo("Encolada")
    }

    @Test
    fun `upNext es lo que viene despues de la actual`() {
        val q = PlayQueue.fromContext(contexto, 2)

        assertThat(q.upNext.map { it.song.title }).containsExactly("Tema 4", "Tema 5").inOrder()
    }
}
