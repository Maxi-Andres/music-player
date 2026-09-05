package com.max.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.delay

/** Alto de cada letra en la barra. */
private val LETTER_HEIGHT = 18.dp
private val BAR_WIDTH = 24.dp
private val BAR_PADDING = 8.dp

/** Globo que muestra en grande la letra que se esta tocando. */
private val BUBBLE_SIZE = 64.dp
private val BUBBLE_GAP = 10.dp

/** Cuanto queda visible el globo despues de soltar. */
private const val BUBBLE_LINGER_MS = 650L

/**
 * Barra de indice alfabetico del costado derecho.
 *
 * Se ajusta a la cantidad de letras presentes en vez de estirarse a toda la altura, y
 * mientras se toca resalta la letra activa y muestra un globo con esa letra en grande
 * (ver docs/reference/0c63a2cf-b1b3-498d-9c39-ce0fb3291bb6.jpg).
 *
 * Solo tiene sentido sobre una lista **ordenada alfabeticamente**; quien la usa se
 * encarga de no mostrarla con otro orden, porque si no "la primera con B" cae en
 * cualquier lado.
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
    var indiceActivo by remember { mutableIntStateOf(-1) }
    var tocando by remember { mutableStateOf(false) }
    var globoVisible by remember { mutableStateOf(false) }

    // El globo sobrevive un instante al soltar: si se ocultara de golpe, en un toque
    // corto no llegarias a verlo.
    LaunchedEffect(indiceActivo, tocando) {
        if (indiceActivo >= 0) {
            globoVisible = true
            if (!tocando) {
                delay(BUBBLE_LINGER_MS)
                globoVisible = false
                indiceActivo = -1
            }
        }
    }

    fun seleccionarEn(y: Float) {
        if (alturaPx <= 0) return
        val indice = ((y / alturaPx) * letters.size)
            .toInt()
            .coerceIn(0, letters.lastIndex)
        if (indice != indiceActivo) {
            indiceActivo = indice
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onLetterSelected(letters[indice])
        }
    }

    BoxWithConstraints(modifier = modifier.wrapContentHeight()) {
        // Con 27 letras a 18dp la barra mide ~500dp y en pantallas chicas no entra.
        // Se achica solo hasta lo que haya disponible en vez de cortarse.
        val alturaLetra = minOf(
            LETTER_HEIGHT,
            (maxHeight - BAR_PADDING * 2) / letters.size,
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(BAR_WIDTH)
                .wrapContentHeight()
                .clip(RoundedCornerShape(BAR_WIDTH / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .padding(vertical = BAR_PADDING)
                .onSizeChanged { alturaPx = it.height }
                .pointerInput(letters) {
                    detectTapGestures(
                        onPress = { offset ->
                            tocando = true
                            seleccionarEn(offset.y)
                            tryAwaitRelease()
                            tocando = false
                        },
                    )
                }
                .pointerInput(letters) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            tocando = true
                            seleccionarEn(offset.y)
                        },
                        onDragEnd = { tocando = false },
                        onDragCancel = { tocando = false },
                        onVerticalDrag = { change, _ -> seleccionarEn(change.position.y) },
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEachIndexed { indice, letra ->
                val activa = indice == indiceActivo && globoVisible
                Box(
                    modifier = Modifier.height(alturaLetra),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letra,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (activa) FontWeight.Bold else FontWeight.Medium,
                        color = if (activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        if (globoVisible && indiceActivo >= 0) {
            LetterBubble(
                letter = letters[indiceActivo],
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(
                        x = -(BAR_WIDTH + BUBBLE_GAP),
                        // Centrado sobre la letra tocada: se mide desde el centro de la
                        // barra, por eso el desplazamiento va respecto del medio.
                        y = alturaLetra * (indiceActivo - (letters.size - 1) / 2f),
                    ),
            )
        }
    }
}

@Composable
private fun LetterBubble(letter: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BUBBLE_SIZE)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
