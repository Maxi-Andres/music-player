package com.max.musicplayer.data

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lee la biblioteca de audio del dispositivo desde MediaStore.
 *
 * MediaStore es el indice que mantiene el propio Android, asi que no hay que recorrer
 * el disco a mano ni pedir permisos de "todos los archivos".
 */
class MediaStoreScanner(private val context: Context) {

    suspend fun scan(): List<Song> = withContext(Dispatchers.IO) {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            SELECTION,
            arrayOf(MIN_DURATION_MS.toString()),
            null,
        )?.use { readSongs(it) } ?: emptyList()
    }

    internal companion object {
        /**
         * Descarta clips muy cortos (pitidos de UI, fragmentos rotos) pero deja pasar
         * las notas de voz y audios de WhatsApp, que si aparecen en la biblioteca.
         */
        const val MIN_DURATION_MS = 5_000L

        val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )

        /**
         * Excluye tonos, alarmas y sonidos de notificacion. No se filtra por IS_MUSIC
         * porque muchos audios validos (WhatsApp, grabaciones) vienen con IS_MUSIC = 0.
         */
        const val SELECTION =
            "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
                "${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
                "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
                "${MediaStore.Audio.Media.DURATION} >= ?"

        /**
         * Convierte el cursor en canciones. Es `internal` para poder testearlo con un
         * MatrixCursor, sin dispositivo ni base de datos real.
         */
        fun readSongs(cursor: Cursor): List<Song> {
            if (cursor.count == 0) return emptyList()

            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            val songs = ArrayList<Song>(cursor.count)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue // sin ruta no hay carpeta
                songs += Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol)
                        ?: path.substringAfterLast('/').substringBeforeLast('.'),
                    artist = cursor.getString(artistCol) ?: Song.UNKNOWN_ARTIST,
                    album = cursor.getString(albumCol).orEmpty(),
                    albumId = cursor.getLong(albumIdCol),
                    durationMs = cursor.getLong(durationCol),
                    filePath = path,
                    dateModifiedSeconds = cursor.getLong(dateCol),
                )
            }
            return songs
        }
    }
}
