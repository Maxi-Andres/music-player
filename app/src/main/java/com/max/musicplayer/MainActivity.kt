package com.max.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.max.musicplayer.ui.MusicApp
import com.max.musicplayer.ui.PermissionGate
import com.max.musicplayer.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MusicPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionGate {
                        MusicApp()
                    }
                }
            }
        }
    }
}
