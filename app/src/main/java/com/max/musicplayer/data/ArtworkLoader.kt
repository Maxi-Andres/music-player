package com.max.musicplayer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size

/**
 * Lee la caratula embebida en un archivo de audio.
 *
 * Vive aparte porque la necesitan dos consumidores distintos: Coil, para las listas de
 * la app, y Media3, para la notificacion y la pantalla de bloqueo. Si cada uno la
 * resolviera por su cuenta, terminarian mostrando tapas distintas para la misma cancion.
 *
 * Importante: NO se usa `content://media/external/audio/albumart/<albumId>`, que es la
 * caratula **del album**. Los archivos sin tag de album caen todos en el mismo album
 * "desconocido" y ahi terminan compartiendo la tapa de otra cancion cualquiera.
 */
object ArtworkLoader {

    const val DEFAULT_SIZE_PX = 512

    fun load(context: Context, uri: Uri, targetPx: Int = DEFAULT_SIZE_PX): Bitmap? =
        loadThumbnail(context, uri, targetPx) ?: loadEmbedded(context, uri, targetPx)

    /** Camino rapido de Android 10+: el sistema ya tiene la miniatura generada. */
    private fun loadThumbnail(context: Context, uri: Uri, targetPx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            context.contentResolver.loadThumbnail(uri, Size(targetPx, targetPx), null)
        }.getOrNull()
    }

    /**
     * Respaldo para Android 8 y 9, y para archivos sin miniatura generada.
     *
     * Se libera a mano y no con `use`: esa extension pide AutoCloseable, que
     * MediaMetadataRetriever recien implementa en API 29. Con minSdk 26 el `use`
     * compilaba pero explotaba en Android 8 y 9, que son justo los unicos que dependen
     * de este camino.
     */
    private fun loadEmbedded(context: Context, uri: Uri, targetPx: Int): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            decodeDownsampled(bytes, targetPx)
        } finally {
            retriever.release()
        }
    }.getOrNull()

    /**
     * Decodifica a la resolucion en la que se va a mostrar. Sin esto se cargan tapas de
     * 1000x1000 para dibujarlas en 48dp, que es lo que hace tironear el scroll.
     */
    fun decodeDownsampled(bytes: ByteArray, targetPx: Int): Bitmap? {
        val medir = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, medir)

        var escala = 1
        while (medir.outWidth / (escala * 2) >= targetPx &&
            medir.outHeight / (escala * 2) >= targetPx
        ) {
            escala *= 2
        }

        val opciones = BitmapFactory.Options().apply { inSampleSize = escala }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
    }
}
