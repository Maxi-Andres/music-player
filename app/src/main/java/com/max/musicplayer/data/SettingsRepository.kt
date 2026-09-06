package com.max.musicplayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
            glassEffect = prefs[VIDRIO] ?: true,
            ringOnNowPlaying = prefs[ANILLO_GRANDE] ?: false,
            ringFromArtwork = prefs[ANILLO_CARATULA] ?: false,
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

    suspend fun setGlassEffect(enabled: Boolean) = edit { it[VIDRIO] = enabled }

    suspend fun setRingOnNowPlaying(enabled: Boolean) = edit { it[ANILLO_GRANDE] = enabled }

    suspend fun setRingFromArtwork(enabled: Boolean) = edit { it[ANILLO_CARATULA] = enabled }

    /**
     * Cuando se consulto GitHub por ultima vez, para no pegarle en cada arranque.
     * Se guarda aparte de [AppSettings] porque no es algo que el usuario elija.
     */
    val lastUpdateCheckMs: Flow<Long> = store.data.map { it[ULTIMO_CHEQUEO] ?: 0L }

    suspend fun setLastUpdateCheck(epochMs: Long) = edit { it[ULTIMO_CHEQUEO] = epochMs }

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
        val ULTIMO_CHEQUEO = longPreferencesKey("ultimo_chequeo_version")
        val VIDRIO = booleanPreferencesKey("efecto_vidrio")
        val ANILLO_GRANDE = booleanPreferencesKey("anillo_reproduciendo")
        val ANILLO_CARATULA = booleanPreferencesKey("anillo_caratula")
    }
}
