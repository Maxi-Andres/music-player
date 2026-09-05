package com.max.musicplayer.data

/** Una carpeta del disco que contiene al menos una cancion. */
data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int,
)
