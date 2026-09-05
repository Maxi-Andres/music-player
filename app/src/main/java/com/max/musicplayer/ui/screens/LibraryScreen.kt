package com.max.musicplayer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.max.musicplayer.R
import com.max.musicplayer.data.FolderSort
import com.max.musicplayer.data.MusicLibrary
import com.max.musicplayer.data.Song
import com.max.musicplayer.data.SongSort
import com.max.musicplayer.ui.LibraryTab
import com.max.musicplayer.ui.components.AlphabetIndex
import com.max.musicplayer.ui.components.DirectoriesRow
import com.max.musicplayer.ui.components.FolderRow
import com.max.musicplayer.ui.components.ListHeader
import com.max.musicplayer.ui.components.PlayActionPills
import com.max.musicplayer.ui.components.SongRow
import com.max.musicplayer.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** Solo las pestanias que estan implementadas; no se muestran tabs sin contenido. */
private val TABS = listOf(LibraryTab.SONGS, LibraryTab.FOLDERS)

private fun tabLabel(tab: LibraryTab) = when (tab) {
    LibraryTab.SONGS -> R.string.tab_songs
    LibraryTab.FOLDERS -> R.string.tab_folders
    LibraryTab.PLAYLISTS -> R.string.tab_playlists
    LibraryTab.ALBUMS -> R.string.tab_albums
    LibraryTab.ARTISTS -> R.string.tab_artists
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    folders: List<com.max.musicplayer.data.MusicFolder>,
    totalSongs: Int,
    isScanning: Boolean,
    selectedTab: LibraryTab,
    query: String,
    songSort: SongSort,
    folderSort: FolderSort,
    onTabSelected: (LibraryTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onSongSortChange: (SongSort) -> Unit,
    onFolderSortChange: (FolderSort) -> Unit,
    onSongClick: (Int) -> Unit,
    onSongMenu: (Song) -> Unit,
    onFolderClick: (String) -> Unit,
    onDirectoriesClick: () -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var buscando by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Barra de acciones propia en vez de TopAppBar: la de Material mide 64dp
            // y aca no hay titulo que poner, era todo espacio vacio.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (buscando) {
                    SearchField(
                        query = query,
                        onQueryChange = onQueryChange,
                        onClose = {
                            buscando = false
                            onQueryChange("")
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { buscando = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.action_search),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = TABS.indexOf(selectedTab).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                TABS.forEach { tab ->
                    val seleccionada = tab == selectedTab
                    Tab(
                        selected = seleccionada,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = stringResource(tabLabel(tab)),
                                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
                                color = if (seleccionada) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    TextSecondary
                                },
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                LibraryTab.FOLDERS -> FoldersTab(
                    folders = folders,
                    folderSort = folderSort,
                    onFolderSortChange = onFolderSortChange,
                    onFolderClick = onFolderClick,
                    onDirectoriesClick = onDirectoriesClick,
                )

                else -> SongsTab(
                    songs = songs,
                    totalSongs = totalSongs,
                    isScanning = isScanning,
                    songSort = songSort,
                    onSongSortChange = onSongSortChange,
                    onSongClick = onSongClick,
                    onSongMenu = onSongMenu,
                    onShuffleAll = onShuffleAll,
                    onPlayAll = onPlayAll,
                )
            }
        }
    }
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    totalSongs: Int,
    isScanning: Boolean,
    songSort: SongSort,
    onSongSortChange: (SongSort) -> Unit,
    onSongClick: (Int) -> Unit,
    onSongMenu: (Song) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit,
) {
    var menuOrden by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val letras = remember(songs) { MusicLibrary.alphabetIndex(songs.map { it.title }) }
    val etiquetas = remember(songs) { songs.map { it.title } }

    Column(modifier = Modifier.fillMaxSize()) {
        Box {
            ListHeader(
                title = pluralStringResource(
                    R.plurals.song_count_title,
                    totalSongs,
                    totalSongs,
                ),
                subtitle = if (isScanning) stringResource(R.string.scanning) else null,
                onSortClick = { menuOrden = true },
                onSelectClick = {},
            )
            SongSortMenu(
                expanded = menuOrden,
                current = songSort,
                onDismiss = { menuOrden = false },
                onSelect = {
                    onSongSortChange(it)
                    menuOrden = false
                },
            )
        }

        PlayActionPills(onShuffleClick = onShuffleAll, onPlayClick = onPlayAll)

        // El indice A-Z se superpone solo a la lista, para no tapar los botones de arriba.
        Box(modifier = Modifier.weight(1f)) {
            if (songs.isEmpty() && !isScanning) {
                EmptyMessage(stringResource(R.string.empty_library))
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(songs.size, key = { songs[it].id }) { indice ->
                        val song = songs[indice]
                        SongRow(
                            song = song,
                            onClick = { onSongClick(indice) },
                            onMenuClick = { onSongMenu(song) },
                        )
                    }
                }
            }

            AlphabetIndex(
                letters = letras,
                onLetterSelected = { letra ->
                    val destino = MusicLibrary.firstIndexForLetter(etiquetas, letra)
                    if (destino >= 0) scope.launch { listState.scrollToItem(destino) }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(vertical = 8.dp, horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun FoldersTab(
    folders: List<com.max.musicplayer.data.MusicFolder>,
    folderSort: FolderSort,
    onFolderSortChange: (FolderSort) -> Unit,
    onFolderClick: (String) -> Unit,
    onDirectoriesClick: () -> Unit,
) {
    var menuOrden by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val letras = remember(folders) { MusicLibrary.alphabetIndex(folders.map { it.name }) }
    val etiquetas = remember(folders) { folders.map { it.name } }

    Column(modifier = Modifier.fillMaxSize()) {
        Box {
            ListHeader(
                title = pluralStringResource(
                    R.plurals.folder_count,
                    folders.size,
                    folders.size,
                ),
                onSortClick = { menuOrden = true },
                onSelectClick = {},
            )
            FolderSortMenu(
                expanded = menuOrden,
                current = folderSort,
                onDismiss = { menuOrden = false },
                onSelect = {
                    onFolderSortChange(it)
                    menuOrden = false
                },
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item { DirectoriesRow(onClick = onDirectoriesClick) }
                items(folders, key = { it.path }) { carpeta ->
                    FolderRow(
                        name = carpeta.name,
                        songCount = carpeta.songCount,
                        onClick = { onFolderClick(carpeta.path) },
                        onMenuClick = {},
                    )
                }
            }

            AlphabetIndex(
                letters = letras,
                onLetterSelected = { letra ->
                    val destino = MusicLibrary.firstIndexForLetter(etiquetas, letra)
                    // +1 porque la fila "Directorios" ocupa la posicion 0.
                    if (destino >= 0) scope.launch { listState.scrollToItem(destino + 1) }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(vertical = 8.dp, horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = TextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.action_search), color = TextSecondary) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SongSortMenu(
    expanded: Boolean,
    current: SongSort,
    onDismiss: () -> Unit,
    onSelect: (SongSort) -> Unit,
) {
    val etiquetas = mapOf(
        SongSort.DATE_ADDED_DESC to "Mas recientes primero",
        SongSort.DATE_ADDED_ASC to "Mas antiguas primero",
        SongSort.TITLE_ASC to "Titulo A-Z",
        SongSort.TITLE_DESC to "Titulo Z-A",
        SongSort.ARTIST_ASC to "Artista A-Z",
        SongSort.DURATION_DESC to "Mas largas primero",
        SongSort.DURATION_ASC to "Mas cortas primero",
    )
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        etiquetas.forEach { (orden, texto) ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = texto,
                        fontWeight = if (orden == current) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = { onSelect(orden) },
            )
        }
    }
}

@Composable
private fun FolderSortMenu(
    expanded: Boolean,
    current: FolderSort,
    onDismiss: () -> Unit,
    onSelect: (FolderSort) -> Unit,
) {
    val etiquetas = mapOf(
        FolderSort.NAME_ASC to "Nombre A-Z",
        FolderSort.NAME_DESC to "Nombre Z-A",
        FolderSort.SONG_COUNT_DESC to "Mas canciones primero",
        FolderSort.SONG_COUNT_ASC to "Menos canciones primero",
    )
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        etiquetas.forEach { (orden, texto) ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = texto,
                        fontWeight = if (orden == current) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = { onSelect(orden) },
            )
        }
    }
}
