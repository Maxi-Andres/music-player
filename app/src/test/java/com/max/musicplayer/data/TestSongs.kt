package com.max.musicplayer.data

/** Constructor corto de canciones para los tests: solo se declara lo que importa. */
fun song(
    id: Long = 1L,
    title: String = "Cancion",
    artist: String = "Artista",
    album: String = "Album",
    albumId: Long = 100L,
    durationMs: Long = 180_000L,
    filePath: String = "/storage/emulated/0/Music/carpeta/tema.mp3",
    dateModifiedSeconds: Long = 1_700_000_000L,
) = Song(id, title, artist, album, albumId, durationMs, filePath, dateModifiedSeconds)
