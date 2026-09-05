package com.max.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.max.musicplayer.data.PlayQueue
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
)

fun Song.toMediaItem(uid: Long): MediaItem = MediaItem.Builder()
    // El uid va en el mediaId para poder reencontrar la entrada exacta en la cola
    // aunque la misma cancion este encolada mas de una vez.
    .setMediaId("$uid|$id")
    .setUri(contentUri.toUri())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(displayArtist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri.toUri())
            .build(),
    )
    .build()

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

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val _queue = MutableStateFlow(PlayQueue())
    val queue: StateFlow<PlayQueue> = _queue.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer(player)
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
                _state.value = _state.value.copy(isConnected = true)
                startPositionUpdates()
                accionPendiente?.invoke()
                accionPendiente = null
            },
            MoreExecutors.directExecutor(),
        )
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
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
    }

    // --- comandos ---

    /** Arranca una lista nueva (una carpeta, todas las canciones) desde [startIndex]. */
    fun playContext(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val c = controller ?: run {
            accionPendiente = { playContext(songs, startIndex) }
            return
        }

        val nueva = PlayQueue.fromContext(songs, startIndex, uidFrom = nextUid)
        nextUid += songs.size

        _queue.value = nueva
        c.setMediaItems(
            nueva.entries.map { it.song.toMediaItem(it.uid) },
            nueva.currentIndex.coerceAtLeast(0),
            0L,
        )
        c.prepare()
        c.play()
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
        c.addMediaItem(posicion, entrada.song.toMediaItem(entrada.uid))
        if (!c.isPlaying && c.mediaItemCount == 1) {
            c.prepare()
            c.play()
        }
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

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
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
