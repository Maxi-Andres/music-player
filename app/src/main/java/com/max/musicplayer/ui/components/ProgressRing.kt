package com.max.musicplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.max.musicplayer.playback.PlayerConnection

/** Cuanto de la vuelta completa marca el circulo apagado de atras. */
private const val RING_TRACK_ALPHA = 0.20f

/**
 * Anillo de progreso que rodea a [content].
 *
 * El avance se anima con la misma duracion con la que el reproductor informa la posicion,
 * asi se mueve parejo en vez de dar un salto cada medio segundo.
 *
 * El tamanio y el grosor llegan de afuera porque lo usan dos botones muy distintos: el
 * play grande de la pantalla de reproduccion y el chiquito de la barra de abajo.
 */
@Composable
fun AnilloDeProgreso(
    fraction: Float,
    color: Color,
    size: Dp,
    stroke: Dp,
    content: @Composable () -> Unit,
) {
    val avance by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = PlayerConnection.POSITION_POLL_MS.toInt(),
            easing = LinearEasing,
        ),
        label = "anilloDeProgreso",
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trazo = stroke.toPx()
            val esquina = Offset(trazo / 2, trazo / 2)
            val medida = Size(this.size.width - trazo, this.size.height - trazo)

            // El circulo completo apagado marca cuanto falta.
            drawArc(
                color = color.copy(alpha = RING_TRACK_ALPHA),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = esquina,
                size = medida,
                style = Stroke(width = trazo),
            )
            // Arranca arriba de todo, como un reloj.
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * avance,
                useCenter = false,
                topLeft = esquina,
                size = medida,
                style = Stroke(width = trazo, cap = StrokeCap.Round),
            )
        }
        content()
    }
}
