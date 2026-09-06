package com.max.musicplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.max.musicplayer.R

/**
 * Cabecera de lista: "N Canciones", el boton de ordenar y el de seleccion multiple.
 * Ver docs/reference/01-tab-canciones.jpeg.
 *
 * [onSelectClick] es opcional: donde no hay nada que seleccionar (la lista de carpetas)
 * el boton no se dibuja, en vez de quedar puesto sin hacer nada.
 */
@Composable
fun ListHeader(
    title: String,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectClick: (() -> Unit)? = null,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = " $subtitle",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = stringResource(R.string.action_sort),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (onSelectClick != null) {
                Text("|", color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onSelectClick) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = stringResource(R.string.action_select),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

/**
 * Cabecera que reemplaza a [ListHeader] mientras hay canciones marcadas.
 *
 * Ocupa el mismo lugar que la cabecera normal en vez de aparecer como una barra
 * flotante: asi la lista no se corre para abajo al entrar al modo seleccion.
 */
@Composable
fun SelectionHeader(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hayAlgo = selectedCount > 0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_cancel),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = pluralStringResource(R.plurals.selected_count, selectedCount, selectedCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSelectAll) {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = stringResource(R.string.action_select_all),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        // Sin nada marcado las acciones no harian nada: se apagan en vez de mentir.
        IconButton(onClick = onPlay, enabled = hayAlgo) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.action_play),
                tint = if (hayAlgo) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = onAddToQueue, enabled = hayAlgo) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = stringResource(R.string.menu_add_to_queue),
                tint = if (hayAlgo) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/**
 * Par de botones "Aleatorio" / "Reproducir".
 *
 * [filled] cambia entre las dos variantes de la referencia: oscuros con icono ambar
 * en la pestania de canciones, blancos rellenos dentro de una carpeta.
 */
@Composable
fun PlayActionPills(
    onShuffleClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val contenedor = if (filled) Color.White else MaterialTheme.colorScheme.surfaceVariant
    val contenido = if (filled) Color(0xFF101010) else MaterialTheme.colorScheme.onBackground
    val tinteIcono = if (filled) Color(0xFF101010) else MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Pill(
            text = stringResource(R.string.action_shuffle),
            icon = Icons.Default.Shuffle,
            container = contenedor,
            content = contenido,
            iconTint = tinteIcono,
            onClick = onShuffleClick,
            modifier = Modifier.weight(1f),
        )
        Pill(
            text = stringResource(R.string.action_play),
            icon = Icons.Default.PlayArrow,
            container = contenedor,
            content = contenido,
            iconTint = tinteIcono,
            onClick = onPlayClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Pill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    content: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** Icono de cola usado en el mini reproductor. */
@Composable
fun QueueIcon(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
            contentDescription = stringResource(R.string.cd_queue),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}
