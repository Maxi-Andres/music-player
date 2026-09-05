package com.max.musicplayer.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DirectoryTreeTest {

    private val raiz = "/storage/emulated/0"

    private val biblioteca = listOf(
        song(id = 1, filePath = "$raiz/Music/acdc/tnt.mp3"),
        song(id = 2, filePath = "$raiz/Music/acdc/bib.mp3"),
        song(id = 3, filePath = "$raiz/Music/rock/zeppelin/kashmir.mp3"),
        song(id = 4, filePath = "$raiz/Music/suelta.mp3"),
        song(id = 5, filePath = "$raiz/Download/podcast.mp3"),
    )

    // --- raiz comun ---

    @Test
    fun `la raiz comun es el ancestro compartido mas profundo`() {
        assertThat(DirectoryTree.commonRoot(biblioteca)).isEqualTo(raiz)
    }

    @Test
    fun `con una sola carpeta la raiz comun es esa carpeta`() {
        val una = listOf(song(filePath = "$raiz/Music/acdc/tnt.mp3"))

        assertThat(DirectoryTree.commonRoot(una)).isEqualTo("$raiz/Music/acdc")
    }

    @Test
    fun `sin canciones la raiz comun es vacia`() {
        assertThat(DirectoryTree.commonRoot(emptyList())).isEmpty()
    }

    @Test
    fun `carpetas hermanas comparten el padre como raiz`() {
        val songs = listOf(
            song(id = 1, filePath = "$raiz/Music/a/x.mp3"),
            song(id = 2, filePath = "$raiz/Music/b/y.mp3"),
        )

        assertThat(DirectoryTree.commonRoot(songs)).isEqualTo("$raiz/Music")
    }

    // --- navegar ---

    @Test
    fun `childrenOf devuelve solo los subdirectorios inmediatos`() {
        val hijos = DirectoryTree.childrenOf(biblioteca, raiz)

        assertThat(hijos.map { it.name }).containsExactly("Download", "Music").inOrder()
    }

    @Test
    fun `no baja mas de un nivel por vez`() {
        val hijos = DirectoryTree.childrenOf(biblioteca, "$raiz/Music")

        // "zeppelin" cuelga de "rock", asi que no debe aparecer en este nivel.
        assertThat(hijos.map { it.name }).containsExactly("acdc", "rock").inOrder()
    }

    @Test
    fun `el conteo de canciones incluye las subcarpetas`() {
        val music = DirectoryTree.childrenOf(biblioteca, raiz).first { it.name == "Music" }

        // 2 en acdc + 1 en rock/zeppelin + 1 suelta en Music
        assertThat(music.songCount).isEqualTo(4)
    }

    @Test
    fun `cuenta los subdirectorios inmediatos`() {
        val music = DirectoryTree.childrenOf(biblioteca, raiz).first { it.name == "Music" }
        val acdc = DirectoryTree.childrenOf(biblioteca, "$raiz/Music").first { it.name == "acdc" }

        assertThat(music.subdirectoryCount).isEqualTo(2) // acdc y rock
        assertThat(acdc.subdirectoryCount).isEqualTo(0)
    }

    @Test
    fun `una hoja no tiene hijos`() {
        assertThat(DirectoryTree.childrenOf(biblioteca, "$raiz/Music/acdc")).isEmpty()
    }

    @Test
    fun `un directorio inexistente no rompe`() {
        assertThat(DirectoryTree.childrenOf(biblioteca, "$raiz/NoExiste")).isEmpty()
    }

    @Test
    fun `los subdirectorios vienen ordenados por nombre sin acentos`() {
        val songs = listOf(
            song(id = 1, filePath = "$raiz/Zeta/a.mp3"),
            song(id = 2, filePath = "$raiz/Ángeles/b.mp3"),
            song(id = 3, filePath = "$raiz/beta/c.mp3"),
        )

        val nombres = DirectoryTree.childrenOf(songs, raiz).map { it.name }

        assertThat(nombres).containsExactly("Ángeles", "beta", "Zeta").inOrder()
    }

    // --- canciones por directorio ---

    @Test
    fun `songsDirectlyIn ignora las de las subcarpetas`() {
        val directas = DirectoryTree.songsDirectlyIn(biblioteca, "$raiz/Music")

        assertThat(directas.map { it.id }).containsExactly(4L)
    }

    @Test
    fun `songsUnder incluye todo el subarbol`() {
        val todas = DirectoryTree.songsUnder(biblioteca, "$raiz/Music")

        assertThat(todas.map { it.id }).containsExactly(1L, 2L, 3L, 4L)
    }

    @Test
    fun `songsUnder no toma carpetas que solo comparten prefijo de texto`() {
        val songs = listOf(
            song(id = 1, filePath = "$raiz/Music/a.mp3"),
            song(id = 2, filePath = "$raiz/MusicVieja/b.mp3"),
        )

        // "MusicVieja" empieza con "Music" pero NO es una subcarpeta suya.
        val bajoMusic = DirectoryTree.songsUnder(songs, "$raiz/Music")

        assertThat(bajoMusic.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `childrenOf tampoco confunde carpetas con prefijo comun`() {
        val songs = listOf(
            song(id = 1, filePath = "$raiz/Music/a.mp3"),
            song(id = 2, filePath = "$raiz/MusicVieja/b.mp3"),
        )

        assertThat(DirectoryTree.childrenOf(songs, "$raiz/Music")).isEmpty()
    }

    // --- subir y migas ---

    @Test
    fun `parentOf sube un nivel`() {
        assertThat(DirectoryTree.parentOf("$raiz/Music/acdc")).isEqualTo("$raiz/Music")
    }

    @Test
    fun `parentOf devuelve null en la raiz`() {
        assertThat(DirectoryTree.parentOf("/")).isNull()
        assertThat(DirectoryTree.parentOf("")).isNull()
        assertThat(DirectoryTree.parentOf("/storage")).isNull()
    }

    @Test
    fun `breadcrumbs arma el camino desde la raiz`() {
        val migas = DirectoryTree.breadcrumbs(raiz, "$raiz/Music/acdc")

        assertThat(migas.map { it.name }).containsExactly("0", "Music", "acdc").inOrder()
        assertThat(migas.last().path).isEqualTo("$raiz/Music/acdc")
    }

    @Test
    fun `breadcrumbs en la propia raiz devuelve un solo tramo`() {
        val migas = DirectoryTree.breadcrumbs(raiz, raiz)

        assertThat(migas).hasSize(1)
        assertThat(migas.single().path).isEqualTo(raiz)
    }

    @Test
    fun `breadcrumbs de una ruta ajena a la raiz devuelve solo esa ruta`() {
        val migas = DirectoryTree.breadcrumbs(raiz, "/otro/lado")

        assertThat(migas).hasSize(1)
        assertThat(migas.single().path).isEqualTo("/otro/lado")
    }
}
