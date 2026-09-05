package com.max.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Alto de cada letra. Sale de medir la barra en docs/reference/02-tab-carpetas.jpeg. */
private val LETTER_HEIGHT = 13.dp
private val BAR_WIDTH = 22.dp

/**
 * Barra de indice alfabetico del costado derecho.
 *
 * Se ajusta a la cantidad de letras presentes en vez de estirarse a toda la altura:
 * en la referencia, la carpeta "acdc" muestra una barra corta con solo A B F H M S T Y.
 *
 * Solo tiene sentido sobre una lista **ordenada alfabeticamente**; quien la usa se
 * encarga de no mostrarla cuando el orden es otro (por fecha, por duracion, etc.),
 * porque si no "la primera que empieza con B" cae en cualquier lado.
 */
@Composable
fun AlphabetIndex(
    letters: List<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.size < 2) return

    val haptics = LocalHapticFeedback.current
    var alturaPx by remember { mutableIntStateOf(0) }
    var ultimoIndice by remember { mutableIntStateOf(-1) }

    fun seleccionarEn(y: Float) {
        if (alturaPx <= 0) return
        val indice = ((y / alturaPx) * letters.size)
            .toInt()
            .coerceIn(0, letters.lastIndex)
        if (indice != ultimoIndice) {
            ultimoIndice = indice
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onLetterSelected(letters[indice])
        }
    }

    Column(
        modifier = modifier
            .width(BAR_WIDTH)
            .wrapContentHeight()
            .clip(RoundedCornerShape(BAR_WIDTH / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            .padding(vertical = 6.dp)
            .onSizeChanged { alturaPx = it.height }
            // Tocar una letra: sin esto solo funcionaba arrastrando.
            .pointerInput(letters) {
                detectTapGestures { offset ->
                    ultimoIndice = -1
                    seleccionarEn(offset.y)
                }
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> seleccionarEn(offset.y) },
                    onDragEnd = { ultimoIndice = -1 },
                    onDragCancel = { ultimoIndice = -1 },
                    onVerticalDrag = { change, _ -> seleccionarEn(change.position.y) },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letra ->
            Box(
                modifier = Modifier.height(LETTER_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letra,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
