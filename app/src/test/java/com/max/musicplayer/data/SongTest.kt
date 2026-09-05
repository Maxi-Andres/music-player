package com.max.musicplayer.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SongTest {

    @Test
    fun `folderPath y folderName salen de la ruta del archivo`() {
        val s = song(filePath = "/storage/emulated/0/Music/acdc/tnt.mp3")

        assertThat(s.folderPath).isEqualTo("/storage/emulated/0/Music/acdc")
        assertThat(s.folderName).isEqualTo("acdc")
    }

    @Test
    fun `archivo en la raiz no tiene carpeta`() {
        val s = song(filePath = "tema.mp3")

        assertThat(s.folderPath).isEmpty()
        assertThat(s.folderName).isEmpty()
    }

    @Test
    fun `nombres de carpeta con espacios y acentos se conservan tal cual`() {
        val s = song(filePath = "/storage/emulated/0/Music/Música Clásica/aria.flac")

        assertThat(s.folderName).isEqualTo("Música Clásica")
    }

    @Test
    fun `el content URI se arma con el id de la cancion`() {
        // La caratula NO se resuelve por album (ver ArtworkLoader): se lee del archivo.
        assertThat(song(id = 42L).contentUri)
            .isEqualTo("content://media/external/audio/media/42")
    }

    @Test
    fun `artista vacio se muestra como desconocido`() {
        assertThat(song(artist = "").displayArtist).isEqualTo(Song.UNKNOWN_ARTIST)
        assertThat(song(artist = "   ").displayArtist).isEqualTo(Song.UNKNOWN_ARTIST)
    }

    @Test
    fun `artista real se respeta`() {
        assertThat(song(artist = "AC/DC").displayArtist).isEqualTo("AC/DC")
    }
}
