package com.max.musicplayer.data

/**
 * Una cancion del dispositivo.
 *
 * A proposito no depende de ninguna clase de Android (ni Uri, ni Context): asi toda la
 * logica de biblioteca se puede testear en la JVM, sin emulador ni Robolectric.
 * Los URIs se exponen como String porque su formato es estable y documentado.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    /** Ruta absoluta en disco, ej: /storage/emulated/0/Music/acdc/tnt.mp3 */
    val filePath: String,
    /** Fecha de modificacion en segundos epoch (la que muestra la lista: "08-31"). */
    val dateModifiedSeconds: Long,
) {
    /** Carpeta contenedora, ej: /storage/emulated/0/Music/acdc */
    val folderPath: String
        get() = filePath.substringBeforeLast('/', "")

    /** Nombre visible de la carpeta, ej: acdc */
    val folderName: String
        get() = folderPath.substringAfterLast('/', "")

    val contentUri: String
        get() = "content://media/external/audio/media/$id"

    /** Artista tal como lo muestra la lista: MediaStore usa "<unknown>" cuando no hay tag. */
    val displayArtist: String
        get() = artist.takeIf { it.isNotBlank() && it != UNKNOWN_ARTIST_RAW } ?: UNKNOWN_ARTIST

    companion object {
        const val UNKNOWN_ARTIST_RAW = "<unknown>"
        const val UNKNOWN_ARTIST = "<unknown>"
    }
}
