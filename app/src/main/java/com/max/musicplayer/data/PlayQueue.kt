package com.max.musicplayer.data

/**
 * Una entrada de la cola.
 *
 * @param uid identificador unico de *esta aparicion* en la cola. No alcanza con el id de
 *   la cancion porque la misma cancion puede estar encolada dos veces.
 * @param ephemeral true si el usuario la encolo a mano ("Reproducir a continuacion" /
 *   "Agregar a la cola"). Estas se consumen: una vez que suenan, se van de la cola.
 */
data class QueueEntry(
    val uid: Long,
    val song: Song,
    val ephemeral: Boolean = false,
)

/**
 * Cola de reproduccion con cola efimera encima del contexto, al estilo Spotify.
 *
 * El "contexto" es la lista desde la que arrancaste (una carpeta, todas las canciones).
 * Encima de eso podes encolar temas sueltos: suenan a continuacion y despues la
 * reproduccion vuelve sola al contexto, sin haberlo modificado.
 *
 * Todo es inmutable y sin dependencias de Android: cada operacion devuelve una cola
 * nueva, lo que la hace trivial de testear y de comparar contra el estado del player.
 */
data class PlayQueue(
    val entries: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
) {
    val current: QueueEntry? get() = entries.getOrNull(currentIndex)

    val isEmpty: Boolean get() = entries.isEmpty()

    /** Lo que viene despues de la cancion actual. */
    val upNext: List<QueueEntry>
        get() = if (currentIndex < 0) entries else entries.drop(currentIndex + 1)

    /** Solo los temas encolados a mano que todavia no sonaron. */
    val pendingEphemeral: List<QueueEntry>
        get() = upNext.filter { it.ephemeral }

    /**
     * Posicion absoluta donde arranca el bloque de temas encolados a mano.
     * Van siempre juntos y justo despues de la actual, asi que alcanza con esta base
     * para traducir posiciones de la pantalla de cola a posiciones reales.
     */
    val ephemeralBaseIndex: Int
        get() = currentIndex + 1

    /**
     * El contexto: la carpeta o lista desde la que se empezo a escuchar, sin los temas
     * encolados a mano.
     *
     * Es lo que la pantalla de reproduccion muestra abajo. Contexto y cola son cosas
     * distintas: poner una cancion de una carpeta no "crea una cola", solo fija un
     * contexto; la cola son los temas que el usuario agrega aparte.
     */
    val contextEntries: List<QueueEntry>
        get() = entries.filter { !it.ephemeral }

    /** Posicion de la actual dentro del contexto, o -1 si lo que suena es un encolado. */
    val currentContextIndex: Int
        get() {
            val actual = current ?: return -1
            if (actual.ephemeral) return -1
            return contextEntries.indexOfFirst { it.uid == actual.uid }
        }

    /**
     * Inserta [song] para que suene inmediatamente despues de la actual.
     * Si se llama varias veces, la ultima queda primera (como "Reproducir a continuacion").
     */
    fun playNext(song: Song, uid: Long): PlayQueue =
        insertAt(currentIndex + 1, QueueEntry(uid, song, ephemeral = true))

    /**
     * Agrega [song] al final de la cola efimera: despues de los temas ya encolados a mano,
     * pero antes de que se retome el contexto.
     */
    fun addToQueue(song: Song, uid: Long): PlayQueue =
        insertAt(endOfEphemeralBlock(), QueueEntry(uid, song, ephemeral = true))

    /**
     * Posicion justo despues del ultimo tema efimero contiguo que sigue a la actual.
     * Si no hay ninguno, es la posicion siguiente a la actual.
     */
    private fun endOfEphemeralBlock(): Int {
        var i = currentIndex + 1
        while (i < entries.size && entries[i].ephemeral) i++
        return i
    }

    private fun insertAt(index: Int, entry: QueueEntry): PlayQueue {
        val target = index.coerceIn(0, entries.size)
        val nuevas = entries.toMutableList().apply { add(target, entry) }
        val nuevoIndice = when {
            // Encolar sobre una cola vacia: esa entrada pasa a ser la actual,
            // si no quedaria en -1 y no sonaria nada.
            entries.isEmpty() -> 0
            // Insertar por delante de la actual la correria un lugar. Hoy siempre
            // insertamos despues, pero lo dejamos explicito para que siga siendo correcto.
            target <= currentIndex -> currentIndex + 1
            else -> currentIndex
        }
        return copy(entries = nuevas, currentIndex = nuevoIndice)
    }

    /**
     * Mueve la reproduccion a [index] y descarta los temas efimeros que quedaron atras:
     * ya sonaron, asi que salen de la cola.
     */
    fun moveToIndex(index: Int): PlayQueue {
        if (index !in entries.indices) return this

        val objetivo = entries[index]
        val sobrevivientes = entries.filterIndexed { i, e ->
            i >= index || !e.ephemeral
        }
        val nuevoIndice = sobrevivientes.indexOfFirst { it.uid == objetivo.uid }
        return copy(entries = sobrevivientes, currentIndex = nuevoIndice)
    }

    /** Saca una entrada de la cola, corrigiendo el indice actual. */
    fun removeAt(index: Int): PlayQueue {
        if (index !in entries.indices) return this

        val nuevas = entries.toMutableList().apply { removeAt(index) }
        val nuevoIndice = when {
            nuevas.isEmpty() -> -1
            index < currentIndex -> currentIndex - 1
            // Se borro la que sonaba: el indice se queda apuntando a la que ocupa su lugar.
            index == currentIndex -> currentIndex.coerceAtMost(nuevas.lastIndex)
            else -> currentIndex
        }
        return copy(entries = nuevas, currentIndex = nuevoIndice)
    }

    /** Reordena la cola arrastrando una entrada de [from] a [to]. */
    fun move(from: Int, to: Int): PlayQueue {
        if (from !in entries.indices || to !in entries.indices || from == to) return this

        val actual = current
        val nuevas = entries.toMutableList()
        val movida = nuevas.removeAt(from) // MutableList.removeAt, no el de PlayQueue
        nuevas.add(to, movida)
        val nuevoIndice = if (actual == null) currentIndex
        else nuevas.indexOfFirst { it.uid == actual.uid }
        return copy(entries = nuevas, currentIndex = nuevoIndice)
    }

    /** Vacia la cola efimera pendiente sin tocar el contexto ni lo que esta sonando. */
    fun clearEphemeral(): PlayQueue {
        val actual = current
        val nuevas = entries.filterIndexed { i, e -> i <= currentIndex || !e.ephemeral }
        val nuevoIndice = if (actual == null) -1 else nuevas.indexOfFirst { it.uid == actual.uid }
        return copy(entries = nuevas, currentIndex = nuevoIndice)
    }

    companion object {
        /**
         * Arranca una cola nueva desde un contexto (una carpeta, la lista completa),
         * empezando por [startIndex].
         */
        fun fromContext(songs: List<Song>, startIndex: Int, uidFrom: Long = 0L): PlayQueue =
            PlayQueue(
                entries = songs.mapIndexed { i, s -> QueueEntry(uidFrom + i, s, ephemeral = false) },
                currentIndex = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
                    .takeIf { songs.isNotEmpty() } ?: -1,
            )
    }
}
