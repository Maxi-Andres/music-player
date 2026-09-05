package com.max.musicplayer.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MusicLibraryTest {

    private val base = "/storage/emulated/0/Music"

    private val biblioteca = listOf(
        song(id = 1, title = "T.N.T.", artist = "AC/DC", filePath = "$base/acdc/tnt.mp3"),
        song(id = 2, title = "Back In Black", artist = "AC/DC", filePath = "$base/acdc/bib.mp3"),
        song(id = 3, title = "Zombie", artist = "The Cranberries", filePath = "$base/a romper/zombie.mp3"),
        song(id = 4, title = "Angeles", artist = "Alguien", filePath = "$base/ambiente/angeles.mp3"),
        song(id = 5, title = "Big Gun", artist = "AC/DC", filePath = "$base/acdc/biggun.mp3"),
    )

    // --- agrupar en carpetas ---

    @Test
    fun `agrupa las canciones por carpeta y cuenta bien`() {
        val carpetas = MusicLibrary.groupIntoFolders(biblioteca)

        assertThat(carpetas).hasSize(3)
        assertThat(carpetas.first { it.name == "acdc" }.songCount).isEqualTo(3)
        assertThat(carpetas.first { it.name == "a romper" }.songCount).isEqualTo(1)
        assertThat(carpetas.first { it.name == "ambiente" }.songCount).isEqualTo(1)
    }

    @Test
    fun `las carpetas vienen ordenadas alfabeticamente`() {
        val nombres = MusicLibrary.groupIntoFolders(biblioteca).map { it.name }

        assertThat(nombres).isInOrder()
    }

    @Test
    fun `el orden de carpetas reproduce el de la app de referencia`() {
        // Orden exacto de docs/reference/02-tab-carpetas.jpeg: el espacio y el guion
        // pesan y van antes que las letras, por eso "a romper" va primera.
        val nombres = listOf(
            "andres calamaro", "acdc", "animacion------", "a romper-------",
            "ani------------", "aerosmith", "alphaville", "ambiente-------",
            "arctic monkeys",
        )
        val carpetas = nombres.map { MusicFolder(path = "/m/$it", name = it, songCount = 1) }

        val ordenadas = MusicLibrary.sortFolders(carpetas, FolderSort.NAME_ASC).map { it.name }

        assertThat(ordenadas).containsExactly(
            "a romper-------",
            "acdc",
            "aerosmith",
            "alphaville",
            "ambiente-------",
            "andres calamaro",
            "ani------------",
            "animacion------",
            "arctic monkeys",
        ).inOrder()
    }

    @Test
    fun `la carpeta guarda la ruta completa, no solo el nombre`() {
        val acdc = MusicLibrary.groupIntoFolders(biblioteca).first { it.name == "acdc" }

        assertThat(acdc.path).isEqualTo("$base/acdc")
    }

    @Test
    fun `dos carpetas con el mismo nombre en rutas distintas no se mezclan`() {
        val songs = listOf(
            song(id = 1, filePath = "$base/rock/acdc/a.mp3"),
            song(id = 2, filePath = "$base/viejo/acdc/b.mp3"),
        )

        val carpetas = MusicLibrary.groupIntoFolders(songs)

        assertThat(carpetas).hasSize(2)
        assertThat(carpetas.map { it.path })
            .containsExactly("$base/rock/acdc", "$base/viejo/acdc")
    }

    @Test
    fun `descarta archivos sueltos sin carpeta`() {
        val carpetas = MusicLibrary.groupIntoFolders(listOf(song(filePath = "suelto.mp3")))

        assertThat(carpetas).isEmpty()
    }

    @Test
    fun `biblioteca vacia devuelve cero carpetas`() {
        assertThat(MusicLibrary.groupIntoFolders(emptyList())).isEmpty()
    }

    @Test
    fun `songsInFolder devuelve solo las de esa carpeta`() {
        val enAcdc = MusicLibrary.songsInFolder(biblioteca, "$base/acdc")

        assertThat(enAcdc).hasSize(3)
        assertThat(enAcdc.map { it.title })
            .containsExactly("T.N.T.", "Back In Black", "Big Gun")
    }

    // --- ordenar ---

    @Test
    fun `ordena por titulo ignorando acentos`() {
        val songs = listOf(
            song(id = 1, title = "Zapato"),
            song(id = 2, title = "Ángeles"),
            song(id = 3, title = "Bandera"),
        )

        val titulos = MusicLibrary.sortSongs(songs, SongSort.TITLE_ASC).map { it.title }

        // El acento no debe mandar la palabra al final de la lista.
        assertThat(titulos).containsExactly("Ángeles", "Bandera", "Zapato").inOrder()
    }

    @Test
    fun `ordenar por titulo descendente invierte el ascendente`() {
        val asc = MusicLibrary.sortSongs(biblioteca, SongSort.TITLE_ASC).map { it.id }
        val desc = MusicLibrary.sortSongs(biblioteca, SongSort.TITLE_DESC).map { it.id }

        assertThat(desc).isEqualTo(asc.reversed())
    }

    @Test
    fun `al ordenar por artista se desempata por titulo`() {
        val orden = MusicLibrary.sortSongs(biblioteca, SongSort.ARTIST_ASC)
            .filter { it.artist == "AC/DC" }
            .map { it.title }

        assertThat(orden).containsExactly("Back In Black", "Big Gun", "T.N.T.").inOrder()
    }

    @Test
    fun `ordena por fecha de agregado, mas nuevo primero`() {
        val songs = listOf(
            song(id = 1, dateModifiedSeconds = 100),
            song(id = 2, dateModifiedSeconds = 300),
            song(id = 3, dateModifiedSeconds = 200),
        )

        val ids = MusicLibrary.sortSongs(songs, SongSort.DATE_ADDED_DESC).map { it.id }

        assertThat(ids).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun `ordena por duracion en ambos sentidos`() {
        val songs = listOf(
            song(id = 1, durationMs = 300_000),
            song(id = 2, durationMs = 100_000),
        )

        assertThat(MusicLibrary.sortSongs(songs, SongSort.DURATION_ASC).map { it.id })
            .containsExactly(2L, 1L).inOrder()
        assertThat(MusicLibrary.sortSongs(songs, SongSort.DURATION_DESC).map { it.id })
            .containsExactly(1L, 2L).inOrder()
    }

    @Test
    fun `ordenar nunca pierde ni duplica canciones`() {
        SongSort.entries.forEach { sort ->
            val resultado = MusicLibrary.sortSongs(biblioteca, sort)

            assertThat(resultado).hasSize(biblioteca.size)
            assertThat(resultado.map { it.id })
                .containsExactlyElementsIn(biblioteca.map { it.id })
        }
    }

    @Test
    fun `ordena carpetas por cantidad de canciones y desempata por nombre`() {
        val carpetas = MusicLibrary.groupIntoFolders(biblioteca)

        val porCantidad = MusicLibrary.sortFolders(carpetas, FolderSort.SONG_COUNT_DESC)

        assertThat(porCantidad.first().name).isEqualTo("acdc")
        assertThat(porCantidad.drop(1).map { it.name })
            .containsExactly("a romper", "ambiente").inOrder()
    }

    // --- buscar ---

    @Test
    fun `busca por titulo sin importar mayusculas`() {
        val r = MusicLibrary.search(biblioteca, "ZOMBIE")

        assertThat(r.map { it.title }).containsExactly("Zombie")
    }

    @Test
    fun `busca ignorando acentos en ambos sentidos`() {
        val conAcento = listOf(song(id = 9, title = "Ángeles"))

        assertThat(MusicLibrary.search(conAcento, "angeles")).hasSize(1)
        assertThat(MusicLibrary.search(conAcento, "Ángeles")).hasSize(1)
    }

    @Test
    fun `busca por artista y por nombre de carpeta`() {
        assertThat(MusicLibrary.search(biblioteca, "cranberries")).hasSize(1)
        assertThat(MusicLibrary.search(biblioteca, "acdc")).hasSize(3)
    }

    @Test
    fun `busqueda vacia devuelve todo`() {
        assertThat(MusicLibrary.search(biblioteca, "")).hasSize(biblioteca.size)
        assertThat(MusicLibrary.search(biblioteca, "   ")).hasSize(biblioteca.size)
    }

    @Test
    fun `busqueda sin resultados devuelve lista vacia`() {
        assertThat(MusicLibrary.search(biblioteca, "reggaeton")).isEmpty()
    }

    @Test
    fun `searchFolders filtra por nombre de carpeta`() {
        val carpetas = MusicLibrary.groupIntoFolders(biblioteca)

        assertThat(MusicLibrary.searchFolders(carpetas, "AMBI").map { it.name })
            .containsExactly("ambiente")
    }

    // --- indice A-Z ---

    @Test
    fun `el indice lista las letras presentes, sin repetir`() {
        val letras = MusicLibrary.alphabetIndex(listOf("acdc", "aerosmith", "Beatles", "charly"))

        assertThat(letras).containsExactly("A", "B", "C").inOrder()
    }

    @Test
    fun `lo que no empieza con letra va bajo almohadilla y queda al final`() {
        val letras = MusicLibrary.alphabetIndex(listOf("3 Doors Down", "acdc", "_temp"))

        assertThat(letras).containsExactly("A", "#").inOrder()
    }

    @Test
    fun `los titulos con caracteres raros van al final, no arriba`() {
        val songs = listOf(
            song(id = 1, title = "雨に唄えば"),
            song(id = 2, title = "Zapato"),
            song(id = 3, title = "3 Doors Down"),
            song(id = 4, title = "Айсберг"),
            song(id = 5, title = "Abrelatas"),
            song(id = 6, title = "_borrador"),
        )

        val titulos = MusicLibrary.sortSongs(songs, SongSort.TITLE_ASC).map { it.title }

        // Primero las que arrancan con A-Z, en orden; despues todo lo demas.
        assertThat(titulos.take(2)).containsExactly("Abrelatas", "Zapato").inOrder()
        assertThat(titulos.drop(2))
            .containsExactly("3 Doors Down", "_borrador", "Айсберг", "雨に唄えば")
    }

    @Test
    fun `japones y cirilico se agrupan bajo almohadilla, no como letras sueltas`() {
        // Char.isLetter() da true para estos, y antes aparecian como entradas propias
        // en la barra de indice (ゆ, イ, 雨...).
        assertThat(MusicLibrary.indexLetterOf("雨に唄えば")).isEqualTo("#")
        assertThat(MusicLibrary.indexLetterOf("Айсберг")).isEqualTo("#")
        assertThat(MusicLibrary.indexLetterOf("ゆめ")).isEqualTo("#")
    }

    @Test
    fun `el indice de una lista mezclada tiene solo letras A-Z y almohadilla`() {
        val letras = MusicLibrary.alphabetIndex(
            listOf("Abrelatas", "雨に唄えば", "Zapato", "Айсберг", "3 Doors Down"),
        )

        assertThat(letras).containsExactly("A", "Z", "#").inOrder()
    }

    @Test
    fun `las letras acentuadas se indexan bajo su letra base`() {
        assertThat(MusicLibrary.indexLetterOf("Ángeles")).isEqualTo("A")
        assertThat(MusicLibrary.indexLetterOf("Éxito")).isEqualTo("E")
    }

    @Test
    fun `firstIndexForLetter ubica la primera coincidencia`() {
        val labels = listOf("acdc", "aerosmith", "beatles", "charly")

        assertThat(MusicLibrary.firstIndexForLetter(labels, "B")).isEqualTo(2)
        assertThat(MusicLibrary.firstIndexForLetter(labels, "A")).isEqualTo(0)
    }

    @Test
    fun `firstIndexForLetter devuelve -1 si la letra no esta`() {
        assertThat(MusicLibrary.firstIndexForLetter(listOf("acdc"), "Z")).isEqualTo(-1)
    }

    @Test
    fun `indexLetterOf tolera una etiqueta vacia`() {
        assertThat(MusicLibrary.indexLetterOf("")).isEqualTo("#")
        assertThat(MusicLibrary.indexLetterOf("   ")).isEqualTo("#")
    }
}
