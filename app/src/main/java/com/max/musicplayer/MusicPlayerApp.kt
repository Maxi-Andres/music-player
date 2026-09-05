package com.max.musicplayer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.max.musicplayer.ui.AudioArtworkFetcher
import okio.Path.Companion.toOkioPath

/**
 * Punto de entrada del proceso.
 *
 * Ademas configura el cargador de imagenes: le registra el fetcher que saca la caratula
 * embebida de cada archivo de audio y le da caches en memoria y disco, para que scrollear
 * listas largas no vuelva a leer el mismo archivo una y otra vez.
 */
class MusicPlayerApp : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(AudioArtworkFetcher.Factory(this@MusicPlayerApp))
            }
            .memoryCache {
                MemoryCache.Builder()
                    // Las tapas son chicas; con un 20% de la memoria de la app sobra
                    // para mantener en RAM todo lo que se ve al scrollear.
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("artwork").toOkioPath())
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            // Sin crossfade: en una lista que se scrollea rapido, la animacion por fila
            // suma trabajo justo cuando menos sobra.
            .crossfade(false)
            .build()
}
