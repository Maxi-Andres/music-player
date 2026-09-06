package com.max.musicplayer.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.max.musicplayer.MainActivity
import com.max.musicplayer.R

/**
 * Servicio de reproduccion.
 *
 * Al heredar de [MediaSessionService], Media3 aporta gratis:
 *  - notificacion con controles y controles en la pantalla de bloqueo
 *  - botones del auricular (play/pausa, doble tap = siguiente)
 *  - audio focus: baja el volumen o pausa ante una llamada u otra app
 *  - pausa al desconectar los auriculares (setHandleAudioBecomingNoisy)
 *
 * Por eso no hay codigo de notificaciones en este proyecto: seria reimplementar
 * a mano algo que la libreria ya resuelve.
 */
// Media3 marca como @UnstableApi buena parte de lo que usa este servicio (los slots de
// los botones, el BitmapLoader, la configuracion de la sesion). Se declara el opt-in
// una vez para toda la clase en vez de anotar cada linea.
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /**
     * Anterior y siguiente, declarados por nosotros y clavados a su lugar.
     *
     * Media3 los pone solo, pero **los saca cuando no hay adonde ir**: con una sola
     * cancion en la carpeta desaparecia el de siguiente, el hueco lo tapaba un boton
     * propio y toda la fila cambiaba de orden. Declarandolos con su `playerCommand` y su
     * slot fijo siguen siempre en el mismo lugar, y Media3 los muestra apagados cuando no
     * se pueden usar en vez de esconderlos.
     */
    private val botonAnterior: CommandButton by lazy {
        CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setDisplayName(getString(R.string.cd_previous))
            .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
            .setSlots(CommandButton.SLOT_BACK)
            .build()
    }

    private val botonSiguiente: CommandButton by lazy {
        CommandButton.Builder(CommandButton.ICON_NEXT)
            .setDisplayName(getString(R.string.cd_next))
            .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
            .setSlots(CommandButton.SLOT_FORWARD)
            .build()
    }

    /**
     * Botones propios de la notificacion y la pantalla de bloqueo: repetir y cerrar.
     * Media3 no los pone solo, hay que declararlos como comandos de la sesion.
     */
    private fun botonRepetir(repeatMode: Int): CommandButton {
        val icono = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
            Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
            else -> CommandButton.ICON_REPEAT_OFF
        }
        return CommandButton.Builder(icono)
            .setDisplayName(getString(R.string.cd_repeat))
            .setSessionCommand(SessionCommand(ACTION_REPEAT, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
    }

    private val botonCerrar: CommandButton by lazy {
        CommandButton.Builder()
            .setDisplayName(getString(R.string.cd_close))
            .setIconResId(R.drawable.ic_close)
            .setSessionCommand(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
    }

    /**
     * El orden de la fila. Anterior y siguiente van clavados a sus slots; repetir y la X
     * ocupan lo que queda a los costados.
     */
    private fun preferencias(repeatMode: Int) = ImmutableList.of(
        botonAnterior,
        botonSiguiente,
        botonRepetir(repeatMode),
        botonCerrar,
    )

    override fun onCreate() {
        super.onCreate()

        // Se genera aca y se fija en el player para poder pasarselo al ecualizador:
        // si se dejara que ExoPlayer lo asigne solo, no habria id hasta que suene algo.
        val audioSessionId = Util.generateAudioSessionIdV21(this)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // Pausa sola cuando se desenchufan los auriculares.
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply { setAudioSessionId(audioSessionId) }

        // Al cambiar el modo de repeticion hay que refrescar el icono del boton.
        player.addListener(object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) {
                mediaSession?.setMediaButtonPreferences(preferencias(repeatMode))
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())
            .setBitmapLoader(CacheBitmapLoader(AudioArtworkBitmapLoader(this)))
            // La UI lo lee desde los extras de la sesion para armar el ecualizador.
            .setExtras(Bundle().apply { putInt(KEY_AUDIO_SESSION_ID, audioSessionId) })
            .setMediaButtonPreferences(preferencias(player.repeatMode))
            .setCallback(Callback())
            .build()
    }

    private inner class Callback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val comandos = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_REPEAT, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(comandos)
                .setMediaButtonPreferences(preferencias(session.player.repeatMode))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_CLOSE -> {
                    // Cerrar = frenar y sacar la notificacion, no solo pausar.
                    session.player.stop()
                    session.player.clearMediaItems()
                    stopSelf()
                }

                ACTION_REPEAT -> {
                    session.player.repeatMode = when (session.player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                }

                else -> return Futures.immediateFuture(
                    SessionResult(SessionError.ERROR_NOT_SUPPORTED),
                )
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /** Tocar la notificacion abre la app en vez de arrancar una Activity nueva. */
    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val KEY_AUDIO_SESSION_ID = "audio_session_id"
        private const val ACTION_CLOSE = "com.max.musicplayer.CLOSE"
        private const val ACTION_REPEAT = "com.max.musicplayer.REPEAT"
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /**
     * Si el usuario cierra la app desde recientes y no hay nada sonando, se corta el
     * servicio. Si esta sonando, sigue: es el comportamiento esperado de un reproductor.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
