package com.max.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.max.musicplayer.data.AppSettings
import com.max.musicplayer.ui.MusicApp
import com.max.musicplayer.ui.PermissionGate
import com.max.musicplayer.ui.SettingsViewModel
import com.max.musicplayer.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val ajustes by settingsVm.settings.collectAsStateWithLifecycle()

            MusicPlayerTheme(ajustes) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // La imagen va por detras de todo; con ella puesta el tema deja el
                    // fondo transparente para que se vea.
                    FondoPersonalizado(ajustes)

                    PermissionGate {
                        MusicApp(settingsVm = settingsVm)
                    }
                }
            }
        }
    }
}

@Composable
private fun FondoPersonalizado(settings: AppSettings) {
    if (!settings.usesBackgroundImage) return

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = settings.backgroundImageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Velo oscuro para que el texto se siga leyendo sobre cualquier foto.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = settings.backgroundDim)),
        )
    }
}
