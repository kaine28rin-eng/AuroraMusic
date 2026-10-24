package com.aurora.music

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aurora.music.data.LibraryStore
import com.aurora.music.data.MusicRepository
import com.aurora.music.data.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUi(
    val mediaId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val queue: List<String> = emptyList(),
    val queueIndex: Int = 0,
    val sleepLeftMin: Int? = null,
)

class PlayerViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository(app)
    private val store = LibraryStore(app)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()
    private var byId: Map<Long, Track> = emptyMap()

    private val _ui = MutableStateFlow(PlayerUi())
    val ui: StateFlow<PlayerUi> = _ui.asStateFlow()

    val favorites = store.favorites
    val recents = store.recents
    val playCounts = store.playCounts
    val playlists = store.playlists

    private var controller: MediaController? = null
    private var sleepJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            item?.mediaId?.toLongOrNull()?.let(store::recordPlay)
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(listener) }
            refresh()
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (true) {
                delay(500)
                val c = controller ?: continue
                if (c.isPlaying) _ui.update { it.copy(positionMs = c.currentPosition) }
            }
        }
    }

    fun trackById(id: String?): Track? = id?.toLongOrNull()?.let { byId[it] }
    fun trackById(id: Long): Track? = byId[id]

    fun scan() {
        viewModelScope.launch {
            val list = repo.scan()
            byId = list.associateBy { it.id }
            _tracks.value = list
        }
    }

    private fun refresh() {
        val c = controller ?: return
        _ui.update {
            it.copy(
                mediaId = c.currentMediaItem?.mediaId,
                isPlaying = c.isPlaying,
                positionMs = c.currentPosition,
                durationMs = c.duration.coerceAtLeast(0),
                shuffle = c.shuffleModeEnabled,
                repeat = c.repeatMode,
                queue = (0 until c.mediaItemCount).map { i -> c.getMediaItemAt(i).mediaId },
                queueIndex = c.currentMediaItemIndex,
            )
        }
    }

    private fun Track.toItem() = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setArtworkUri(artwork).build()
        ).build()

    fun play(list: List<Track>, index: Int = 0, shuffle: Boolean = false) {
        val c = controller ?: return
        if (list.isEmpty()) return
        c.shuffleModeEnabled = shuffle
        c.setMediaItems(list.map { it.toItem() }, if (shuffle) list.indices.random() else index, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(ms: Long) { controller?.seekTo(ms); _ui.update { it.copy(positionMs = ms) } }
    fun skipTo(index: Int) { controller?.seekToDefaultPosition(index) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun playNext(t: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) play(listOf(t)) else c.addMediaItem(c.currentMediaItemIndex + 1, t.toItem())
    }
    fun addToQueue(t: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) play(listOf(t)) else c.addMediaItem(t.toItem())
    }

    fun toggleFavorite(t: Track) = store.toggleFavorite(t.id)
    fun createPlaylist(name: String) = store.createPlaylist(name.trim())
    fun deletePlaylist(name: String) = store.deletePlaylist(name)
    fun addToPlaylist(name: String, t: Track) = store.addToPlaylist(name, t.id)
    fun removeFromPlaylist(name: String, t: Track) = store.removeFromPlaylist(name, t.id)

    fun setSleep(minutes: Int?) {
        sleepJob?.cancel()
        _ui.update { it.copy(sleepLeftMin = minutes) }
        if (minutes == null) return
        sleepJob = viewModelScope.launch {
            var left = minutes
            while (left > 0) {
                delay(60_000)
                left--
                _ui.update { it.copy(sleepLeftMin = left) }
            }
            controller?.pause()
            _ui.update { it.copy(sleepLeftMin = null) }
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        super.onCleared()
    }
}
