package com.max.musicplayer.data

/** Criterios de ordenamiento que ofrece el boton de ordenar de las listas. */
enum class SongSort {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    DATE_ADDED_DESC,
    DATE_ADDED_ASC,
    DURATION_ASC,
    DURATION_DESC,
}

enum class FolderSort {
    NAME_ASC,
    NAME_DESC,
    SONG_COUNT_DESC,
    SONG_COUNT_ASC,
}
