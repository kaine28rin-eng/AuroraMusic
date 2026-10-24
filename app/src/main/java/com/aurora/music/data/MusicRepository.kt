package com.aurora.music.data

import android.content.Context
import android.provider.MediaStore.Audio.Media as A
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val ctx: Context) {
    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val out = ArrayList<Track>()
        val projection = arrayOf(A._ID, A.TITLE, A.ARTIST, A.ALBUM, A.ALBUM_ID, A.DURATION, A.TRACK, A.DATE_ADDED)
        val selection = "${A.IS_MUSIC} != 0 AND ${A.DURATION} >= 30000"
        ctx.contentResolver.query(A.EXTERNAL_CONTENT_URI, projection, selection, null, "${A.TITLE} COLLATE NOCASE ASC")?.use { c ->
            val iId = c.getColumnIndexOrThrow(A._ID)
            val iTitle = c.getColumnIndexOrThrow(A.TITLE)
            val iArtist = c.getColumnIndexOrThrow(A.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(A.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(A.ALBUM_ID)
            val iDur = c.getColumnIndexOrThrow(A.DURATION)
            val iTrack = c.getColumnIndexOrThrow(A.TRACK)
            val iAdded = c.getColumnIndexOrThrow(A.DATE_ADDED)
            while (c.moveToNext()) {
                val artist = c.getString(iArtist)?.takeUnless { it == "<unknown>" } ?: "Unknown artist"
                val album = c.getString(iAlbum)?.takeUnless { it.isBlank() } ?: "Unknown album"
                out += Track(
                    id = c.getLong(iId),
                    title = c.getString(iTitle) ?: "Untitled",
                    artist = artist,
                    album = album,
                    albumId = c.getLong(iAlbumId),
                    durationMs = c.getLong(iDur),
                    trackNo = c.getInt(iTrack) % 1000,
                    dateAdded = c.getLong(iAdded),
                )
            }
        }
        out
    }
}
