@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aurora.music.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurora.music.PlayerViewModel
import com.aurora.music.data.Detail

@Composable
fun Root(vm: PlayerViewModel = viewModel()) {
    val ctx = LocalContext.current
    val perms = remember {
        buildList {
            add(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, perms[0]) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        granted = r[perms[0]] == true
        if (granted) vm.scan()
    }
    LaunchedEffect(granted) { if (granted) vm.scan() }

    if (!granted) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LibraryMusic, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Your music, beautifully played", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Allow access to audio files on this device to build your library.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button({ launcher.launch(perms) }) { Text("Allow access") }
        }
        return
    }

    val ui by vm.ui.collectAsState()
    val tracks by vm.tracks.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showPlayer by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    val current = remember(ui.mediaId, tracks) { vm.trackById(ui.mediaId) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    if (current != null) MiniPlayer(vm) { showPlayer = true }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf(Icons.Default.Home to "Home", Icons.Default.Search to "Search", Icons.Default.LibraryMusic to "Library")
                            .forEachIndexed { i, (icon, label) ->
                                NavigationBarItem(tab == i, { tab = i; detail = null }, { Icon(icon, label) }, label = { Text(label) })
                            }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    0 -> HomeScreen(vm) { detail = it }
                    1 -> SearchScreen(vm) { detail = it }
                    else -> LibraryScreen(vm) { detail = it }
                }
                detail?.let { d ->
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        DetailScreen(d, vm, onBack = { detail = null })
                    }
                }
            }
        }
        AnimatedVisibility(showPlayer, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            NowPlaying(vm, onClose = { showPlayer = false }, onEq = { showEq = true })
        }
        AnimatedVisibility(showEq, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            EqualizerScreen(onClose = { showEq = false })
        }
    }

    BackHandler(showEq) { showEq = false }
    BackHandler(showPlayer && !showEq) { showPlayer = false }
    BackHandler(detail != null && !showPlayer && !showEq) { detail = null }
}

@Composable
fun MiniPlayer(vm: PlayerViewModel, onOpen: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val tracks by vm.tracks.collectAsState()
    val t = remember(ui.mediaId, tracks) { vm.trackById(ui.mediaId) } ?: return
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Art(t.artwork, Modifier.size(44.dp), 6.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(vm::togglePlay) { Icon(if (ui.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause") }
                IconButton(vm::next) { Icon(Icons.Default.SkipNext, "Next") }
            }
            LinearProgressIndicator(
                progress = { if (ui.durationMs > 0) ui.positionMs.toFloat() / ui.durationMs else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
        }
    }
}
