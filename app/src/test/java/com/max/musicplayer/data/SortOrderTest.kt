package com.max.musicplayer.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * La barra de indice A-Z solo se muestra cuando la lista quedo ordenada por nombre.
 * Estos tests fijan ese contrato: si alguien agrega un criterio de orden nuevo y se
 * olvida de clasificarlo, la barra volveria a saltar a posiciones arbitrarias.
 */
class SortOrderTest {

    @Test
    fun `solo los ordenes por titulo cuentan como alfabeticos`() {
        val alfabeticos = SongSort.entries.filter { it.isAlphabetical }

        assertThat(alfabeticos).containsExactly(SongSort.TITLE_ASC, SongSort.TITLE_DESC)
    }

    @Test
    fun `ordenar por fecha o duracion no habilita el indice`() {
        assertThat(SongSort.DATE_ADDED_DESC.isAlphabetical).isFalse()
        assertThat(SongSort.DATE_ADDED_ASC.isAlphabetical).isFalse()
        assertThat(SongSort.DURATION_ASC.isAlphabetical).isFalse()
        assertThat(SongSort.DURATION_DESC.isAlphabetical).isFalse()
        // Por artista la lista queda ordenada, pero no por la letra que se ve primero.
        assertThat(SongSort.ARTIST_ASC.isAlphabetical).isFalse()
    }

    @Test
    fun `solo los ordenes por nombre de carpeta cuentan como alfabeticos`() {
        val alfabeticos = FolderSort.entries.filter { it.isAlphabetical }

        assertThat(alfabeticos).containsExactly(FolderSort.NAME_ASC, FolderSort.NAME_DESC)
    }

    @Test
    fun `ordenar carpetas por cantidad no habilita el indice`() {
        assertThat(FolderSort.SONG_COUNT_DESC.isAlphabetical).isFalse()
        assertThat(FolderSort.SONG_COUNT_ASC.isAlphabetical).isFalse()
    }

    @Test
    fun `con orden alfabetico el indice apunta a la carpeta correcta`() {
        val nombres = listOf("acdc", "madonna", "man ray", "metallica", "zeta")
        val carpetas = nombres.map { MusicFolder("/m/$it", it, 1) }

        val ordenadas = MusicLibrary.sortFolders(carpetas, FolderSort.NAME_ASC).map { it.name }
        val posicion = MusicLibrary.firstIndexForLetter(ordenadas, "M")

        assertThat(ordenadas[posicion]).isEqualTo("madonna")
    }
}
