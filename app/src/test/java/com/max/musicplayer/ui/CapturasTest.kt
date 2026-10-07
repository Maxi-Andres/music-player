package com.max.musicplayer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.max.musicplayer.data.FolderSort
import com.max.musicplayer.data.MusicLibrary
import com.max.musicplayer.data.QueueEntry
import com.max.musicplayer.data.Song
import com.max.musicplayer.data.SongSort
import com.max.musicplayer.data.song
import com.max.musicplayer.ui.components.MiniPlayer
import com.max.musicplayer.ui.screens.LibraryScreen
import com.max.musicplayer.ui.screens.NowPlayingScreen
import com.max.musicplayer.ui.theme.MusicPlayerTheme
import java.io.File
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Dibuja las pantallas reales a PNG, sin celular, emulador ni APK.
 *
 * Robolectric renderiza Compose en la JVM con los graficos nativos de Android, asi que lo
 * que sale es la UI de verdad con datos de mentira. Las imagenes quedan en
 * `app/build/capturas/`:
 *
 *     ./gradlew testDebugUnitTest --tests "*CapturasTest*"
 *
 * No verifica nada: es para mirar un cambio de UI antes de publicarlo.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class CapturasTest {

    @get:Rule
    val compose = createComposeRule()

    private val canciones = listOf(
        "Back in Black" to "AC/DC",
        "Bohemian Rhapsody" to "Queen",
        "Rock and Roll All Nite" to "Kiss",
        "De Musica Ligera" to "Soda Stereo",
        "Rockstar" to "Post Malone",
        "Flaca" to "Andres Calamaro",
        "Persiana Americana" to "Soda Stereo",
        "Highway to Hell" to "AC/DC",
        "Crocodile Rock" to "Elton John",
        "Seminare" to "Seru Giran",
    ).mapIndexed { i, (titulo, artista) ->
        song(
            id = i + 1L,
            title = titulo,
            artist = artista,
            filePath = "/storage/emulated/0/Music/${artista.replace("/", "")}/$titulo.mp3",
        )
    }

    @Test
    fun biblioteca() = capturar("01-biblioteca") { Biblioteca(query = "") }

    @Test
    fun bibliotecaBuscando() = capturar("02-biblioteca-buscando") { Biblioteca(query = "rock") }

    @Test
    fun reproduciendo() = capturar("03-reproduciendo") {
        NowPlayingScreen(
            song = canciones[1],
            isPlaying = true,
            positionMs = 70_000L,
            durationMs = 180_000L,
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_ALL,
            contextEntries = canciones.mapIndexed { i, s -> QueueEntry(i.toLong(), s) },
            currentContextIndex = 1,
            queuedCount = 0,
            onCollapse = {},
            onPlayPause = {},
            onPrevious = {},
            onNext = {},
            onSeek = {},
            onSeekBy = {},
            onToggleShuffle = {},
            onCycleRepeat = {},
            onOpenQueue = {},
            onOpenEqualizer = {},
            tintFromArtwork = false,
            showRing = false,
            ringColor = MaterialTheme.colorScheme.primary,
            onContextItemClick = {},
        )
    }

    @Composable
    private fun Biblioteca(query: String) {
        val visibles = MusicLibrary.sortSongs(MusicLibrary.search(canciones, query), SongSort.TITLE_ASC)
        LibraryScreen(
            songs = visibles,
            folders = MusicLibrary.groupIntoFolders(canciones),
            totalSongs = canciones.size,
            isScanning = false,
            selectedTab = LibraryTab.SONGS,
            query = query,
            songSort = SongSort.TITLE_ASC,
            folderSort = FolderSort.NAME_ASC,
            songsListState = rememberLazyListState(),
            foldersListState = rememberLazyListState(),
            onTabSelected = {},
            onQueryChange = {},
            onSongSortChange = {},
            onFolderSortChange = {},
            onSongClick = {},
            onSongMenu = {},
            onFolderClick = {},
            onFolderMenu = {},
            onDirectoriesClick = {},
            onOpenSettings = {},
            onShuffleAll = {},
            onPlayAll = {},
            onPlaySelection = {},
            onQueueSelection = {},
            updateAvailable = false,
            bottomBar = { Mini(canciones[2]) },
        )
    }

    @Composable
    private fun Mini(song: Song) {
        MiniPlayer(
            song = song,
            isPlaying = true,
            positionMs = 70_000L,
            durationMs = 180_000L,
            ringColor = MaterialTheme.colorScheme.primary,
            onPlayPause = {},
            onPrevious = {},
            onNext = {},
            onQueueClick = {},
            onExpand = {},
        )
    }

    private fun capturar(nombre: String, contenido: @Composable () -> Unit) {
        compose.setContent {
            MusicPlayerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) { contenido() }
            }
        }
        compose.waitForIdle()

        val archivo = File(CARPETA, "$nombre.png")
        archivo.parentFile?.mkdirs()
        archivo.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    companion object {
        /** Relativa al modulo `app`, que es desde donde Gradle corre los tests. */
        private const val CARPETA = "build/capturas"

        @JvmStatic
        @BeforeClass
        fun graficosReales() {
            // Sin esto PixelCopy devuelve una imagen vacia en vez de lo que se dibujo.
            System.setProperty("robolectric.pixelCopyRenderMode", "hardware")
        }
    }
}
