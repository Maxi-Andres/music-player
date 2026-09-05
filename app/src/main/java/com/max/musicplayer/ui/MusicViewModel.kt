package com.max.musicplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.max.musicplayer.data.DirectoryTree
import com.max.musicplayer.data.FolderSort
import com.max.musicplayer.data.MediaStoreScanner
import com.max.musicplayer.data.MusicFolder
import com.max.musicplayer.data.MusicLibrary
import com.max.musicplayer.data.Song
import com.max.musicplayer.data.SongSort
import com.max.musicplayer.playback.PlayerConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab { SONGS, PLAYLISTS, FOLDERS, ALBUMS, ARTISTS }

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = MediaStoreScanner(app)

    val player = PlayerConnection(app, viewModelScope)

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())

    /**
     * Biblioteca cruda. Las pantallas de detalle (una carpeta, un directorio) derivan
     * de aca con `remember`, para no multiplicar flujos por cada ruta posible.
     */
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _tab = MutableStateFlow(LibraryTab.SONGS)
    val tab: StateFlow<LibraryTab> = _tab.asStateFlow()

    private val _songSort = MutableStateFlow(SongSort.DATE_ADDED_DESC)
    val songSort: StateFlow<SongSort> = _songSort.asStateFlow()

    private val _folderSort = MutableStateFlow(FolderSort.NAME_ASC)
    val folderSort: StateFlow<FolderSort> = _folderSort.asStateFlow()

    /**
     * Orden dentro de una carpeta. Va aparte del de la pestania Canciones y arranca
     * alfabetico, igual que en docs/reference/03-detalle-carpeta.jpeg (la lista general
     * viene por fecha, la de adentro de una carpeta por titulo).
     */
    private val _folderSongSort = MutableStateFlow(SongSort.TITLE_ASC)
    val folderSongSort: StateFlow<SongSort> = _folderSongSort.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val totalSongs: StateFlow<Int> = _allSongs
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    /** Canciones de la pestania "Canciones": filtradas por busqueda y ordenadas. */
    val visibleSongs: StateFlow<List<Song>> =
        combine(_allSongs, _query, _songSort) { songs, q, sort ->
            MusicLibrary.sortSongs(MusicLibrary.search(songs, q), sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Carpetas de la pestania "Carpetas". */
    val visibleFolders: StateFlow<List<MusicFolder>> =
        combine(_allSongs, _query, _folderSort) { songs, q, sort ->
            MusicLibrary.sortFolders(
                MusicLibrary.searchFolders(MusicLibrary.groupIntoFolders(songs), q),
                sort,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Directorio desde el que arranca el navegador de archivos. */
    val directoryRoot: StateFlow<String> = _allSongs
        .map { DirectoryTree.commonRoot(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    fun start() {
        player.connect()
        rescan()
    }

    fun rescan() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _allSongs.value = scanner.scan()
            _isScanning.value = false
        }
    }

    fun selectTab(t: LibraryTab) { _tab.value = t }

    fun setQuery(q: String) { _query.value = q }

    fun setSongSort(s: SongSort) { _songSort.value = s }

    fun setFolderSort(s: FolderSort) { _folderSort.value = s }

    fun setFolderSongSort(s: SongSort) { _folderSongSort.value = s }

    // --- derivaciones puntuales, usadas desde las pantallas de detalle ---

    fun songsInFolder(songs: List<Song>, folderPath: String): List<Song> =
        MusicLibrary.sortSongs(
            MusicLibrary.songsInFolder(songs, folderPath),
            _folderSongSort.value,
        )

    fun subdirectoriesOf(songs: List<Song>, path: String) =
        DirectoryTree.childrenOf(songs, path)

    fun songsDirectlyIn(songs: List<Song>, path: String): List<Song> =
        MusicLibrary.sortSongs(DirectoryTree.songsDirectlyIn(songs, path), _folderSongSort.value)

    fun songsUnder(songs: List<Song>, path: String): List<Song> =
        DirectoryTree.songsUnder(songs, path)

    // --- reproduccion ---

    fun play(songs: List<Song>, index: Int) = player.playContext(songs, index)

    /** Reproduce la lista mezclada arrancando por una cualquiera, como "Aleatorio". */
    fun shufflePlay(songs: List<Song>) {
        if (songs.isEmpty()) return
        player.playContext(songs, songs.indices.random(), shuffle = true)
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    private companion object {
        /** Se mantiene el flujo vivo un rato tras girar la pantalla, para no recalcular. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
