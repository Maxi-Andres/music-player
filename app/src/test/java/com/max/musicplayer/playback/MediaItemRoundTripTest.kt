package com.max.musicplayer.playback

import com.google.common.truth.Truth.assertThat
import com.max.musicplayer.data.Song
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Ida y vuelta entre [Song] y el item del reproductor.
 *
 * Existe por un bug concreto: al cerrar la app y volver a abrirla con la musica
 * sonando, la cola se rearma leyendo los items del reproductor. Si algun dato no
 * viaja en el item, el mini reproductor aparece vacio o directamente no aparece.
 */
@RunWith(RobolectricTestRunner::class)
class MediaItemRoundTripTest {

    private val cancion = Song(
        id = 42L,
        title = "T.N.T.",
        artist = "AC/DC",
        album = "High Voltage",
        albumId = 7L,
        durationMs = 210_000L,
        filePath = "/storage/emulated/0/Music/acdc/tnt.mp3",
        dateModifiedSeconds = 1_700_000_000L,
    )

    @Test
    fun `la cancion sobrevive el viaje de ida y vuelta`() {
        val entrada = cancion.toMediaItem(uid = 5L).toQueueEntry()

        assertThat(entrada).isNotNull()
        assertThat(entrada!!.song).isEqualTo(cancion)
    }

    @Test
    fun `el uid se conserva para poder ubicar la entrada exacta`() {
        val entrada = cancion.toMediaItem(uid = 99L).toQueueEntry()

        assertThat(entrada?.uid).isEqualTo(99L)
    }

    @Test
    fun `se recuerda si la cancion fue encolada a mano`() {
        val efimera = cancion.toMediaItem(uid = 1L, ephemeral = true).toQueueEntry()
        val delContexto = cancion.toMediaItem(uid = 2L, ephemeral = false).toQueueEntry()

        assertThat(efimera?.ephemeral).isTrue()
        assertThat(delContexto?.ephemeral).isFalse()
    }

    @Test
    fun `la carpeta se puede deducir despues de rearmar la cola`() {
        val entrada = cancion.toMediaItem(uid = 1L).toQueueEntry()

        // Sin la ruta no habria carpeta, y el subtitulo del mini reproductor
        // quedaria incompleto.
        assertThat(entrada?.song?.folderName).isEqualTo("acdc")
    }

    @Test
    fun `dos entradas de la misma cancion no se confunden`() {
        val primera = cancion.toMediaItem(uid = 1L).toQueueEntry()
        val segunda = cancion.toMediaItem(uid = 2L).toQueueEntry()

        assertThat(primera?.uid).isNotEqualTo(segunda?.uid)
        assertThat(primera?.song).isEqualTo(segunda?.song)
    }
}
