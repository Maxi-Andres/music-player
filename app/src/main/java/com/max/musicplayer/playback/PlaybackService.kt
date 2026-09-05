package com.max.musicplayer.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
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
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /**
     * Boton de cerrar de la notificacion y la pantalla de bloqueo.
     * Media3 no lo pone solo: hay que declararlo como comando propio de la sesion.
     */
    private val botonCerrar: CommandButton by lazy {
        CommandButton.Builder()
            .setDisplayName(getString(R.string.cd_close))
            .setIconResId(R.drawable.ic_close)
            .setSessionCommand(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
            // Sin ranura explicita los botones propios caen al overflow y no se ven.
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
    }

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

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())
            .setBitmapLoader(CacheBitmapLoader(AudioArtworkBitmapLoader(this)))
            // La UI lo lee desde los extras de la sesion para armar el ecualizador.
            .setExtras(Bundle().apply { putInt(KEY_AUDIO_SESSION_ID, audioSessionId) })
            .setMediaButtonPreferences(ImmutableList.of(botonCerrar))
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
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(comandos)
                .setMediaButtonPreferences(ImmutableList.of(botonCerrar))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != ACTION_CLOSE) {
                return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED),
                )
            }
            // Cerrar = frenar y sacar la notificacion, no solo pausar.
            session.player.stop()
            session.player.clearMediaItems()
            stopSelf()
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
