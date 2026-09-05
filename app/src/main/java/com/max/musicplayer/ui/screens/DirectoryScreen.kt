package com.max.musicplayer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.max.musicplayer.R
import com.max.musicplayer.data.DirectoryNode
import com.max.musicplayer.data.Song
import com.max.musicplayer.ui.components.FolderRow
import com.max.musicplayer.ui.components.SongRow
import com.max.musicplayer.ui.theme.TextSecondary

/**
 * Navegador de archivos nivel por nivel (la entrada "Directorios" de la
 * pestania Carpetas). Muestra subdirectorios arriba y las canciones sueltas
 * de este nivel abajo, como cualquier explorador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    path: String,
    subdirectories: List<DirectoryNode>,
    songsHere: List<Song>,
    onBack: () -> Unit,
    onDirectoryClick: (String) -> Unit,
    onSongClick: (Int) -> Unit,
    onSongMenu: (Song) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Column {
                        Text(
                            text = path.substringAfterLast('/').ifEmpty {
                                stringResource(R.string.directories)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(subdirectories, key = { it.path }) { nodo ->
                FolderRow(
                    name = nodo.name,
                    songCount = nodo.songCount,
                    onClick = { onDirectoryClick(nodo.path) },
                    onMenuClick = {},
                )
            }
            items(songsHere.size, key = { songsHere[it].id }) { indice ->
                val song = songsHere[indice]
                SongRow(
                    song = song,
                    onClick = { onSongClick(indice) },
                    onMenuClick = { onSongMenu(song) },
                    showDate = false,
                )
            }
            if (subdirectories.isEmpty() && songsHere.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.empty_library),
                        color = TextSecondary,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}
