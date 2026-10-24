package com.aurora.music.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNo: Int,
    val dateAdded: Long,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val artwork: Uri get() = Uri.parse("content://media/external/audio/media/$id/albumart")
}

sealed interface Detail {
    data class Album(val name: String) : Detail
    data class Artist(val name: String) : Detail
    data class Playlist(val name: String) : Detail
    data object Liked : Detail
}
