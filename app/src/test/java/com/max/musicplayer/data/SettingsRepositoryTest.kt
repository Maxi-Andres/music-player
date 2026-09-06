package com.max.musicplayer.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Logica de las preferencias de apariencia: valores por defecto, que elegir un color
 * de fondo descarte la imagen, y que restablecer vuelva todo a cero.
 *
 * Corre sobre un DataStore en memoria. Lo que se prueba es **nuestro mapeo**, no la
 * persistencia de la libreria; ademas el DataStore de archivo no se puede reescribir
 * dos veces seguidas en Windows (falla el rename del archivo temporal).
 */
class SettingsRepositoryTest {

    /** DataStore minimo en memoria, suficiente para `edit {}` y `data`. */
    private class DataStoreEnMemoria : DataStore<Preferences> {
        private val flujo = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = flujo
        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences,
        ): Preferences = transform(flujo.value).also { flujo.value = it }
    }

    private fun repositorio() = SettingsRepository(DataStoreEnMemoria())

    @Test
    fun `sin nada guardado devuelve los valores por defecto`() = runTest {
        val ajustes = repositorio().settings.first()

        assertThat(ajustes).isEqualTo(AppSettings())
        assertThat(ajustes.usesBackgroundImage).isFalse()
    }

    @Test
    fun `guarda y devuelve el color de acento`() = runTest {
        val repo = repositorio()

        repo.setAccentColor(0xFF112233.toInt())

        assertThat(repo.settings.first().accentColor).isEqualTo(0xFF112233.toInt())
    }

    @Test
    fun `guarda cada interruptor de efectos por separado`() = runTest {
        val repo = repositorio()

        repo.setGlassEffect(false)
        repo.setRingOnNowPlaying(true)
        repo.setRingFromArtwork(true)

        val ajustes = repo.settings.first()
        assertThat(ajustes.glassEffect).isFalse()
        assertThat(ajustes.ringOnNowPlaying).isTrue()
        assertThat(ajustes.ringFromArtwork).isTrue()
        // No se pisan entre ellos ni tocan lo que ya estaba.
        assertThat(ajustes.tintFromArtwork).isTrue()
    }

    @Test
    fun `restablecer devuelve los efectos a su valor por defecto`() = runTest {
        val repo = repositorio()
        repo.setGlassEffect(false)
        repo.setRingOnNowPlaying(true)

        repo.resetAll()

        assertThat(repo.settings.first()).isEqualTo(AppSettings())
    }

    @Test
    fun `guarda el color de las carpetas sin tocar el resto`() = runTest {
        val repo = repositorio()

        repo.setFolderColor(0xFF445566.toInt())

        val ajustes = repo.settings.first()
        assertThat(ajustes.folderColor).isEqualTo(0xFF445566.toInt())
        assertThat(ajustes.accentColor).isEqualTo(AppSettings.DEFAULT_ACCENT)
    }

    @Test
    fun `elegir un color de fondo descarta la imagen`() = runTest {
        val repo = repositorio()
        repo.setBackgroundImage("content://foto/1")
        assertThat(repo.settings.first().usesBackgroundImage).isTrue()

        repo.setBackgroundColor(0xFF000000.toInt())

        val ajustes = repo.settings.first()
        assertThat(ajustes.usesBackgroundImage).isFalse()
        assertThat(ajustes.backgroundColor).isEqualTo(0xFF000000.toInt())
    }

    @Test
    fun `quitar la imagen vuelve al color de fondo`() = runTest {
        val repo = repositorio()
        repo.setBackgroundImage("content://foto/1")

        repo.setBackgroundImage(null)

        assertThat(repo.settings.first().usesBackgroundImage).isFalse()
    }

    @Test
    fun `guarda cuanto se oscurece la imagen`() = runTest {
        val repo = repositorio()

        repo.setBackgroundDim(0.25f)

        assertThat(repo.settings.first().backgroundDim).isWithin(0.001f).of(0.25f)
    }

    @Test
    fun `restablecer deja todo como al principio`() = runTest {
        val repo = repositorio()
        repo.setAccentColor(0xFF112233.toInt())
        repo.setFolderColor(0xFF445566.toInt())
        repo.setBackgroundImage("content://foto/1")

        repo.resetAll()

        assertThat(repo.settings.first()).isEqualTo(AppSettings())
    }
}
