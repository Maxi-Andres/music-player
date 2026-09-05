package com.max.musicplayer.ui.screens

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.max.musicplayer.R
import com.max.musicplayer.ui.theme.Amber
import com.max.musicplayer.ui.theme.TextSecondary

/**
 * Ecualizador del sistema aplicado a la sesion de audio de la app.
 *
 * Usa [android.media.audiofx.Equalizer], que no todos los fabricantes exponen a apps
 * de terceros; si no esta disponible se avisa en vez de mostrar controles que no hacen
 * nada.
 */
@Composable
fun EqualizerScreen(
    audioSessionId: Int,
    onBack: () -> Unit,
) {
    val equalizer = remember(audioSessionId) {
        if (audioSessionId == 0) {
            null
        } else {
            runCatching { Equalizer(EFFECT_PRIORITY, audioSessionId) }.getOrNull()
        }
    }

    DisposableEffect(equalizer) {
        onDispose { equalizer?.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
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
                text = stringResource(R.string.equalizer),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (equalizer == null) {
            NoEqualizer(audioSessionId)
            return@Column
        }

        EqualizerControls(equalizer)
    }
}

/**
 * Algunos fabricantes (Samsung entre ellos) no dejan que una app de terceros cree
 * efectos sobre su sesion, pero si exponen su propio panel. En vez de dejar la pantalla
 * muerta, se ofrece abrirlo.
 */
@Composable
private fun NoEqualizer(audioSessionId: Int) {
    val context = LocalContext.current
    val intent = remember(audioSessionId) {
        Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
    }
    val haySistema = remember(intent) {
        intent.resolveActivity(context.packageManager) != null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                if (haySistema) R.string.eq_unavailable else R.string.eq_no_system,
            ),
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (haySistema) {
            Button(
                onClick = { runCatching { context.startActivity(intent) } },
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text(stringResource(R.string.eq_open_system))
            }
        }
    }
}

@Composable
private fun EqualizerControls(equalizer: Equalizer) {
    val bandas = remember(equalizer) { equalizer.numberOfBands.toInt() }
    val rango = remember(equalizer) { equalizer.bandLevelRange }
    val minNivel = rango[0].toFloat()
    val maxNivel = rango[1].toFloat()

    var activado by remember { mutableStateOf(equalizer.enabled) }
    val niveles = remember(equalizer) {
        mutableStateListOf<Float>().apply {
            repeat(bandas) { add(equalizer.getBandLevel(it.toShort()).toFloat()) }
        }
    }
    val presets = remember(equalizer) {
        (0 until equalizer.numberOfPresets.toInt()).map { equalizer.getPresetName(it.toShort()) }
    }
    var presetActual by remember { mutableStateOf(-1) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.eq_enable),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = activado,
            onCheckedChange = {
                activado = it
                runCatching { equalizer.enabled = it }
            },
        )
    }

    if (presets.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEachIndexed { indice, nombre ->
                AssistChip(
                    onClick = {
                        runCatching {
                            equalizer.usePreset(indice.toShort())
                            presetActual = indice
                            repeat(bandas) { banda ->
                                niveles[banda] = equalizer.getBandLevel(banda.toShort()).toFloat()
                            }
                        }
                    },
                    label = { Text(nombre) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (indice == presetActual) {
                            Amber
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                    ),
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(bandas) { banda ->
            val frecuencia = remember(equalizer, banda) {
                equalizer.getCenterFreq(banda.toShort()) / 1000
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(56.dp),
            ) {
                // Slider vertical: Compose no trae uno, se rota el horizontal.
                Slider(
                    value = niveles[banda],
                    valueRange = minNivel..maxNivel,
                    onValueChange = { valor ->
                        niveles[banda] = valor
                        presetActual = -1
                        runCatching {
                            equalizer.setBandLevel(banda.toShort(), valor.toInt().toShort())
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Amber,
                        activeTrackColor = Amber,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier
                        .height(220.dp)
                        .graphicsLayer { rotationZ = 270f }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = constraints.minHeight,
                                    maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth,
                                    maxHeight = constraints.maxWidth,
                                ),
                            )
                            layout(placeable.height, placeable.width) {
                                placeable.place(-placeable.width / 2 + placeable.height / 2, 0)
                            }
                        },
                )
                Text(
                    text = formatFrequency(frecuencia),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

private fun formatFrequency(hz: Int): String =
    if (hz >= 1000) "${hz / 1000}k" else "$hz"

/** 0 = prioridad normal; no desplaza a efectos de otras apps. */
private const val EFFECT_PRIORITY = 0
