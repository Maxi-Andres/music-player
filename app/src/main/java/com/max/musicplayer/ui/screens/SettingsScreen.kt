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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.max.musicplayer.R
import com.max.musicplayer.data.AppSettings
import com.max.musicplayer.ui.UpdateState
import kotlin.math.roundToInt

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
    onBackgroundDim: (Float) -> Unit,
    onTintFromArtwork: (Boolean) -> Unit,
    onReset: () -> Unit,
    installedVersion: String,
    updateState: UpdateState,
    onCheckUpdates: () -> Unit,
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

        // Un solo boton: para quitar la imagen alcanza con tocar un color, que ya la
        // descarta. Con dos botones el texto se partia en dos renglones.
        Button(
            onClick = { elegirImagen.launch(arrayOf("image/*")) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(
                    if (settings.usesBackgroundImage) {
                        R.string.settings_change_image
                    } else {
                        R.string.settings_pick_image
                    },
                ),
            )
        }

        if (settings.usesBackgroundImage) {
            Text(
                text = stringResource(R.string.settings_image_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            BarraDeOscurecer(valor = settings.backgroundDim, onChange = onBackgroundDim)
        }

        Seccion(stringResource(R.string.settings_now_playing))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTintFromArtwork(!settings.tintFromArtwork) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_tint_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.settings_tint_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = settings.tintFromArtwork,
                onCheckedChange = onTintFromArtwork,
            )
        }

        Seccion(stringResource(R.string.settings_folder_color))
        FilaDeColores(
            colores = AppSettings.ACCENT_CHOICES,
            seleccionado = settings.folderColor,
            onSelect = onFolderColor,
        )

        Seccion(stringResource(R.string.settings_updates))
        Actualizaciones(
            installedVersion = installedVersion,
            estado = updateState,
            onCheck = onCheckUpdates,
        )

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(stringResource(R.string.settings_reset))
        }

        // La pantalla scrollea: sin esto el ultimo boton queda pegado al borde.
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Version instalada y busqueda de una version nueva en GitHub.
 *
 * La app no se actualiza sola porque se instala por fuera de Play: lo unico que se
 * puede hacer es avisar y abrir la pagina de la release. Ver docs/publicar.md.
 */
@Composable
private fun Actualizaciones(
    installedVersion: String,
    estado: UpdateState,
    onCheck: () -> Unit,
) {
    val context = LocalContext.current

    Text(
        text = stringResource(R.string.settings_version, installedVersion),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    when (estado) {
        UpdateState.Idle -> Unit

        UpdateState.Checking -> Mensaje(stringResource(R.string.settings_checking))

        UpdateState.UpToDate -> Mensaje(stringResource(R.string.settings_up_to_date))

        UpdateState.Failed -> Mensaje(stringResource(R.string.settings_update_failed))

        is UpdateState.Available -> {
            Text(
                text = stringResource(
                    R.string.settings_update_available,
                    estado.release.version,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
            Button(
                onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(estado.release.pageUrl),
                    )
                    // El navegador arranca fuera de la pila de la app.
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            ) {
                Text(stringResource(R.string.settings_download))
            }
        }
    }

    TextButton(
        onClick = onCheck,
        enabled = estado != UpdateState.Checking,
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Text(stringResource(R.string.settings_check_updates))
    }
}

@Composable
private fun Mensaje(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
    )
}

/**
 * Control para oscurecer la imagen de fondo.
 *
 * Los botones de -/+ ocupan las puntas y empujan la barra hacia adentro: los extremos
 * (imagen a full o bien oscura) quedan lejos del borde de la pantalla, donde con funda
 * cuesta llegar con el dedo. Ademas van de a saltos parejos, asi que se puede ajustar
 * sin arrastrar.
 */
@Composable
private fun BarraDeOscurecer(valor: Float, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_dim),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                R.string.settings_dim_value,
                (valor * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange((valor - DIM_STEP).coerceIn(0f, DIM_MAX)) }) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_dim_less),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Slider(
            value = valor,
            valueRange = 0f..DIM_MAX,
            steps = DIM_STEPS,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onChange((valor + DIM_STEP).coerceIn(0f, DIM_MAX)) }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_dim_more),
                tint = MaterialTheme.colorScheme.onBackground,
            )
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

private const val DIM_MAX = 0.9f
private const val DIM_STEP = 0.05f

/** Cortes intermedios del slider: uno cada [DIM_STEP] entre 0 y [DIM_MAX]. */
private const val DIM_STEPS = 17
