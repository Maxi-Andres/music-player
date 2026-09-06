package com.max.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.max.musicplayer.ui.theme.LocalGlassEnabled

/** Grosor del canto iluminado. Mas que esto ya parece un marco dibujado. */
private val GLASS_EDGE = 1.dp

/**
 * Superficie de vidrio.
 *
 * En vez de un color liso, un relleno translucido con un degrade y el canto iluminado
 * arriba, que es lo que hace que algo "se lea" como vidrio. Al ser translucido deja
 * pasar lo que hay atras: sobre una imagen de fondo se ve la foto a traves del boton.
 *
 * El brillo es blanco sobre temas oscuros y negro sobre los claros, asi que funciona
 * con cualquiera de los fondos que se pueden elegir en Personalizacion.
 *
 * NO lleva desenfoque de lo que hay detras. Eso necesita Android 12 en adelante
 * (RenderEffect) y grabar el fondo en una capa aparte para volver a dibujarlo borroso
 * dentro de cada elemento; con minSdk 26 haria falta ademas este mismo camino como
 * respaldo. Sobre un fondo liso o un degrade, desenfocar no cambia nada de lo que se ve:
 * recien se nota con una imagen de fondo puesta.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    withEdge: Boolean = true,
    fallback: Color = MaterialTheme.colorScheme.surfaceVariant,
): Modifier {
    // Apagado desde Personalizacion: color liso, exactamente como se veia antes.
    if (!LocalGlassEnabled.current) return this.clip(shape).background(fallback)

    val temaClaro = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val brillo = if (temaClaro) Color.Black else Color.White

    val relleno = Brush.verticalGradient(
        listOf(brillo.copy(alpha = 0.18f), brillo.copy(alpha = 0.07f)),
    )
    val canto = Brush.verticalGradient(
        listOf(brillo.copy(alpha = 0.38f), brillo.copy(alpha = 0.08f)),
    )

    val conRelleno = this.clip(shape).background(relleno)
    return if (withEdge) conRelleno.border(GLASS_EDGE, canto, shape) else conRelleno
}
