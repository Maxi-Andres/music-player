package com.max.musicplayer.ui

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pide la caratula de UNA cancion concreta.
 *
 * Existe como tipo propio (en vez de pasar una Uri) por dos motivos: evita chocar con
 * los fetchers que Coil trae de fabrica para `content://`, y su `toString()` sirve de
 * clave estable para la cache en memoria.
 */
data class AudioArtwork(val songId: Long)

/**
 * Extrae la caratula embebida en el propio archivo de audio.
 *
 * No se usa `content://media/external/audio/albumart/<albumId>` porque esa caratula es
 * **del album, no de la cancion**: los archivos sin tag de album caen todos en el mismo
 * album "desconocido" y terminan mostrando la tapa de otra cancion cualquiera.
 */
class AudioArtworkFetcher(
    private val context: Context,
    private val data: AudioArtwork,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            data.songId,
        )
        val lado = targetPx()
        val bitmap = loadThumbnail(uri, lado) ?: loadEmbedded(uri, lado) ?: return@withContext null

        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    /** Camino rapido de Android 10+: el sistema ya tiene la miniatura cacheada. */
    private fun loadThumbnail(uri: Uri, lado: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            context.contentResolver.loadThumbnail(uri, Size(lado, lado), null)
        }.getOrNull()
    }

    /** Respaldo para Android 8 y 9, y para archivos sin miniatura generada. */
    private fun loadEmbedded(uri: Uri, lado: Int): Bitmap? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            decodeDownsampled(bytes, lado)
        }
    }.getOrNull()

    /**
     * Decodifica a la resolucion que se va a mostrar. Sin esto se cargan tapas de
     * 1000x1000 para dibujarlas en 48dp, que es lo que hace tironear el scroll.
     */
    private fun decodeDownsampled(bytes: ByteArray, lado: Int): Bitmap? {
        val medir = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, medir)

        var escala = 1
        while (medir.outWidth / (escala * 2) >= lado && medir.outHeight / (escala * 2) >= lado) {
            escala *= 2
        }

        val opciones = BitmapFactory.Options().apply { inSampleSize = escala }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
    }

    private fun targetPx(): Int {
        val ancho = (options.size.width as? Dimension.Pixels)?.px ?: 0
        val alto = (options.size.height as? Dimension.Pixels)?.px ?: 0
        return maxOf(ancho, alto).takeIf { it > 0 } ?: DEFAULT_PX
    }

    class Factory(private val context: Context) : Fetcher.Factory<AudioArtwork> {
        override fun create(
            data: AudioArtwork,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = AudioArtworkFetcher(context, data, options)
    }

    private companion object {
        const val DEFAULT_PX = 512
    }
}
