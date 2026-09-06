package com.max.musicplayer.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.max.musicplayer.data.AppSettings
import com.max.musicplayer.data.Release
import com.max.musicplayer.data.SettingsRepository
import com.max.musicplayer.data.UpdateChecker
import com.max.musicplayer.net.GitHubReleases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** En que anda la busqueda de una version nueva. */
sealed interface UpdateState {
    /** Todavia no se busco nada en esta sesion. */
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    /** Sin red, o GitHub no contesto. */
    data object Failed : UpdateState
}

/** Estado de la pantalla de personalizacion. */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository.from(app)

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /** La version instalada, para mostrarla y para comparar contra la de GitHub. */
    val installedVersion: String = runCatching {
        val pm = app.packageManager
        pm.getPackageInfo(app.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

    /**
     * Busca si hay una version nueva.
     *
     * Con [force] en false (el chequeo automatico al abrir) no se conecta si ya se
     * consulto hace menos de [UpdateChecker.CHECK_INTERVAL_MS]; el boton de la pantalla
     * de personalizacion pasa true y se conecta siempre.
     */
    fun checkForUpdates(force: Boolean) {
        if (_updateState.value is UpdateState.Checking) return
        viewModelScope.launch {
            val ahora = System.currentTimeMillis()
            if (!force) {
                val ultimo = repo.lastUpdateCheckMs.first()
                if (ahora - ultimo < UpdateChecker.CHECK_INTERVAL_MS) return@launch
            }

            _updateState.value = UpdateState.Checking
            val ultima = GitHubReleases.latestJson()?.let(UpdateChecker::parse)
            _updateState.value = when {
                ultima == null -> UpdateState.Failed
                UpdateChecker.isNewer(installedVersion, ultima.version) ->
                    UpdateState.Available(ultima)
                else -> UpdateState.UpToDate
            }
            // Solo cuenta como chequeo hecho si de verdad se pudo consultar: si no
            // habia red, conviene reintentar en el proximo arranque.
            if (ultima != null) repo.setLastUpdateCheck(ahora)
        }
    }

    fun setAccentColor(color: Int) = viewModelScope.launch { repo.setAccentColor(color) }

    fun setBackgroundColor(color: Int) = viewModelScope.launch { repo.setBackgroundColor(color) }

    fun setFolderColor(color: Int) = viewModelScope.launch { repo.setFolderColor(color) }

    fun setBackgroundDim(value: Float) = viewModelScope.launch { repo.setBackgroundDim(value) }

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
