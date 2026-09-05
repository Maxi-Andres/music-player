package com.max.musicplayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("apariencia")

/**
 * Guarda y lee las preferencias de apariencia.
 *
 * Recibe el [DataStore] en vez de un Context para poder testearlo con un almacen
 * temporal, sin Android de por medio.
 */
class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            accentColor = prefs[ACENTO] ?: AppSettings.DEFAULT_ACCENT,
            backgroundColor = prefs[FONDO] ?: AppSettings.DEFAULT_BACKGROUND,
            folderColor = prefs[CARPETAS] ?: AppSettings.DEFAULT_FOLDER,
            backgroundImageUri = prefs[IMAGEN],
            backgroundDim = prefs[OSCURECER] ?: AppSettings.DEFAULT_DIM,
            tintFromArtwork = prefs[TINTE] ?: true,
        )
    }

    suspend fun setAccentColor(color: Int) = edit { it[ACENTO] = color }

    suspend fun setBackgroundColor(color: Int) = edit {
        it[FONDO] = color
        // Elegir un color de fondo descarta la imagen: si no, no se veria el cambio.
        it.remove(IMAGEN)
    }

    suspend fun setFolderColor(color: Int) = edit { it[CARPETAS] = color }

    suspend fun setBackgroundImage(uri: String?) = edit {
        if (uri == null) it.remove(IMAGEN) else it[IMAGEN] = uri
    }

    suspend fun setBackgroundDim(value: Float) = edit { it[OSCURECER] = value }

    suspend fun setTintFromArtwork(enabled: Boolean) = edit { it[TINTE] = enabled }

    suspend fun resetAll() = edit { it.clear() }

    private suspend fun edit(bloque: (MutablePreferences) -> Unit) {
        store.edit(bloque)
    }

    companion object {
        fun from(context: Context) = SettingsRepository(context.settingsStore)

        val ACENTO = intPreferencesKey("color_acento")
        val FONDO = intPreferencesKey("color_fondo")
        val CARPETAS = intPreferencesKey("color_carpetas")
        val IMAGEN = stringPreferencesKey("imagen_fondo")
        val OSCURECER = floatPreferencesKey("oscurecer_fondo")
        val TINTE = booleanPreferencesKey("tinte_caratula")
    }
}
