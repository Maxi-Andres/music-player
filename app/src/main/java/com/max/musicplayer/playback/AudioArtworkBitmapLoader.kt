package com.max.musicplayer.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.max.musicplayer.data.ArtworkLoader
import java.util.concurrent.Executors

/**
 * Resuelve la caratula que muestran la notificacion y la pantalla de bloqueo.
 *
 * Media3 usa por defecto un cargador pensado para URIs de imagen. Como aca el
 * `artworkUri` de cada item apunta al **archivo de audio**, hace falta este cargador
 * para sacarle la tapa embebida. Sin esto la notificacion volveria a la caratula por
 * album, que en archivos sin tags muestra la tapa de otra cancion.
 */
@OptIn(UnstableApi::class)
class AudioArtworkBitmapLoader(private val context: Context) : BitmapLoader {

    private val executor: ListeningExecutorService =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        executor.submit<Bitmap> {
            ArtworkLoader.decodeDownsampled(data, NOTIFICATION_ART_PX)
                ?: error("No se pudo decodificar la caratula embebida")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        executor.submit<Bitmap> {
            ArtworkLoader.load(context, uri, NOTIFICATION_ART_PX)
                ?: error("La cancion no tiene caratula: $uri")
        }

    private companion object {
        /** La notificacion expandida y el lockscreen no necesitan mas que esto. */
        const val NOTIFICATION_ART_PX = 512
    }
}
