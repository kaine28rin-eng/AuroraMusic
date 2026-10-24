@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aurora.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurora.music.PlayerViewModel
import com.aurora.music.data.Detail
import com.aurora.music.data.Track
import java.util.Calendar

@Composable
fun HomeScreen(vm: PlayerViewModel, open: (Detail) -> Unit) {
    val tracks by vm.tracks.collectAsState()
    val recents by vm.recents.collectAsState()
    val counts by vm.playCounts.collectAsState()
    val favs by vm.favorites.collectAsState()
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening"
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ vm.play(tracks, shuffle = true) }, enabled = tracks.isNotEmpty()) {
                    Icon(Icons.Default.Shuffle, null); Spacer(Modifier.width(8.dp)); Text("Shuffle all")
                }
                FilledTonalButton({ open(Detail.Liked) }) {
                    Icon(Icons.Default.Favorite, null); Spacer(Modifier.width(8.dp)); Text("Liked (${favs.size})")
                }
            }
            if (tracks.isEmpty()) Text("No music found on this device yet.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Shelf("Recently played", recents.mapNotNull { vm.trackById(it) }, vm) }
        item { Shelf("Most played", counts.entries.sortedByDescending { it.value }.take(15).mapNotNull { vm.trackById(it.key) }, vm) }
        item { Shelf("Recently added", tracks.sortedByDescending { it.dateAdded }.take(15), vm) }
        item { Shelf("Your favorites", favs.mapNotNull { vm.trackById(it) }.take(15), vm) }
    }
}

@Composable
fun SearchScreen(vm: PlayerViewModel, open: (Detail) -> Unit) {
    val tracks by vm.tracks.collectAsState()
    val ui by vm.ui.collectAsState()
    var q by remember { mutableStateOf("") }
    val hits = remember(q, tracks) {
        if (q.isBlank()) emptyList() else tracks.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
    }
    val albums = remember(hits) { hits.map { it.album }.distinct().take(5) }
    val artists = remember(hits) { hits.map { it.artist }.distinct().take(5) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            q, { q = it }, singleLine = true, placeholder = { Text("Songs, artists, albums") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (q.isNotEmpty()) IconButton({ q = "" }) { Icon(Icons.Default.Close, "Clear") } },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        LazyColumn {
            if (artists.isNotEmpty()) item { SectionTitle("Artists") }
            items(artists.size) { i -> NameRow(artists[i], Icons.Default.Person) { open(Detail.Artist(artists[i])) } }
            if (albums.isNotEmpty()) item { SectionTitle("Albums") }
            items(albums.size) { i -> NameRow(albums[i], Icons.Default.Album) { open(Detail.Album(albums[i])) } }
            if (hits.isNotEmpty()) item { SectionTitle("Songs") }
            itemsIndexed(hits) { i, t -> TrackRow(t, vm, ui.mediaId == t.id.toString()) { vm.play(hits, i) } }
        }
    }
}

@Composable
private fun SectionTitle(s: String) =
    Text(s, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))

@Composable
private fun NameRow(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private enum class Sort(val label: String) { Title("Title"), Artist("Artist"), Added("Recently added") }

@Composable
fun LibraryScreen(vm: PlayerViewModel, open: (Detail) -> Unit) {
    val tracks by vm.tracks.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val favs by vm.favorites.collectAsState()
    val ui by vm.ui.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var sort by remember { mutableStateOf(Sort.Title) }
    var sortMenu by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(vm::scan) { Icon(Icons.Default.Refresh, "Rescan") }
        }
        ScrollableTabRow(tab, edgePadding = 8.dp, containerColor = MaterialTheme.colorScheme.background) {
            listOf("Songs", "Albums", "Artists", "Playlists").forEachIndexed { i, s -> Tab(tab == i, { tab = i }, text = { Text(s) }) }
        }
        when (tab) {
            0 -> {
                val sorted = remember(tracks, sort) {
                    when (sort) {
                        Sort.Title -> tracks.sortedBy { it.title.lowercase() }
                        Sort.Artist -> tracks.sortedWith(compareBy({ it.artist.lowercase() }, { it.album }, { it.trackNo }))
                        Sort.Added -> tracks.sortedByDescending { it.dateAdded }
                    }
                }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${tracks.size} songs", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        TextButton({ sortMenu = true }) { Text("Sort: ${sort.label}") }
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            Sort.entries.forEach { s -> DropdownMenuItem({ Text(s.label) }, { sort = s; sortMenu = false }) }
                        }
                    }
                }
                LazyColumn { itemsIndexed(sorted) { i, t -> TrackRow(t, vm, ui.mediaId == t.id.toString()) { vm.play(sorted, i) } } }
            }
            1 -> {
                val albums = remember(tracks) { tracks.groupBy { it.album }.toList().sortedBy { it.first.lowercase() } }
                LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(albums) { (name, list) ->
                        Column(Modifier.clickable { open(Detail.Album(name)) }) {
                            Art(list.first().artwork, Modifier.fillMaxWidth().aspectRatio(1f), 10.dp)
                            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
                            Text(list.first().artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            2 -> {
                val artists = remember(tracks) { tracks.groupBy { it.artist }.toList().sortedBy { it.first.lowercase() } }
                LazyColumn {
                    items(artists.size) { i ->
                        val (name, list) = artists[i]
                        Row(Modifier.fillMaxWidth().clickable { open(Detail.Artist(name)) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Art(list.first().artwork, Modifier.size(48.dp), 24.dp)
                            Spacer(Modifier.width(16.dp))
                            Column { Text(name, fontWeight = FontWeight.Medium); Text("${list.size} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
            else -> LazyColumn {
                item { NameRow("Liked songs (${favs.size})", Icons.Default.Favorite) { open(Detail.Liked) } }
                items(playlists.keys.toList().size) { i ->
                    val n = playlists.keys.toList()[i]
                    NameRow("$n (${playlists[n]?.size ?: 0})", Icons.Default.QueueMusic) { open(Detail.Playlist(n)) }
                }
                item { NameRow("New playlist", Icons.Default.Add) { newName = "" } }
            }
        }
    }
    newName?.let { n ->
        AlertDialog(
            onDismissRequest = { newName = null },
            title = { Text("New playlist") },
            text = { OutlinedTextField(n, { newName = it }, singleLine = true, label = { Text("Name") }) },
            confirmButton = { TextButton({ vm.createPlaylist(n); newName = null }) { Text("Create") } },
            dismissButton = { TextButton({ newName = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun DetailScreen(d: Detail, vm: PlayerViewModel, onBack: () -> Unit) {
    val tracks by vm.tracks.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val favs by vm.favorites.collectAsState()
    val ui by vm.ui.collectAsState()
    val (title, list) = when (d) {
        is Detail.Album -> d.name to tracks.filter { it.album == d.name }.sortedBy { it.trackNo }
        is Detail.Artist -> d.name to tracks.filter { it.artist == d.name }.sortedWith(compareBy({ it.album }, { it.trackNo }))
        is Detail.Playlist -> d.name to (playlists[d.name] ?: emptyList()).mapNotNull { vm.trackById(it) }
        Detail.Liked -> "Liked songs" to favs.mapNotNull { vm.trackById(it) }
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding()) {
        item {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Spacer(Modifier.weight(1f))
                if (d is Detail.Playlist) IconButton({ vm.deletePlaylist(d.name); onBack() }) { Icon(Icons.Default.Delete, "Delete playlist") }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Art(list.firstOrNull()?.artwork, Modifier.size(200.dp), 12.dp)
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text("${list.size} songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ vm.play(list, 0) }, enabled = list.isNotEmpty()) { Icon(Icons.Default.PlayArrow, null); Text("Play") }
                    FilledTonalButton({ vm.play(list, shuffle = true) }, enabled = list.isNotEmpty()) { Icon(Icons.Default.Shuffle, null); Spacer(Modifier.width(6.dp)); Text("Shuffle") }
                }
            }
        }
        itemsIndexed(list) { i, t ->
            TrackRow(
                t, vm, ui.mediaId == t.id.toString(),
                onRemove = (d as? Detail.Playlist)?.let { p -> { vm.removeFromPlaylist(p.name, t) } }
            ) { vm.play(list, i) }
        }
    }
}
