package com.max.musicplayer.data

import org.json.JSONObject

/** Una version publicada en GitHub Releases. */
data class Release(val version: String, val pageUrl: String)

/**
 * Decide si hay una version nueva publicada en GitHub.
 *
 * La app no se actualiza sola: se instala por fuera de Play, asi que lo unico que se
 * puede hacer es enterarse y abrir la pagina de la release para bajar el APK a mano.
 * Ver docs/publicar.md.
 *
 * Solo interpreta y compara; la consulta la hace
 * [com.max.musicplayer.net.GitHubReleases].
 */
object UpdateChecker {

    /** Cada cuanto se chequea solo al abrir la app. */
    const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    /**
     * Lee la respuesta de la API. Devuelve null si el JSON no tiene lo que se espera,
     * que es lo que pasa cuando el repo todavia no publico ninguna release.
     */
    fun parse(json: String): Release? = runCatching {
        val objeto = JSONObject(json)
        val tag = objeto.optString("tag_name").ifBlank { return null }
        val pagina = objeto.optString("html_url").ifBlank { return null }
        Release(version = normalize(tag), pageUrl = pagina)
    }.getOrNull()

    /**
     * Compara dos versiones `MAYOR.MENOR.PARCHE`.
     *
     * Se comparan numero por numero y no como texto: "0.10.0" es mas nueva que "0.9.0"
     * aunque alfabeticamente vaya antes. Lo que no se pueda leer como numero cuenta
     * como 0, asi que una version rara nunca dispara un aviso falso.
     */
    fun isNewer(installed: String, candidate: String): Boolean {
        val actual = numbersOf(installed)
        val nueva = numbersOf(candidate)
        for (i in 0 until maxOf(actual.size, nueva.size)) {
            val a = actual.getOrElse(i) { 0 }
            val b = nueva.getOrElse(i) { 0 }
            if (b != a) return b > a
        }
        return false
    }

    /** Saca la "v" del tag: los tags son `v0.2.0` y el versionName es `0.2.0`. */
    private fun normalize(tag: String): String = tag.trim().removePrefix("v")

    private fun numbersOf(version: String): List<Int> =
        normalize(version).split('.').map { parte ->
            parte.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
}
