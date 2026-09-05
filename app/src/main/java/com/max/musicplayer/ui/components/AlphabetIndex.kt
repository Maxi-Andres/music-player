package com.max.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Barra de indice A-Z del costado derecho (docs/reference/02-tab-carpetas.jpeg).
 *
 * Se puede tocar una letra o arrastrar el dedo por la barra; en cada cambio de letra
 * avisa con [onLetterSelected] y da un pulso haptico, como los indices del sistema.
 */
@Composable
fun AlphabetIndex(
    letters: List<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    var alturaPx by remember { mutableIntStateOf(0) }
    var ultimaLetra by remember { mutableIntStateOf(-1) }

    fun seleccionarPorPosicion(y: Float) {
        if (alturaPx <= 0) return
        val indice = ((y / alturaPx) * letters.size)
            .toInt()
            .coerceIn(0, letters.lastIndex)
        if (indice != ultimaLetra) {
            ultimaLetra = indice
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onLetterSelected(letters[indice])
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
            .onSizeChanged { alturaPx = it.height }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> seleccionarPorPosicion(offset.y) },
                    onDragEnd = { ultimaLetra = -1 },
                    onVerticalDrag = { change, _ -> seleccionarPorPosicion(change.position.y) },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        letters.forEach { letra ->
            Text(
                text = letra,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }
    }
}
