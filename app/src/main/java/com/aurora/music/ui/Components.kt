@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aurora.music.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aurora.music.PlayerViewModel
import com.aurora.music.data.Track

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun Art(uri: Uri?, modifier: Modifier = Modifier, corner: Dp = 8.dp) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(Color(0xFF3A2D7A), Color(0xFF7A2D55)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = .5f))
        AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun TrackRow(
    t: Track,
    vm: PlayerViewModel,
    current: Boolean,
    onRemove: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    val favs by vm.favorites.collectAsState()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Art(t.artwork, Modifier.size(48.dp), 6.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium,
                color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${t.artist} · ${t.album}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (t.id in favs) Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        Box {
            IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "More") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Play next") }, { vm.playNext(t); menu = false })
                DropdownMenuItem({ Text("Add to queue") }, { vm.addToQueue(t); menu = false })
                DropdownMenuItem({ Text(if (t.id in favs) "Remove from Liked" else "Add to Liked") }, { vm.toggleFavorite(t); menu = false })
                DropdownMenuItem({ Text("Add to playlist") }, { addDialog = true; menu = false })
                if (onRemove != null) DropdownMenuItem({ Text("Remove from playlist") }, { onRemove(); menu = false })
            }
        }
    }
    if (addDialog) AddToPlaylistDialog(t, vm) { addDialog = false }
}

@Composable
fun AddToPlaylistDialog(t: Track, vm: PlayerViewModel, onDismiss: () -> Unit) {
    val playlists by vm.playlists.collectAsState()
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                playlists.keys.forEach { p ->
                    Text(p, Modifier.fillMaxWidth().clickable { vm.addToPlaylist(p, t); onDismiss() }.padding(vertical = 12.dp))
                }
                OutlinedTextField(name, { name = it }, label = { Text("New playlist") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton({
                if (name.isNotBlank()) { vm.createPlaylist(name); vm.addToPlaylist(name.trim(), t) }
                onDismiss()
            }) { Text(if (name.isBlank()) "Close" else "Create & add") }
        }
    )
}

@Composable
fun Shelf(title: String, list: List<Track>, vm: PlayerViewModel) {
    if (list.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(list) { i, t ->
            Column(Modifier.width(140.dp).clickable { vm.play(list, i) }) {
                Art(t.artwork, Modifier.size(140.dp), 10.dp)
                Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp), fontWeight = FontWeight.Medium)
                Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
