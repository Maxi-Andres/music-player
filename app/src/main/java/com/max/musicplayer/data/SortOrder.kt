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
    ;

    /**
     * Si la lista queda ordenada por titulo. La barra de indice A-Z solo se muestra
     * en ese caso: con cualquier otro orden, "saltar a la B" caeria en cualquier lado.
     */
    val isAlphabetical: Boolean
        get() = this == TITLE_ASC || this == TITLE_DESC
}

enum class FolderSort {
    NAME_ASC,
    NAME_DESC,
    SONG_COUNT_DESC,
    SONG_COUNT_ASC,
    ;

    val isAlphabetical: Boolean
        get() = this == NAME_ASC || this == NAME_DESC
}
