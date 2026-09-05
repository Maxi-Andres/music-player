package com.max.musicplayer.ui

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Dimension
import com.max.musicplayer.data.ArtworkLoader
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

/** Adapta [ArtworkLoader] a Coil, para las listas de la app. */
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
        val bitmap = ArtworkLoader.load(context, uri, targetPx()) ?: return@withContext null

        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    private fun targetPx(): Int {
        val ancho = (options.size.width as? Dimension.Pixels)?.px ?: 0
        val alto = (options.size.height as? Dimension.Pixels)?.px ?: 0
        return maxOf(ancho, alto).takeIf { it > 0 } ?: ArtworkLoader.DEFAULT_SIZE_PX
    }

    class Factory(private val context: Context) : Fetcher.Factory<AudioArtwork> {
        override fun create(
            data: AudioArtwork,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = AudioArtworkFetcher(context, data, options)
    }
}
