package com.max.musicplayer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.max.musicplayer.R
import com.max.musicplayer.data.MusicLibrary
import com.max.musicplayer.data.Song
import com.max.musicplayer.data.SongSort
import com.max.musicplayer.ui.components.AlphabetIndex
import com.max.musicplayer.ui.components.ListHeader
import com.max.musicplayer.ui.components.PlayActionPills
import com.max.musicplayer.ui.components.SongRow
import com.max.musicplayer.ui.theme.FolderAmber
import kotlinx.coroutines.launch

/** Canciones de una carpeta. Ver docs/reference/03-detalle-carpeta.jpeg. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderName: String,
    songs: List<Song>,
    sort: SongSort,
    onSortChange: (SongSort) -> Unit,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit,
    onSongMenu: (Song) -> Unit,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var menuOrden by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val letras = remember(songs) { MusicLibrary.alphabetIndex(songs.map { it.title }) }
    val etiquetas = remember(songs) { songs.map { it.title } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = { Text("") },
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
        Box(modifier = Modifier.padding(padding)) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = FolderAmber,
                        modifier = Modifier.size(56.dp),
                    )
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }

                Box {
                    ListHeader(
                        title = pluralStringResource(
                            R.plurals.song_count_title,
                            songs.size,
                            songs.size,
                        ),
                        onSortClick = { menuOrden = true },
                        onSelectClick = {},
                    )
                    SongSortMenu(
                        expanded = menuOrden,
                        current = sort,
                        onDismiss = { menuOrden = false },
                        onSelect = {
                            onSortChange(it)
                            menuOrden = false
                        },
                    )
                }

                // Dentro de una carpeta los botones son blancos rellenos, como la referencia.
                PlayActionPills(
                    onShuffleClick = onShuffle,
                    onPlayClick = onPlay,
                    filled = true,
                )

                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(songs.size, key = { songs[it].id }) { indice ->
                        val song = songs[indice]
                        SongRow(
                            song = song,
                            onClick = { onSongClick(indice) },
                            onMenuClick = { onSongMenu(song) },
                            showDate = false,
                        )
                    }
                }
            }

            if (sort.isAlphabetical) {
                AlphabetIndex(
                    letters = letras,
                    onLetterSelected = { letra ->
                        val destino = MusicLibrary.firstIndexForLetter(etiquetas, letra)
                        if (destino >= 0) scope.launch { listState.scrollToItem(destino) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(horizontal = 4.dp),
                )
            }
        }
    }
}
