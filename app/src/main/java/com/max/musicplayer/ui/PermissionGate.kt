package com.max.musicplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.max.musicplayer.R

/** Permiso de lectura de audio segun la version de Android. */
val audioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/**
 * Muestra [content] solo cuando hay permiso para leer el audio del telefono.
 * Si no lo hay, lo pide; y si el usuario lo denego para siempre, ofrece ir a Ajustes.
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    fun concedido() = ContextCompat.checkSelfPermission(context, audioPermission) ==
        PackageManager.PERMISSION_GRANTED

    var tienePermiso by remember { mutableStateOf(concedido()) }
    var yaPregunto by remember { mutableStateOf(false) }

    val solicitar = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedidoAhora ->
        tienePermiso = concedidoAhora
        yaPregunto = true
    }

    // En Android 13+ la notificacion del reproductor tambien necesita permiso.
    val solicitarNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        if (!tienePermiso) solicitar.launch(audioPermission)

        // Solo se pide si realmente falta: pedirlo en cada arranque le tira al usuario
        // un dialogo encima cada vez que abre la app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificacionesConcedidas = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!notificacionesConcedidas) {
                solicitarNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (tienePermiso) {
        content()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (yaPregunto) R.string.permission_denied_body else R.string.permission_body,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
        if (yaPregunto) {
            Button(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            }) {
                Text(stringResource(R.string.permission_settings))
            }
        } else {
            Button(onClick = { solicitar.launch(audioPermission) }) {
                Text(stringResource(R.string.permission_grant))
            }
        }
    }
}
