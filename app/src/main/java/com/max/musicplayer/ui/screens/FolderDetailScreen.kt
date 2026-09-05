package com.max.musicplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.max.musicplayer.ui.theme.Amber
import com.max.musicplayer.ui.theme.FolderAmber
import kotlinx.coroutines.launch

/**
 * Canciones de una carpeta.
 *
 * La cabecera grande (icono, nombre, conteo y botones) es el primer item de la lista,
 * asi que se va hacia arriba al scrollear y quedan mas canciones a la vista; el nombre
 * reaparece en la barra de arriba, ya colapsada.
 * Ver docs/reference/03-detalle-carpeta.jpeg y la version colapsada en
 * docs/reference/WhatsApp Image 2026-09-05 at 14.15.22.jpeg.
 */
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var menuOrden by remember { mutableStateOf(false) }

    val letras = remember(songs) { MusicLibrary.alphabetIndex(songs.map { it.title }) }
    val etiquetas = remember(songs) { songs.map { it.title } }

    // La cabecera ocupa el item 0: si ya no se ve, esta colapsada.
    val colapsado by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CollapsedBar(
                title = folderName,
                showTitle = colapsado,
                onBack = onBack,
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item(key = "header") {
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

                        // Dentro de una carpeta los botones son blancos rellenos,
                        // como en la referencia.
                        PlayActionPills(
                            onShuffleClick = onShuffle,
                            onPlayClick = onPlay,
                            filled = true,
                        )
                    }
                }

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

            // Solo con la cabecera colapsada: arriba de todo taparia los botones
            // Aleatorio/Reproducir, y ademas ahi ya estas en la primera letra.
            AnimatedVisibility(
                visible = colapsado,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                AlphabetIndex(
                    letters = letras,
                    onLetterSelected = { letra ->
                        if (sort.isAlphabetical) {
                            val destino = MusicLibrary.firstIndexForLetter(etiquetas, letra)
                            // +1 por la cabecera, que es el item 0.
                            if (destino >= 0) {
                                scope.launch { listState.scrollToItem(destino + 1) }
                            }
                        } else {
                            onSortChange(SongSort.TITLE_ASC)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            AnimatedVisibility(
                visible = colapsado,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            ) {
                ScrollToTopButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                )
            }
        }
    }
}

/** Barra superior compacta. El titulo aparece cuando la cabecera grande se fue. */
@Composable
private fun CollapsedBar(
    title: String,
    showTitle: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Scaffold no aplica el inset de la barra de estado a un topBar propio
            // (el TopAppBar de Material lo hace solo), asi que hay que ponerlo aca:
            // si no, la flecha queda pisando el reloj.
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        AnimatedVisibility(visible = showTitle, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ScrollToTopButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardDoubleArrowUp,
            contentDescription = stringResource(R.string.action_scroll_top),
            tint = Amber,
            modifier = Modifier.size(28.dp),
        )
    }
}
