package com.max.musicplayer.data

import android.database.MatrixCursor
import android.provider.MediaStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifica la lectura del cursor de MediaStore sin necesidad de un dispositivo:
 * se le pasa un [MatrixCursor] armado a mano con las mismas columnas que devuelve
 * Android de verdad.
 */
@RunWith(RobolectricTestRunner::class)
class MediaStoreScannerTest {

    private fun cursorCon(vararg filas: Array<Any?>): MatrixCursor =
        MatrixCursor(MediaStoreScanner.PROJECTION).apply {
            filas.forEach { addRow(it) }
        }

    private fun fila(
        id: Long = 1L,
        title: String? = "T.N.T.",
        artist: String? = "AC/DC",
        album: String? = "High Voltage",
        albumId: Long = 10L,
        duration: Long = 210_000L,
        data: String? = "/storage/emulated/0/Music/acdc/tnt.mp3",
        dateModified: Long = 1_700_000_000L,
    ): Array<Any?> = arrayOf(id, title, artist, album, albumId, duration, data, dateModified)

    @Test
    fun `mapea una fila completa a una cancion`() {
        val songs = MediaStoreScanner.readSongs(cursorCon(fila()))

        assertThat(songs).hasSize(1)
        with(songs.single()) {
            assertThat(id).isEqualTo(1L)
            assertThat(title).isEqualTo("T.N.T.")
            assertThat(artist).isEqualTo("AC/DC")
            assertThat(album).isEqualTo("High Voltage")
            assertThat(albumId).isEqualTo(10L)
            assertThat(durationMs).isEqualTo(210_000L)
            assertThat(folderName).isEqualTo("acdc")
        }
    }

    @Test
    fun `un cursor vacio devuelve lista vacia`() {
        assertThat(MediaStoreScanner.readSongs(cursorCon())).isEmpty()
    }

    @Test
    fun `si falta el titulo usa el nombre del archivo sin extension`() {
        val songs = MediaStoreScanner.readSongs(
            cursorCon(fila(title = null, data = "/storage/emulated/0/Music/x/Highway to Hell.mp3")),
        )

        assertThat(songs.single().title).isEqualTo("Highway to Hell")
    }

    @Test
    fun `si falta el artista queda como desconocido`() {
        val songs = MediaStoreScanner.readSongs(cursorCon(fila(artist = null)))

        assertThat(songs.single().displayArtist).isEqualTo(Song.UNKNOWN_ARTIST)
    }

    @Test
    fun `si falta el album queda vacio en vez de romper`() {
        val songs = MediaStoreScanner.readSongs(cursorCon(fila(album = null)))

        assertThat(songs.single().album).isEmpty()
    }

    @Test
    fun `una fila sin ruta se descarta porque no se puede ubicar en una carpeta`() {
        val songs = MediaStoreScanner.readSongs(
            cursorCon(fila(id = 1, data = null), fila(id = 2)),
        )

        assertThat(songs.map { it.id }).containsExactly(2L)
    }

    @Test
    fun `lee varias filas conservando el orden del cursor`() {
        val songs = MediaStoreScanner.readSongs(
            cursorCon(
                fila(id = 1, title = "Uno"),
                fila(id = 2, title = "Dos"),
                fila(id = 3, title = "Tres"),
            ),
        )

        assertThat(songs.map { it.title }).containsExactly("Uno", "Dos", "Tres").inOrder()
    }

    @Test
    fun `la proyeccion pide exactamente las columnas que usa el mapeo`() {
        assertThat(MediaStoreScanner.PROJECTION.toList()).containsExactly(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )
    }

    @Test
    fun `el filtro excluye tonos, alarmas y notificaciones`() {
        assertThat(MediaStoreScanner.SELECTION).contains(MediaStore.Audio.Media.IS_RINGTONE)
        assertThat(MediaStoreScanner.SELECTION).contains(MediaStore.Audio.Media.IS_ALARM)
        assertThat(MediaStoreScanner.SELECTION).contains(MediaStore.Audio.Media.IS_NOTIFICATION)
    }

    @Test
    fun `el filtro no excluye por IS_MUSIC para no perder audios de WhatsApp`() {
        // Los audios de WhatsApp y las grabaciones suelen venir con IS_MUSIC = 0,
        // y en la app de referencia si aparecen en la biblioteca.
        assertThat(MediaStoreScanner.SELECTION).doesNotContain(MediaStore.Audio.Media.IS_MUSIC)
    }
}
