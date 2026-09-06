package com.max.musicplayer.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Lectura de la respuesta de GitHub y comparacion de versiones.
 *
 * Corre con Robolectric porque [UpdateChecker.parse] usa `org.json`, que en un test
 * de JVM pelado es un stub vacio. La parte de red no se prueba aca: eso seria probar
 * HttpURLConnection, no nuestro codigo.
 */
@RunWith(RobolectricTestRunner::class)
class UpdateCheckerTest {

    private val respuestaReal = """
        {
          "tag_name": "v0.3.0",
          "name": "v0.3.0",
          "html_url": "https://github.com/Maxi-Andres/music-player/releases/tag/v0.3.0",
          "assets": [{ "name": "music-player-0.3.0.apk" }]
        }
    """.trimIndent()

    @Test
    fun `lee la version y el link de la respuesta de GitHub`() {
        val release = UpdateChecker.parse(respuestaReal)

        assertThat(release).isNotNull()
        assertThat(release!!.version).isEqualTo("0.3.0")
        assertThat(release.pageUrl)
            .isEqualTo("https://github.com/Maxi-Andres/music-player/releases/tag/v0.3.0")
    }

    @Test
    fun `le saca la v al tag para poder comparar contra el versionName`() {
        val release = UpdateChecker.parse("""{"tag_name":"v1.2.3","html_url":"http://x"}""")

        assertThat(release?.version).isEqualTo("1.2.3")
    }

    @Test
    fun `devuelve null si el repo todavia no publico ninguna release`() {
        // Es lo que contesta la API: un 404 con este cuerpo.
        assertThat(UpdateChecker.parse("""{"message":"Not Found"}""")).isNull()
    }

    @Test
    fun `devuelve null si la respuesta no es JSON`() {
        assertThat(UpdateChecker.parse("cualquier cosa")).isNull()
    }

    @Test
    fun `devuelve null si falta el link de la pagina`() {
        assertThat(UpdateChecker.parse("""{"tag_name":"v1.0.0"}""")).isNull()
    }

    @Test
    fun `detecta una version mas nueva`() {
        assertThat(UpdateChecker.isNewer("0.2.1", "0.2.2")).isTrue()
        assertThat(UpdateChecker.isNewer("0.2.1", "0.3.0")).isTrue()
        assertThat(UpdateChecker.isNewer("0.2.1", "1.0.0")).isTrue()
    }

    @Test
    fun `no avisa si es la misma version o una mas vieja`() {
        assertThat(UpdateChecker.isNewer("0.2.1", "0.2.1")).isFalse()
        assertThat(UpdateChecker.isNewer("0.2.1", "0.2.0")).isFalse()
        assertThat(UpdateChecker.isNewer("1.0.0", "0.9.9")).isFalse()
    }

    @Test
    fun `compara por numero y no alfabeticamente`() {
        // Como texto "0.10.0" va antes que "0.9.0", pero es mas nueva.
        assertThat(UpdateChecker.isNewer("0.9.0", "0.10.0")).isTrue()
        assertThat(UpdateChecker.isNewer("0.10.0", "0.9.0")).isFalse()
    }

    @Test
    fun `la v adelante no cambia la comparacion`() {
        assertThat(UpdateChecker.isNewer("0.2.1", "v0.2.2")).isTrue()
        assertThat(UpdateChecker.isNewer("v0.2.2", "v0.2.2")).isFalse()
    }

    @Test
    fun `una version mas corta cuenta los faltantes como cero`() {
        assertThat(UpdateChecker.isNewer("1.0", "1.0.1")).isTrue()
        assertThat(UpdateChecker.isNewer("1.0.0", "1.0")).isFalse()
    }

    @Test
    fun `una version ilegible no dispara un aviso falso`() {
        assertThat(UpdateChecker.isNewer("0.2.1", "")).isFalse()
        assertThat(UpdateChecker.isNewer("0.2.1", "no-es-una-version")).isFalse()
        // El versionName vacio (no se pudo leer el paquete) tampoco tiene que avisar
        // de cualquier cosa: 0.0.0 contra 0.0.0 no es mas nuevo.
        assertThat(UpdateChecker.isNewer("", "")).isFalse()
    }
}
