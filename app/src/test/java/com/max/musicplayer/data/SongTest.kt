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
    fun `los content URI se arman con el id correspondiente`() {
        val s = song(id = 42L, albumId = 7L)

        assertThat(s.contentUri).isEqualTo("content://media/external/audio/media/42")
        assertThat(s.albumArtUri).isEqualTo("content://media/external/audio/albumart/7")
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
