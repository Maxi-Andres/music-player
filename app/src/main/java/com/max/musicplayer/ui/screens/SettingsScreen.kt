package com.max.musicplayer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.max.musicplayer.R
import com.max.musicplayer.data.AppSettings

/**
 * Personalizacion de la apariencia: color de acento, fondo (color o imagen) y color
 * de los iconos de carpeta.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onAccentColor: (Int) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onFolderColor: (Int) -> Unit,
    onBackgroundImage: (android.net.Uri) -> Unit,
    onClearBackgroundImage: () -> Unit,
    onBackgroundDim: (Float) -> Unit,
    onReset: () -> Unit,
) {
    val elegirImagen = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onBackgroundImage(uri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Seccion(stringResource(R.string.settings_accent))
        FilaDeColores(
            colores = AppSettings.ACCENT_CHOICES,
            seleccionado = settings.accentColor,
            onSelect = onAccentColor,
        )

        Seccion(stringResource(R.string.settings_background))
        FilaDeColores(
            colores = AppSettings.BACKGROUND_CHOICES,
            // Con imagen puesta no hay color de fondo seleccionado.
            seleccionado = if (settings.usesBackgroundImage) null else settings.backgroundColor,
            onSelect = onBackgroundColor,
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = { elegirImagen.launch(arrayOf("image/*")) }) {
                Text(stringResource(R.string.settings_pick_image))
            }
            if (settings.usesBackgroundImage) {
                OutlinedButton(onClick = onClearBackgroundImage) {
                    Text(stringResource(R.string.settings_remove_image))
                }
            }
        }

        if (settings.usesBackgroundImage) {
            Text(
                text = stringResource(R.string.settings_dim),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
            Slider(
                value = settings.backgroundDim,
                valueRange = 0f..0.9f,
                onValueChange = onBackgroundDim,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Seccion(stringResource(R.string.settings_folder_color))
        FilaDeColores(
            colores = AppSettings.ACCENT_CHOICES,
            seleccionado = settings.folderColor,
            onSelect = onFolderColor,
        )

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(stringResource(R.string.settings_reset))
        }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun FilaDeColores(
    colores: List<Int>,
    seleccionado: Int?,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        colores.forEach { valor ->
            val color = Color(valor)
            val activo = valor == seleccionado
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (activo) 3.dp else 1.dp,
                        color = if (activo) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelect(valor) },
                contentAlignment = Alignment.Center,
            ) {
                if (activo) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
