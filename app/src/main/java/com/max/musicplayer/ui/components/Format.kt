package com.max.musicplayer.ui.components

import java.util.Locale
import java.util.concurrent.TimeUnit

/** Duracion como 3:45, o 1:02:33 si pasa la hora. */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSegundos = TimeUnit.MILLISECONDS.toSeconds(ms)
    val horas = totalSegundos / 3600
    val minutos = (totalSegundos % 3600) / 60
    val segundos = totalSegundos % 60
    return if (horas > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", horas, minutos, segundos)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutos, segundos)
    }
}

/**
 * Fecha corta que muestra la lista de canciones, como "08-31".
 * [epochSeconds] es lo que devuelve MediaStore en DATE_MODIFIED.
 */
fun formatShortDate(epochSeconds: Long): String {
    if (epochSeconds <= 0) return ""
    val fecha = java.time.Instant.ofEpochSecond(epochSeconds)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return String.format(Locale.ROOT, "%02d-%02d", fecha.monthValue, fecha.dayOfMonth)
}
