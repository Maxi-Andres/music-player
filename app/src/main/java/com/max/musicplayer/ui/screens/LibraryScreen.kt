package com.max.musicplayer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.max.musicplayer.R
import com.max.musicplayer.data.FolderSort
import com.max.musicplayer.data.MusicFolder
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

/** Ancho que hay que reservar a la derecha para la barra de indice superpuesta. */
private val INDEX_BAR_SPACE = 30.dp

/** Solo las pestanias que estan implementadas; no se muestran tabs sin contenido. */
private val TABS = listOf(LibraryTab.SONGS, LibraryTab.FOLDERS)

private fun tabLabel(tab: LibraryTab) = when (tab) {
    LibraryTab.SONGS -> R.string.tab_songs
    LibraryTab.FOLDERS -> R.string.tab_folders
    LibraryTab.PLAYLISTS -> R.string.tab_playlists
    LibraryTab.ALBUMS -> R.string.tab_albums
    LibraryTab.ARTISTS -> R.string.tab_artists
}

/**
 * Pantalla principal, con las pestanias Canciones y Carpetas deslizables.
 *
 * Los [LazyListState] llegan desde afuera a proposito: si se crearan aca, al entrar a
 * una carpeta y volver la pantalla se recompone de cero y la lista arrancaria arriba
 * de todo, perdiendo la posicion donde estabas.
 */
@Composable
fun LibraryScreen(
    songs: List<Song>,
    folders: List<MusicFolder>,
    totalSongs: Int,
    isScanning: Boolean,
    selectedTab: LibraryTab,
    query: String,
    songSort: SongSort,
    folderSort: FolderSort,
    songsListState: LazyListState,
    foldersListState: LazyListState,
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

    val pagerState = rememberPagerState(
        initialPage = TABS.indexOf(selectedTab).coerceAtLeast(0),
    ) { TABS.size }

    // Deslizar cambia la pestania...
    LaunchedEffect(pagerState.currentPage) {
        onTabSelected(TABS[pagerState.currentPage])
    }
    // ...y tocar la pestania desliza.
    LaunchedEffect(selectedTab) {
        val destino = TABS.indexOf(selectedTab)
        if (destino >= 0 && destino != pagerState.currentPage) {
            pagerState.animateScrollToPage(destino)
        }
    }

    // Al cambiar la busqueda hay que volver arriba: si no, quedas parado en el medio
    // de los resultados nuevos y parece que hubiera contenido cortado hacia arriba.
    var busquedaPrevia by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query != busquedaPrevia) {
            busquedaPrevia = query
            songsListState.scrollToItem(0)
            foldersListState.scrollToItem(0)
        }
    }

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
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                TABS.forEachIndexed { indice, tab ->
                    val seleccionada = indice == pagerState.currentPage
                    Tab(
                        selected = seleccionada,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = stringResource(tabLabel(tab)),
                                fontWeight = if (seleccionada) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
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

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                // Mantiene viva la pestania vecina para que su scroll no se reinicie.
                beyondViewportPageCount = 1,
            ) { pagina ->
                when (TABS[pagina]) {
                    LibraryTab.FOLDERS -> FoldersTab(
                        folders = folders,
                        folderSort = folderSort,
                        showDirectories = query.isBlank(),
                        listState = foldersListState,
                        onFolderSortChange = onFolderSortChange,
                        onFolderClick = onFolderClick,
                        onDirectoriesClick = onDirectoriesClick,
                    )

                    else -> SongsTab(
                        songs = songs,
                        totalSongs = totalSongs,
                        isScanning = isScanning,
                        songSort = songSort,
                        listState = songsListState,
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
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    totalSongs: Int,
    isScanning: Boolean,
    songSort: SongSort,
    listState: LazyListState,
    onSongSortChange: (SongSort) -> Unit,
    onSongClick: (Int) -> Unit,
    onSongMenu: (Song) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit,
) {
    var menuOrden by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val letras = remember(songs) { MusicLibrary.alphabetIndex(songs.map { it.title }) }
    val etiquetas = remember(songs) { songs.map { it.title } }

    // Si se toca una letra con la lista ordenada por fecha, primero hay que reordenar
    // alfabeticamente. Como la lista nueva llega en la recomposicion siguiente, la letra
    // queda pendiente y el salto se hace cuando el orden ya se aplico.
    var letraPendiente by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(etiquetas, letraPendiente) {
        val letra = letraPendiente ?: return@LaunchedEffect
        val destino = MusicLibrary.firstIndexForLetter(etiquetas, letra)
        if (destino >= 0) {
            listState.scrollToItem(destino)
            letraPendiente = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box {
            ListHeader(
                title = pluralStringResource(R.plurals.song_count_title, totalSongs, totalSongs),
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
                LazyColumn(
                    state = listState,
                    // Deja libre el ancho de la barra de letras, que va superpuesta:
                    // si no, tapa la fecha de cada cancion.
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = INDEX_BAR_SPACE),
                ) {
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
                    if (songSort.isAlphabetical) {
                        val destino = MusicLibrary.firstIndexForLetter(etiquetas, letra)
                        if (destino >= 0) scope.launch { listState.scrollToItem(destino) }
                    } else {
                        // Con orden por fecha la letra no ubica nada, asi que se pasa
                        // a orden alfabetico y recien ahi se salta.
                        onSongSortChange(SongSort.TITLE_ASC)
                        letraPendiente = letra
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun FoldersTab(
    folders: List<MusicFolder>,
    folderSort: FolderSort,
    showDirectories: Boolean,
    listState: LazyListState,
    onFolderSortChange: (FolderSort) -> Unit,
    onFolderClick: (String) -> Unit,
    onDirectoriesClick: () -> Unit,
) {
    var menuOrden by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val letras = remember(folders) { MusicLibrary.alphabetIndex(folders.map { it.name }) }
    val etiquetas = remember(folders) { folders.map { it.name } }

    // Mismo criterio que en Canciones: si el orden no es alfabetico, se reordena y el
    // salto queda pendiente hasta que la lista nueva este lista.
    var letraPendiente by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(etiquetas, letraPendiente) {
        val letra = letraPendiente ?: return@LaunchedEffect
        val destino = MusicLibrary.firstIndexForLetter(etiquetas, letra)
        if (destino >= 0) {
            listState.scrollToItem(destino + if (showDirectories) 1 else 0)
            letraPendiente = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box {
            ListHeader(
                title = pluralStringResource(R.plurals.folder_count, folders.size, folders.size),
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = INDEX_BAR_SPACE),
            ) {
                // Mientras se busca no se muestra: no es un resultado y descuadra
                // el conteo del encabezado.
                if (showDirectories) {
                    item { DirectoriesRow(onClick = onDirectoriesClick) }
                }
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
                    if (folderSort.isAlphabetical) {
                        val destino = MusicLibrary.firstIndexForLetter(etiquetas, letra)
                        // La fila "Directorios" ocupa la posicion 0 cuando esta visible.
                        val corrimiento = if (showDirectories) 1 else 0
                        if (destino >= 0) {
                            scope.launch { listState.scrollToItem(destino + corrimiento) }
                        }
                    } else {
                        onFolderSortChange(FolderSort.NAME_ASC)
                        letraPendiente = letra
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 4.dp),
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

/**
 * Campo de busqueda.
 *
 * Se usa [BasicTextField] y no el TextField de Material porque ese ultimo impone
 * 56dp de alto minimo y el texto quedaba cortado en la barra de acciones.
 * Ademas pide el foco al aparecer, para que el teclado se abra solo.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Row(
        modifier = modifier.padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.action_search),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                }
                inner()
            },
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
internal fun SongSortMenu(
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
