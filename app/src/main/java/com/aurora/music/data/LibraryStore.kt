package com.aurora.music.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Favorites, playlists, recents and play counts, persisted in SharedPreferences. */
class LibraryStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("library", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(
        p.getString("fav", "")!!.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    )
    val favorites: StateFlow<Set<Long>> = _favorites

    private val _recents = MutableStateFlow(
        p.getString("recents", "")!!.split(",").mapNotNull { it.toLongOrNull() }
    )
    val recents: StateFlow<List<Long>> = _recents

    private val _counts = MutableStateFlow(readCounts())
    val playCounts: StateFlow<Map<Long, Int>> = _counts

    private val _playlists = MutableStateFlow(readPlaylists())
    val playlists: StateFlow<Map<String, List<Long>>> = _playlists

    fun toggleFavorite(id: Long) {
        val s = _favorites.value.toMutableSet()
        if (!s.add(id)) s.remove(id)
        _favorites.value = s
        p.edit().putString("fav", s.joinToString(",")).apply()
    }

    fun recordPlay(id: Long) {
        val r = (listOf(id) + _recents.value.filter { it != id }).take(40)
        _recents.value = r
        val c = _counts.value.toMutableMap()
        c[id] = (c[id] ?: 0) + 1
        _counts.value = c
        p.edit().putString("recents", r.joinToString(",")).putString("counts", c.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
    }

    fun createPlaylist(name: String) {
        if (name.isBlank() || _playlists.value.containsKey(name)) return
        savePlaylists(_playlists.value + (name to emptyList()))
    }

    fun deletePlaylist(name: String) = savePlaylists(_playlists.value - name)

    fun addToPlaylist(name: String, id: Long) {
        val cur = _playlists.value[name] ?: return
        if (id in cur) return
        savePlaylists(_playlists.value + (name to cur + id))
    }

    fun removeFromPlaylist(name: String, id: Long) {
        val cur = _playlists.value[name] ?: return
        savePlaylists(_playlists.value + (name to cur.filter { it != id }))
    }

    private fun savePlaylists(m: Map<String, List<Long>>) {
        _playlists.value = m
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, JSONArray(v)) }
        p.edit().putString("playlists", o.toString()).apply()
    }

    private fun readPlaylists(): Map<String, List<Long>> {
        val raw = p.getString("playlists", null) ?: return emptyMap()
        val o = JSONObject(raw)
        return o.keys().asSequence().associateWith { k ->
            val a = o.getJSONArray(k)
            (0 until a.length()).map { a.getLong(it) }
        }
    }

    private fun readCounts(): Map<Long, Int> =
        p.getString("counts", "")!!.split(",").mapNotNull {
            val s = it.split(":")
            if (s.size == 2) s[0].toLongOrNull()?.let { id -> id to (s[1].toIntOrNull() ?: 0) } else null
        }.toMap()
}
