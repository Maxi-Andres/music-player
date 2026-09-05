package com.max.musicplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.max.musicplayer.data.DirectoryTree
import com.max.musicplayer.data.Song
import com.max.musicplayer.ui.components.MiniPlayer
import com.max.musicplayer.ui.screens.DirectoryScreen
import com.max.musicplayer.ui.screens.FolderDetailScreen
import com.max.musicplayer.ui.screens.LibraryScreen

/** Pantalla actual. Navegacion por estado: el arbol es chico y asi es mas facil de seguir. */
private sealed interface Destino {
    data object Library : Destino
    data class Folder(val path: String) : Destino
    data class Directory(val path: String) : Destino
}

@Composable
fun MusicApp(vm: MusicViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.start() }

    var destino by remember { mutableStateOf<Destino>(Destino.Library) }

    val allSongs by vm.allSongs.collectAsStateWithLifecycle()
    val visibleSongs by vm.visibleSongs.collectAsStateWithLifecycle()
    val visibleFolders by vm.visibleFolders.collectAsStateWithLifecycle()
    val totalSongs by vm.totalSongs.collectAsStateWithLifecycle()
    val isScanning by vm.isScanning.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val songSort by vm.songSort.collectAsStateWithLifecycle()
    val folderSort by vm.folderSort.collectAsStateWithLifecycle()
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    val playback by vm.player.state.collectAsStateWithLifecycle()

    val cancionActual: Song? = queue.current?.song

    val bottomBar: @Composable () -> Unit = {
        cancionActual?.let { song ->
            MiniPlayer(
                song = song,
                isPlaying = playback.isPlaying,
                onPlayPause = { vm.player.togglePlayPause() },
                onQueueClick = {},
                onExpand = {},
            )
        }
    }

    BackHandler(enabled = destino != Destino.Library) {
        destino = when (val d = destino) {
            is Destino.Directory -> {
                val padre = DirectoryTree.parentOf(d.path)
                val raiz = vm.directoryRoot.value
                if (padre != null && d.path != raiz) Destino.Directory(padre) else Destino.Library
            }
            else -> Destino.Library
        }
    }

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
            onTabSelected = vm::selectTab,
            onQueryChange = vm::setQuery,
            onSongSortChange = vm::setSongSort,
            onFolderSortChange = vm::setFolderSort,
            onSongClick = { indice -> vm.play(visibleSongs, indice) },
            onSongMenu = {},
            onFolderClick = { path -> destino = Destino.Folder(path) },
            onDirectoriesClick = { destino = Destino.Directory(vm.directoryRoot.value) },
            onShuffleAll = { vm.shufflePlay(visibleSongs) },
            onPlayAll = { vm.play(visibleSongs, 0) },
            bottomBar = bottomBar,
        )

        is Destino.Folder -> {
            val delFolder = remember(allSongs, d.path, songSort) {
                vm.songsInFolder(allSongs, d.path)
            }
            FolderDetailScreen(
                folderName = d.path.substringAfterLast('/'),
                songs = delFolder,
                onBack = { destino = Destino.Library },
                onSongClick = { indice -> vm.play(delFolder, indice) },
                onSongMenu = {},
                onShuffle = { vm.shufflePlay(delFolder) },
                onPlay = { vm.play(delFolder, 0) },
                bottomBar = bottomBar,
            )
        }

        is Destino.Directory -> {
            val subdirs = remember(allSongs, d.path) { vm.subdirectoriesOf(allSongs, d.path) }
            val aqui = remember(allSongs, d.path, songSort) {
                vm.songsDirectlyIn(allSongs, d.path)
            }
            DirectoryScreen(
                path = d.path,
                subdirectories = subdirs,
                songsHere = aqui,
                onBack = {
                    val padre = DirectoryTree.parentOf(d.path)
                    val raiz = vm.directoryRoot.value
                    destino = if (padre != null && d.path != raiz) {
                        Destino.Directory(padre)
                    } else {
                        Destino.Library
                    }
                },
                onDirectoryClick = { path -> destino = Destino.Directory(path) },
                onSongClick = { indice -> vm.play(aqui, indice) },
                onSongMenu = {},
                bottomBar = bottomBar,
            )
        }
    }
}
