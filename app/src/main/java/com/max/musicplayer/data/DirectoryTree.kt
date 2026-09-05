package com.max.musicplayer.data

/** Un directorio dentro del navegador de archivos ("Directorios"). */
data class DirectoryNode(
    val path: String,
    val name: String,
    /** Canciones dentro de este directorio, incluyendo todo lo que cuelga debajo. */
    val songCount: Int,
    /** Subdirectorios inmediatos que contienen musica. */
    val subdirectoryCount: Int,
)

/**
 * Arbol de directorios reconstruido a partir de las rutas de las canciones.
 *
 * No toca el sistema de archivos: se deriva de lo que ya devolvio MediaStore, asi que
 * nunca muestra carpetas vacias ni pide permisos extra. Al ser logica pura, se testea
 * sin dispositivo.
 */
object DirectoryTree {

    /**
     * Directorio desde el que conviene arrancar a navegar: el ancestro comun mas profundo
     * de toda la musica. En la practica suele ser /storage/emulated/0, asi se evita que
     * el usuario tenga que bajar tres niveles vacios antes de ver algo.
     */
    fun commonRoot(songs: List<Song>): String {
        val carpetas = songs.map { it.folderPath }.filter { it.isNotEmpty() }.distinct()
        if (carpetas.isEmpty()) return ""
        if (carpetas.size == 1) return carpetas.first()

        val partido = carpetas.map { it.split('/') }
        val minimo = partido.minOf { it.size }
        var comunes = 0
        while (comunes < minimo && partido.all { it[comunes] == partido[0][comunes] }) {
            comunes++
        }
        return partido[0].take(comunes).joinToString("/")
    }

    /** Subdirectorios inmediatos de [path] que contienen musica, ordenados por nombre. */
    fun childrenOf(songs: List<Song>, path: String): List<DirectoryNode> {
        val prefijo = if (path.isEmpty()) "" else "$path/"

        val hijos = songs.asSequence()
            .map { it.folderPath }
            .filter { it.startsWith(prefijo) && it.length > prefijo.length }
            .map { ruta ->
                // Primer segmento despues del prefijo: el hijo directo.
                val resto = ruta.removePrefix(prefijo)
                prefijo + resto.substringBefore('/')
            }
            .distinct()
            .toList()

        return hijos
            .map { rutaHijo ->
                DirectoryNode(
                    path = rutaHijo,
                    name = rutaHijo.substringAfterLast('/'),
                    songCount = songsUnder(songs, rutaHijo).size,
                    subdirectoryCount = countImmediateSubdirs(songs, rutaHijo),
                )
            }
            .sortedWith(compareBy { MusicLibrary.sortKey(it.name) })
    }

    private fun countImmediateSubdirs(songs: List<Song>, path: String): Int {
        val prefijo = "$path/"
        return songs.asSequence()
            .map { it.folderPath }
            .filter { it.startsWith(prefijo) && it.length > prefijo.length }
            .map { it.removePrefix(prefijo).substringBefore('/') }
            .distinct()
            .count()
    }

    /** Canciones que estan exactamente en [path], sin incluir subcarpetas. */
    fun songsDirectlyIn(songs: List<Song>, path: String): List<Song> =
        songs.filter { it.folderPath == path }

    /** Canciones en [path] y en todo lo que cuelga debajo. */
    fun songsUnder(songs: List<Song>, path: String): List<Song> {
        val prefijo = "$path/"
        return songs.filter { it.folderPath == path || it.folderPath.startsWith(prefijo) }
    }

    /** Directorio padre, o null si [path] ya es la raiz o esta vacio. */
    fun parentOf(path: String): String? {
        if (path.isEmpty() || path == "/") return null
        val padre = path.substringBeforeLast('/', "")
        return padre.ifEmpty { null }
    }

    /**
     * Camino desde [root] hasta [path], para la barra de navegacion de arriba.
     * Si [path] no cuelga de [root], devuelve solo [path].
     */
    fun breadcrumbs(root: String, path: String): List<DirectoryNode> {
        if (!path.startsWith(root)) {
            return listOf(nodoLiviano(path))
        }
        val resto = path.removePrefix(root).trim('/')
        val segmentos = if (resto.isEmpty()) emptyList() else resto.split('/')

        val migas = mutableListOf(nodoLiviano(root))
        var acumulado = root
        segmentos.forEach { seg ->
            acumulado = "$acumulado/$seg"
            migas += nodoLiviano(acumulado)
        }
        return migas
    }

    private fun nodoLiviano(path: String) = DirectoryNode(
        path = path,
        name = path.substringAfterLast('/').ifEmpty { path },
        songCount = 0,
        subdirectoryCount = 0,
    )
}
