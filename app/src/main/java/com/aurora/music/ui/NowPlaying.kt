@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aurora.music.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aurora.music.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DefaultAccent = Color(0xFF5B4BC4)

@Composable
fun NowPlaying(vm: PlayerViewModel, onClose: () -> Unit, onEq: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val tracks by vm.tracks.collectAsState()
    val favs by vm.favorites.collectAsState()
    val t = remember(ui.mediaId, tracks) { vm.trackById(ui.mediaId) }
    val ctx = LocalContext.current

    var accent by remember { mutableStateOf(DefaultAccent) }
    LaunchedEffect(t?.id) {
        accent = DefaultAccent
        val track = t ?: return@LaunchedEffect
        val req = ImageRequest.Builder(ctx).data(track.artwork).allowHardware(false).build()
        val bmp = ((ctx.imageLoader.execute(req) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        val rgb = withContext(Dispatchers.Default) {
            val p = Palette.from(bmp).generate()
            p.getDarkVibrantColor(p.getVibrantColor(p.getMutedColor(DefaultAccent.toArgb())))
        }
        accent = Color(rgb)
    }
    val bg by animateColorAsState(accent, label = "accent")

    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var queueOpen by remember { mutableStateOf(false) }
    var sleepMenu by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(bg.copy(alpha = .9f), MaterialTheme.colorScheme.background)))
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.Default.KeyboardArrowDown, "Close", Modifier.size(32.dp)) }
            Spacer(Modifier.weight(1f))
            Text("Now playing", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            IconButton(onEq) { Icon(Icons.Default.GraphicEq, "Equalizer") }
        }
        Spacer(Modifier.height(16.dp))
        Art(t?.artwork, Modifier.fillMaxWidth().aspectRatio(1f), 16.dp)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t?.title ?: "Nothing playing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t?.artist ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (t != null) IconButton({ vm.toggleFavorite(t) }) {
                val fav = t.id in favs
                Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like", tint = if (fav) MaterialTheme.colorScheme.secondary else Color.White)
            }
        }
        val dur = ui.durationMs.coerceAtLeast(1)
        Slider(
            value = if (dragging) dragValue else ui.positionMs.toFloat().coerceIn(0f, dur.toFloat()),
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = { vm.seekTo(dragValue.toLong()); dragging = false },
            valueRange = 0f..dur.toFloat()
        )
        Row(Modifier.fillMaxWidth()) {
            Text(fmt(if (dragging) dragValue.toLong() else ui.positionMs), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text(fmt(ui.durationMs), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(vm::toggleShuffle) { Icon(Icons.Default.Shuffle, "Shuffle", tint = if (ui.shuffle) MaterialTheme.colorScheme.primary else Color.White) }
            IconButton(vm::previous, Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(36.dp)) }
            FilledIconButton(vm::togglePlay, Modifier.size(72.dp)) {
                Icon(if (ui.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", Modifier.size(40.dp))
            }
            IconButton(vm::next, Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(36.dp)) }
            IconButton(vm::cycleRepeat) {
                Icon(
                    if (ui.repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat",
                    tint = if (ui.repeat != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Box {
                TextButton({ sleepMenu = true }) {
                    Icon(Icons.Default.Bedtime, null); Spacer(Modifier.width(6.dp))
                    Text(ui.sleepLeftMin?.let { "$it min" } ?: "Sleep timer")
                }
                DropdownMenu(sleepMenu, { sleepMenu = false }) {
                    listOf(15, 30, 45, 60).forEach { m -> DropdownMenuItem({ Text("$m minutes") }, { vm.setSleep(m); sleepMenu = false }) }
                    if (ui.sleepLeftMin != null) DropdownMenuItem({ Text("Turn off") }, { vm.setSleep(null); sleepMenu = false })
                }
            }
            TextButton({ queueOpen = true }) { Icon(Icons.Default.QueueMusic, null); Spacer(Modifier.width(6.dp)); Text("Queue") }
        }
    }

    if (queueOpen) {
        ModalBottomSheet(onDismissRequest = { queueOpen = false }) {
            Text("Up next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            LazyColumn {
                itemsIndexed(ui.queue) { i, id ->
                    val q = vm.trackById(id) ?: return@itemsIndexed
                    Row(Modifier.fillMaxWidth().clickable { vm.skipTo(i) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Art(q.artwork, Modifier.size(40.dp), 6.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(q.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (i == ui.queueIndex) MaterialTheme.colorScheme.primary else Color.Unspecified)
                            Text(q.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
