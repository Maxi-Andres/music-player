package com.max.musicplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.max.musicplayer.data.DirectoryTree
import com.max.musicplayer.data.Song
import com.max.musicplayer.ui.components.MiniPlayer
import com.max.musicplayer.ui.components.SongMenuSheet
import com.max.musicplayer.ui.screens.DirectoryScreen
import com.max.musicplayer.ui.screens.EqualizerScreen
import com.max.musicplayer.ui.screens.FolderDetailScreen
import com.max.musicplayer.ui.screens.LibraryScreen
import com.max.musicplayer.ui.screens.NowPlayingScreen
import com.max.musicplayer.ui.screens.QueueScreen
import com.max.musicplayer.ui.screens.SettingsScreen

/** Pantalla actual. Navegacion por estado: el arbol es chico y asi es mas facil de seguir. */
private sealed interface Destino {
    data object Library : Destino
    data class Folder(val path: String) : Destino
    data class Directory(val path: String) : Destino
    data object NowPlaying : Destino
    data object Queue : Destino
    data object Equalizer : Destino
    data object Settings : Destino
}

@Composable
fun MusicApp(
    vm: MusicViewModel = viewModel(),
    settingsVm: SettingsViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.start() }

    // Pila de navegacion: sin ella, volver desde "Reproduciendo" siempre caia en la
    // biblioteca aunque hubieras entrado desde una carpeta.
    val pila = remember { mutableStateListOf<Destino>(Destino.Library) }
    val destino = pila.last()
    var cancionDelMenu by remember { mutableStateOf<Song?>(null) }

    fun navegar(nuevo: Destino) {
        if (pila.last() != nuevo) pila.add(nuevo)
    }

    // Viven aca y no dentro de LibraryScreen: esta funcion no se recompone al navegar,
    // asi que al volver de una carpeta las listas siguen donde estaban.
    val songsListState = rememberLazyListState()
    val foldersListState = rememberLazyListState()

    val allSongs by vm.allSongs.collectAsStateWithLifecycle()
    val visibleSongs by vm.visibleSongs.collectAsStateWithLifecycle()
    val visibleFolders by vm.visibleFolders.collectAsStateWithLifecycle()
    val totalSongs by vm.totalSongs.collectAsStateWithLifecycle()
    val isScanning by vm.isScanning.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val songSort by vm.songSort.collectAsStateWithLifecycle()
    val folderSort by vm.folderSort.collectAsStateWithLifecycle()
    val folderSongSort by vm.folderSongSort.collectAsStateWithLifecycle()
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    // Con .value la composicion no queda suscripta: hay que recolectar el flujo para
    // que un cambio de personalizacion se vea sin tener que navegar a otra pantalla.
    val ajustes by settingsVm.settings.collectAsStateWithLifecycle()
    val playback by vm.player.state.collectAsStateWithLifecycle()

    val cancionActual: Song? = queue.current?.song

    val bottomBar: @Composable () -> Unit = {
        cancionActual?.let { song ->
            MiniPlayer(
                song = song,
                isPlaying = playback.isPlaying,
                onPlayPause = { vm.player.togglePlayPause() },
                onQueueClick = { navegar(Destino.Queue) },
                onExpand = { navegar(Destino.NowPlaying) },
            )
        }
    }

    fun volverAtras() {
        val actual = pila.last()
        // Dentro del navegador de archivos, "atras" sube un nivel antes de salir.
        if (actual is Destino.Directory) {
            val padre = DirectoryTree.parentOf(actual.path)
            val raiz = vm.directoryRoot.value
            if (padre != null && actual.path != raiz) {
                pila[pila.lastIndex] = Destino.Directory(padre)
                return
            }
        }
        if (pila.size > 1) pila.removeAt(pila.lastIndex)
    }

    BackHandler(enabled = pila.size > 1) { volverAtras() }

    when (val d = destino) {
        Destino.Library -> LibraryScreen(
            songs = visibleSongs,
            folders = visibleFolders,
            totalSongs = totalSongs,
            isScanning = isScanning,
            selectedTab = tab,
            query = query,
            songSort = songSort,
            folderSort = folderSort,
            songsListState = songsListState,
            foldersListState = foldersListState,
            onTabSelected = vm::selectTab,
            onQueryChange = vm::setQuery,
            onSongSortChange = vm::setSongSort,
            onFolderSortChange = vm::setFolderSort,
            onSongClick = { indice -> vm.play(visibleSongs, indice) },
            onSongMenu = { cancionDelMenu = it },
            onFolderClick = { path -> navegar(Destino.Folder(path)) },
            onDirectoriesClick = { navegar(Destino.Directory(vm.directoryRoot.value)) },
            onOpenSettings = { navegar(Destino.Settings) },
            onShuffleAll = { vm.shufflePlay(visibleSongs) },
            onPlayAll = { vm.play(visibleSongs, 0) },
            bottomBar = bottomBar,
        )

        is Destino.Folder -> {
            val delFolder = remember(allSongs, d.path, folderSongSort) {
                vm.songsInFolder(allSongs, d.path)
            }
            FolderDetailScreen(
                folderName = d.path.substringAfterLast('/'),
                songs = delFolder,
                sort = folderSongSort,
                onSortChange = vm::setFolderSongSort,
                onBack = { volverAtras() },
                onSongClick = { indice -> vm.play(delFolder, indice) },
                onSongMenu = { cancionDelMenu = it },
                onShuffle = { vm.shufflePlay(delFolder) },
                onPlay = { vm.play(delFolder, 0) },
                bottomBar = bottomBar,
            )
        }

        is Destino.Directory -> {
            val subdirs = remember(allSongs, d.path) { vm.subdirectoriesOf(allSongs, d.path) }
            val aqui = remember(allSongs, d.path, folderSongSort) {
                vm.songsDirectlyIn(allSongs, d.path)
            }
            DirectoryScreen(
                path = d.path,
                subdirectories = subdirs,
                songsHere = aqui,
                onBack = { volverAtras() },
                onDirectoryClick = { path -> navegar(Destino.Directory(path)) },
                onSongClick = { indice -> vm.play(aqui, indice) },
                onSongMenu = { cancionDelMenu = it },
                bottomBar = bottomBar,
            )
        }

        Destino.NowPlaying -> {
            val song = cancionActual
            if (song == null) {
                LaunchedEffect(Unit) { volverAtras() }
            } else {
                NowPlayingScreen(
                    song = song,
                    isPlaying = playback.isPlaying,
                    positionMs = playback.positionMs,
                    durationMs = playback.durationMs,
                    shuffleEnabled = playback.shuffleEnabled,
                    repeatMode = playback.repeatMode,
                    contextEntries = queue.contextEntries,
                    currentContextIndex = queue.currentContextIndex,
                    queuedCount = queue.pendingEphemeral.size,
                    onCollapse = { volverAtras() },
                    onPlayPause = { vm.player.togglePlayPause() },
                    onPrevious = { vm.player.previous() },
                    onNext = { vm.player.next() },
                    onSeek = { vm.player.seekTo(it) },
                    onSeekBy = { vm.player.seekBy(it) },
                    onToggleShuffle = { vm.player.toggleShuffle() },
                    onCycleRepeat = { vm.player.cycleRepeatMode() },
                    onOpenQueue = { navegar(Destino.Queue) },
                    onOpenEqualizer = { navegar(Destino.Equalizer) },
                    tintFromArtwork = ajustes.tintFromArtwork,
                    onContextItemClick = { entrada ->
                        val indice = queue.entries.indexOfFirst { it.uid == entrada.uid }
                        if (indice >= 0) vm.player.playQueueIndex(indice)
                    },
                )
            }
        }

        Destino.Queue -> QueueScreen(
            queued = queue.pendingEphemeral,
            baseIndex = queue.ephemeralBaseIndex,
            onClose = { volverAtras() },
            onPlayIndex = { vm.player.playQueueIndex(it) },
            onRemove = { vm.player.removeFromQueue(it) },
            onMove = { desde, hasta -> vm.player.moveInQueue(desde, hasta) },
            onClearAll = { vm.player.clearEphemeral() },
        )

        Destino.Settings -> {
            SettingsScreen(
                settings = ajustes,
                onBack = { volverAtras() },
                onAccentColor = settingsVm::setAccentColor,
                onBackgroundColor = settingsVm::setBackgroundColor,
                onFolderColor = settingsVm::setFolderColor,
                onBackgroundImage = settingsVm::setBackgroundImage,
                onBackgroundDim = settingsVm::setBackgroundDim,
                onTintFromArtwork = settingsVm::setTintFromArtwork,
                onReset = { settingsVm.resetAll() },
            )
        }

        Destino.Equalizer -> EqualizerScreen(
            audioSessionId = playback.audioSessionId,
            onBack = { volverAtras() },
        )
    }

    cancionDelMenu?.let { song ->
        SongMenuSheet(
            song = song,
            onDismiss = { cancionDelMenu = null },
            onPlayNext = {
                vm.player.playNext(song)
                cancionDelMenu = null
            },
            onAddToQueue = {
                vm.player.addToQueue(song)
                cancionDelMenu = null
            },
            onGoToFolder = {
                navegar(Destino.Folder(song.folderPath))
                cancionDelMenu = null
            },
        )
    }
}
