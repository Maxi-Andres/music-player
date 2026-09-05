package com.max.musicplayer.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.max.musicplayer.MainActivity

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

    override fun onCreate() {
        super.onCreate()

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

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())
            .build()
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
