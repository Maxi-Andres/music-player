package com.max.musicplayer.data

import java.util.Locale

/**
 * Logica pura de la biblioteca: agrupar, ordenar, buscar e indexar.
 *
 * Todo aca es determinista y sin dependencias de Android, para poder testearlo
 * como unit test comun en la JVM. Es la parte mas facil de romper sin darse cuenta,
 * asi que es la que tiene mayor cobertura.
 */
object MusicLibrary {

    /**
     * Clave de ordenamiento: mismo texto sin acentos y en minusculas.
     *
     * A proposito NO se usa java.text.Collator: con strength PRIMARY trata el espacio
     * como ignorable, y entonces "ambiente" quedaria antes que "a romper". La app de
     * referencia (ver docs/reference/02-tab-carpetas.jpeg) ordena "a romper" primero,
     * o sea que el espacio pesa y va antes que las letras. Comparar la clave como String
     * comun da exactamente ese orden, y ademas es mas rapido y predecible.
     */
    internal fun sortKey(text: String): String = stripAccents(text).lowercase(Locale.ROOT)

    private val byName = Comparator<String> { a, b -> sortKey(a).compareTo(sortKey(b)) }

    /**
     * Agrupa las canciones por la carpeta que las contiene.
     * Las carpetas sin nombre (archivos en la raiz) se descartan.
     */
    fun groupIntoFolders(songs: List<Song>): List<MusicFolder> =
        songs.asSequence()
            .filter { it.folderPath.isNotEmpty() && it.folderName.isNotEmpty() }
            .groupingBy { it.folderPath }
            .eachCount()
            .map { (path, count) ->
                MusicFolder(
                    path = path,
                    name = path.substringAfterLast('/'),
                    songCount = count,
                )
            }
            .sortedWith(compareBy(byName) { it.name })

    fun songsInFolder(songs: List<Song>, folderPath: String): List<Song> =
        songs.filter { it.folderPath == folderPath }

    fun sortSongs(songs: List<Song>, sort: SongSort): List<Song> = when (sort) {
        SongSort.TITLE_ASC -> songs.sortedWith(compareBy(byName) { it.title })
        SongSort.TITLE_DESC -> songs.sortedWith(compareBy(byName) { it.title }).reversed()
        SongSort.ARTIST_ASC -> songs.sortedWith(
            compareBy<Song, String>(byName) { it.displayArtist }
                .then(compareBy<Song, String>(byName) { it.title }),
        )
        SongSort.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateModifiedSeconds }
        SongSort.DATE_ADDED_ASC -> songs.sortedBy { it.dateModifiedSeconds }
        SongSort.DURATION_ASC -> songs.sortedBy { it.durationMs }
        SongSort.DURATION_DESC -> songs.sortedByDescending { it.durationMs }
    }

    fun sortFolders(folders: List<MusicFolder>, sort: FolderSort): List<MusicFolder> = when (sort) {
        FolderSort.NAME_ASC -> folders.sortedWith(compareBy(byName) { it.name })
        FolderSort.NAME_DESC -> folders.sortedWith(compareBy(byName) { it.name }).reversed()
        FolderSort.SONG_COUNT_DESC -> folders.sortedWith(
            compareByDescending<MusicFolder> { it.songCount }.then(compareBy(byName) { it.name }),
        )
        FolderSort.SONG_COUNT_ASC -> folders.sortedWith(
            compareBy<MusicFolder> { it.songCount }.then(compareBy(byName) { it.name }),
        )
    }

    /**
     * Busqueda por titulo, artista, album o carpeta.
     * Insensible a mayusculas y acentos: "cancion" encuentra "Canción".
     */
    fun search(songs: List<Song>, query: String): List<Song> {
        val q = normalize(query)
        if (q.isBlank()) return songs
        return songs.filter { song ->
            normalize(song.title).contains(q) ||
                normalize(song.artist).contains(q) ||
                normalize(song.album).contains(q) ||
                normalize(song.folderName).contains(q)
        }
    }

    fun searchFolders(folders: List<MusicFolder>, query: String): List<MusicFolder> {
        val q = normalize(query)
        if (q.isBlank()) return folders
        return folders.filter { normalize(it.name).contains(q) }
    }

    /**
     * Letras presentes en la lista, para la barra de indice A-Z del costado.
     * Lo que no arranca con letra se agrupa bajo "#".
     */
    fun alphabetIndex(labels: List<String>): List<String> =
        labels.map(::indexLetterOf).distinct().sortedWith { a, b ->
            when {
                a == b -> 0
                a == "#" -> 1 // el "#" siempre al final
                b == "#" -> -1
                else -> a.compareTo(b)
            }
        }

    /** Primera posicion de la lista cuya etiqueta cae bajo [letter], o -1 si no hay. */
    fun firstIndexForLetter(labels: List<String>, letter: String): Int =
        labels.indexOfFirst { indexLetterOf(it) == letter }

    internal fun indexLetterOf(label: String): String {
        val first = label.trim().firstOrNull() ?: return "#"
        if (!first.isLetter()) return "#"
        return stripAccents(first.toString()).uppercase(Locale.ROOT)
    }

    private fun normalize(text: String): String =
        stripAccents(text).lowercase(Locale.ROOT)

    /**
     * Saca los acentos descomponiendo el texto (NFD) y tirando las marcas diacriticas.
     * Se filtra por categoria Unicode en vez de por regex para no depender de escapes.
     */
    private fun stripAccents(text: String): String =
        java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
}
