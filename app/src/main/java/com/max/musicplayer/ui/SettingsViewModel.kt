package com.max.musicplayer.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.max.musicplayer.data.AppSettings
import com.max.musicplayer.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla de personalizacion. */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository.from(app)

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setAccentColor(color: Int) = viewModelScope.launch { repo.setAccentColor(color) }

    fun setBackgroundColor(color: Int) = viewModelScope.launch { repo.setBackgroundColor(color) }

    fun setFolderColor(color: Int) = viewModelScope.launch { repo.setFolderColor(color) }

    fun setBackgroundDim(value: Float) = viewModelScope.launch { repo.setBackgroundDim(value) }

    fun clearBackgroundImage() = viewModelScope.launch { repo.setBackgroundImage(null) }

    /**
     * Guarda la imagen elegida. Hay que retener el permiso: sin esto la imagen deja de
     * poder leerse cuando se reinicia la app.
     */
    fun setBackgroundImage(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        viewModelScope.launch { repo.setBackgroundImage(uri.toString()) }
    }

    fun setTintFromArtwork(enabled: Boolean) =
        viewModelScope.launch { repo.setTintFromArtwork(enabled) }

    fun resetAll() = viewModelScope.launch { repo.resetAll() }
}
