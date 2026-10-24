package com.aurora.music.data

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val ctx: Context) {
    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val out = ArrayList<Track>()
        val a = MediaStore.Audio.Media
        val projection = arrayOf(a._ID, a.TITLE, a.ARTIST, a.ALBUM, a.ALBUM_ID, a.DURATION, a.TRACK, a.DATE_ADDED)
        val selection = "${a.IS_MUSIC} != 0 AND ${a.DURATION} >= 30000"
        ctx.contentResolver.query(a.EXTERNAL_CONTENT_URI, projection, selection, null, "${a.TITLE} COLLATE NOCASE ASC")?.use { c ->
            val iId = c.getColumnIndexOrThrow(a._ID)
            val iTitle = c.getColumnIndexOrThrow(a.TITLE)
            val iArtist = c.getColumnIndexOrThrow(a.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(a.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(a.ALBUM_ID)
            val iDur = c.getColumnIndexOrThrow(a.DURATION)
            val iTrack = c.getColumnIndexOrThrow(a.TRACK)
            val iAdded = c.getColumnIndexOrThrow(a.DATE_ADDED)
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
