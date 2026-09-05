package com.max.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.max.musicplayer.data.PlayQueue
import com.max.musicplayer.data.QueueEntry
import com.max.musicplayer.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Lo que la UI necesita saber del reproductor en cada momento. */
data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isConnected: Boolean = false,
    /** Id de sesion de audio, necesario para el ecualizador. 0 = todavia desconocido. */
    val audioSessionId: Int = 0,
)

fun Song.toMediaItem(uid: Long, ephemeral: Boolean = false): MediaItem = MediaItem.Builder()
    // El uid va en el mediaId para poder reencontrar la entrada exacta en la cola
    // aunque la misma cancion este encolada mas de una vez.
    .setMediaId("$uid|$id")
    .setUri(contentUri.toUri())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(displayArtist)
            .setAlbumTitle(album)
            // Apunta al archivo de audio: AudioArtworkBitmapLoader le saca la tapa
            // embebida. La caratula por album mostraba la de otra cancion.
            .setArtworkUri(contentUri.toUri())
            // Los datos completos viajan con el item para poder rearmar la cola tal
            // cual si la app se cierra y se vuelve a abrir con la musica sonando.
            .setExtras(
                Bundle().apply {
                    putLong(EXTRA_SONG_ID, id)
                    putLong(EXTRA_ALBUM_ID, albumId)
                    putLong(EXTRA_DURATION, durationMs)
                    putLong(EXTRA_DATE, dateModifiedSeconds)
                    putString(EXTRA_TITLE, title)
                    putString(EXTRA_ARTIST, artist)
                    putString(EXTRA_ALBUM, album)
                    putString(EXTRA_PATH, filePath)
                    putBoolean(EXTRA_EPHEMERAL, ephemeral)
                },
            )
            .build(),
    )
    .build()

/** Rehace la entrada de cola a partir de un item del reproductor. */
fun MediaItem.toQueueEntry(): QueueEntry? {
    val extras = mediaMetadata.extras ?: return null
    val uid = mediaId.substringBefore('|').toLongOrNull() ?: return null
    val song = Song(
        id = extras.getLong(EXTRA_SONG_ID),
        title = extras.getString(EXTRA_TITLE).orEmpty(),
        artist = extras.getString(EXTRA_ARTIST).orEmpty(),
        album = extras.getString(EXTRA_ALBUM).orEmpty(),
        albumId = extras.getLong(EXTRA_ALBUM_ID),
        durationMs = extras.getLong(EXTRA_DURATION),
        filePath = extras.getString(EXTRA_PATH).orEmpty(),
        dateModifiedSeconds = extras.getLong(EXTRA_DATE),
    )
    return QueueEntry(uid, song, extras.getBoolean(EXTRA_EPHEMERAL))
}

private const val EXTRA_SONG_ID = "song_id"
private const val EXTRA_ALBUM_ID = "album_id"
private const val EXTRA_DURATION = "duration"
private const val EXTRA_DATE = "date"
private const val EXTRA_TITLE = "title"
private const val EXTRA_ARTIST = "artist"
private const val EXTRA_ALBUM = "album"
private const val EXTRA_PATH = "path"
private const val EXTRA_EPHEMERAL = "ephemeral"

/**
 * Puente entre la UI y [PlaybackService].
 *
 * Mantiene [PlayQueue] como fuente de verdad de la cola y refleja cada cambio en el
 * player con operaciones puntuales (add/remove/move), en vez de reemplazar la lista
 * entera: asi la reproduccion no se corta al encolar algo.
 */
class PlayerConnection(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var nextUid = 0L

    /**
     * Conectarse al servicio es asincronico. Si el usuario toca una cancion apenas abre
     * la app, la accion se guarda aca y se ejecuta al conectar, en vez de perderse.
     */
    private var accionPendiente: (() -> Unit)? = null

    /**
     * Orden original del contexto, para poder volver atras al desactivar el aleatorio.
     */
    private var contextoOriginal: List<QueueEntry> = emptyList()
    private var mezclado = false

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val _queue = MutableStateFlow(PlayQueue())
    val queue: StateFlow<PlayQueue> = _queue.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer(player)
            // Si se cerro desde la notificacion, el player se queda sin items. Hay que
            // vaciar tambien nuestro modelo, si no el mini reproductor sigue en pantalla
            // mostrando una cancion que ya no existe.
            if (player.mediaItemCount == 0 && _queue.value.entries.isNotEmpty()) {
                _queue.value = PlayQueue()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = controller ?: return
            // La cancion cambio: movemos el modelo y descartamos lo efimero ya escuchado.
            val nueva = _queue.value.moveToIndex(player.currentMediaItemIndex)
            reconcile(nueva)
        }
    }

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                controller = future.get().apply {
                    addListener(listener)
                    syncFromPlayer(this)
                }
                restaurarColaDesdeElReproductor()
                _state.value = _state.value.copy(
                    isConnected = true,
                    audioSessionId = controller?.sessionExtras
                        ?.getInt(PlaybackService.KEY_AUDIO_SESSION_ID, 0) ?: 0,
                )
                startPositionUpdates()
                accionPendiente?.invoke()
                accionPendiente = null
            },
            MoreExecutors.directExecutor(),
        )
    }

    /**
     * Rearma la cola con lo que ya tiene el reproductor.
     *
     * Hace falta al volver a abrir la app mientras suena algo: el servicio sigue vivo,
     * pero el modelo de la UI arranca vacio y sin esto no se mostraba el mini
     * reproductor, como si no hubiera nada sonando.
     */
    private fun restaurarColaDesdeElReproductor() {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return

        val entradas = (0 until c.mediaItemCount).mapNotNull { c.getMediaItemAt(it).toQueueEntry() }
        if (entradas.size != c.mediaItemCount) return

        // Los uid nuevos tienen que seguir despues de los que ya existen.
        nextUid = (entradas.maxOfOrNull { it.uid } ?: -1L) + 1
        _queue.value = PlayQueue(entradas, c.currentMediaItemIndex)
    }

    fun release() {
        positionJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        _state.value = _state.value.copy(isConnected = false)
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                controller?.let { c ->
                    if (c.isPlaying) {
                        _state.value = _state.value.copy(
                            positionMs = c.currentPosition,
                            durationMs = c.duration.coerceAtLeast(0L),
                        )
                    }
                }
                delay(POSITION_POLL_MS)
            }
        }
    }

    private fun syncFromPlayer(player: Player) {
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L),
            // El aleatorio es nuestro, no el de ExoPlayer (ver toggleShuffle).
            shuffleEnabled = mezclado,
            repeatMode = player.repeatMode,
        )
    }

    // --- comandos ---

    /**
     * Arranca una lista nueva (una carpeta, todas las canciones) desde [startIndex].
     * Con [shuffle] la lista se mezcla de verdad, dejando primero la elegida.
     */
    fun playContext(songs: List<Song>, startIndex: Int, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        val c = controller ?: run {
            accionPendiente = { playContext(songs, startIndex, shuffle) }
            return
        }

        val base = songs.mapIndexed { i, s -> QueueEntry(nextUid + i, s) }
        nextUid += songs.size
        contextoOriginal = base
        mezclado = shuffle

        val elegida = base.getOrNull(startIndex.coerceIn(0, base.lastIndex)) ?: base.first()
        val orden = if (shuffle) {
            listOf(elegida) + base.filter { it.uid != elegida.uid }.shuffled()
        } else {
            base
        }
        val indice = orden.indexOfFirst { it.uid == elegida.uid }

        val nueva = PlayQueue(orden, indice)
        _queue.value = nueva
        c.setMediaItems(nueva.entries.map { it.song.toMediaItem(it.uid, it.ephemeral) }, indice, 0L)
        c.prepare()
        c.play()
    }

    /**
     * Activa o desactiva el aleatorio reordenando la lista de verdad.
     *
     * A proposito NO se usa `shuffleModeEnabled` de ExoPlayer: ese mantiene un orden de
     * reproduccion interno que la app no ve, y entonces la tira de canciones mostraba un
     * orden y el reproductor seguia otro. Cuando el orden interno se terminaba, la
     * reproduccion frenaba y "siguiente" no hacia nada aunque en pantalla hubiera temas
     * mas adelante. Mezclando la lista real, lo que ves es lo que suena.
     */
    fun toggleShuffle() {
        val c = controller ?: return
        val q = _queue.value
        val actual = q.current ?: return

        mezclado = !mezclado

        val contextoVivo = q.contextEntries
        val nuevoOrden = if (mezclado) {
            // La actual queda primera para no cortarla; el resto se mezcla.
            listOf(actual).filter { !it.ephemeral } +
                contextoVivo.filter { it.uid != actual.uid }.shuffled()
        } else {
            val vivos = contextoVivo.map { it.uid }.toSet()
            contextoOriginal.filter { it.uid in vivos }
        }

        val nueva = q.withContextOrder(nuevoOrden)
        aplicarOrdenSinCortar(c, nueva)
        _queue.value = nueva
    }

    /**
     * Reordena el timeline dejando intacta la cancion que esta sonando.
     *
     * Se evita `setMediaItems`, que reemplaza el timeline entero y obliga a volver a
     * preparar la actual: eso cortaba el audio un instante y, tocando el boton varias
     * veces seguidas, la reproduccion quedaba frenada. Quitar y agregar entradas
     * *alrededor* de la actual no la toca, asi que el cambio es imperceptible.
     */
    private fun aplicarOrdenSinCortar(c: MediaController, nueva: PlayQueue) {
        val indiceActual = c.currentMediaItemIndex
        val total = c.mediaItemCount
        if (indiceActual !in 0 until total) return

        // Queda solo la actual, sin tocarla.
        if (indiceActual + 1 < total) c.removeMediaItems(indiceActual + 1, total)
        if (indiceActual > 0) c.removeMediaItems(0, indiceActual)

        val destino = nueva.currentIndex.coerceAtLeast(0)
        val antes = nueva.entries.take(destino)
        val despues = nueva.entries.drop(destino + 1)

        if (antes.isNotEmpty()) {
            c.addMediaItems(0, antes.map { it.song.toMediaItem(it.uid, it.ephemeral) })
        }
        if (despues.isNotEmpty()) {
            c.addMediaItems(despues.map { it.song.toMediaItem(it.uid, it.ephemeral) })
        }
    }

    fun playNext(song: Song) = encolar { it.playNext(song, nextUid++) }

    fun addToQueue(song: Song) = encolar { it.addToQueue(song, nextUid++) }

    private fun encolar(transform: (PlayQueue) -> PlayQueue) {
        val c = controller ?: return
        val antes = _queue.value
        val despues = transform(antes)

        // Buscamos la entrada nueva y la insertamos en el player en la misma posicion.
        val idsAntes = antes.entries.map { it.uid }.toSet()
        val posicion = despues.entries.indexOfFirst { it.uid !in idsAntes }
        if (posicion < 0) return

        val entrada = despues.entries[posicion]
        _queue.value = despues
        c.addMediaItem(posicion, entrada.song.toMediaItem(entrada.uid, entrada.ephemeral))
        if (!c.isPlaying && c.mediaItemCount == 1) {
            c.prepare()
            c.play()
        }
    }

    /** Salta directo a una entrada de la cola (tocarla en la lista o en la tira). */
    fun playQueueIndex(index: Int) {
        val c = controller ?: return
        if (index !in _queue.value.entries.indices) return
        c.seekTo(index, 0L)
        c.play()
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index !in _queue.value.entries.indices) return
        _queue.value = _queue.value.removeAt(index)
        c.removeMediaItem(index)
    }

    fun moveInQueue(from: Int, to: Int) {
        val c = controller ?: return
        if (from == to) return
        _queue.value = _queue.value.move(from, to)
        c.moveMediaItem(from, to)
    }

    fun clearEphemeral() {
        val antes = _queue.value
        val despues = antes.clearEphemeral()
        reconcile(despues)
    }

    /**
     * Aplica al player las entradas que el modelo elimino, de atras hacia adelante
     * para que los indices no se corran mientras se borra.
     */
    private fun reconcile(nueva: PlayQueue) {
        val c = controller ?: run { _queue.value = nueva; return }
        val antes = _queue.value
        val quedan = nueva.entries.map { it.uid }.toSet()

        antes.entries.withIndex()
            .filter { it.value.uid !in quedan }
            .sortedByDescending { it.index }
            .forEach { (indice, _) ->
                if (indice < c.mediaItemCount) c.removeMediaItem(indice)
            }
        _queue.value = nueva
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else { c.prepare(); c.play() }
    }

    fun next() = controller?.seekToNextMediaItem()

    fun previous() {
        val c = controller ?: return
        // Igual que cualquier reproductor: si ya avanzo un poco, la primera pulsacion
        // reinicia la cancion en vez de saltar a la anterior.
        if (c.currentPosition > RESTART_THRESHOLD_MS) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        val destino = (c.currentPosition + deltaMs)
            .coerceIn(0L, c.duration.coerceAtLeast(0L))
        seekTo(destino)
    }

    /** Cicla apagado -> repetir todo -> repetir una, como el boton de la pantalla. */
    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private companion object {
        const val POSITION_POLL_MS = 500L
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}
