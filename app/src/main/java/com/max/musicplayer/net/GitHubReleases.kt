package com.max.musicplayer.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lo unico que sale a internet en toda la app: preguntarle a GitHub cual es la ultima
 * version publicada.
 *
 * Vive fuera de `data` a proposito. Ahi solo va logica que se pueda verificar con un
 * test; esto es un adaptador de red y probarlo seria probar HttpURLConnection. Quien
 * interpreta la respuesta es [com.max.musicplayer.data.UpdateChecker], que si esta
 * cubierto por tests.
 */
object GitHubReleases {

    /** La API publica no pide credenciales; el limite sin token alcanza de sobra. */
    const val LATEST_RELEASE_API =
        "https://api.github.com/repos/Maxi-Andres/music-player/releases/latest"

    private const val TIMEOUT_MS = 10_000

    /** Devuelve el JSON crudo, o null si no hay red o GitHub no contesto con 200. */
    suspend fun latestJson(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conexion = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                // Lo pide la documentacion de la API de GitHub.
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            try {
                if (conexion.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                conexion.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conexion.disconnect()
            }
        }.getOrNull()
    }
}
