@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aurora.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aurora.music.audio.AudioFx

@Composable
fun EqualizerScreen(onClose: () -> Unit) {
    val s by AudioFx.state.collectAsState()
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Equalizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(s.enabled, { AudioFx.setEnabled(it) }, enabled = s.available, modifier = Modifier.padding(end = 12.dp))
        }
        if (!s.available) {
            Text(
                "The equalizer attaches to the audio session once playback has started. Play a song, then come back.",
                Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(s.presets) { i, name ->
                FilterChip(s.preset == i, { AudioFx.usePreset(i) }, { Text(name) }, enabled = s.enabled)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            s.levels.forEachIndexed { i, level ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%+.1f".format(level / 100f), style = MaterialTheme.typography.labelSmall)
                    VSlider(
                        level.toFloat(), s.minLevel.toFloat(), s.maxLevel.toFloat(), s.enabled,
                        { AudioFx.setBand(i, it.toInt()) },
                        Modifier.height(220.dp).width(44.dp)
                    )
                    Text(
                        s.freqsHz[i].let { if (it >= 1000) "${it / 1000}k" else "$it" } + " Hz",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        TextButton({ AudioFx.reset() }, Modifier.padding(horizontal = 8.dp), enabled = s.enabled) { Text("Reset to flat") }
        EffectSlider("Bass boost", s.bass, 1000, s.enabled) { AudioFx.setBass(it) }
        EffectSlider("Virtualizer (stereo width)", s.virtualizer, 1000, s.enabled) { AudioFx.setVirtualizer(it) }
        EffectSlider("Loudness gain", s.loudness, 1000, s.enabled) { AudioFx.setLoudness(it) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EffectSlider(label: String, value: Int, max: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Row { Text(label, Modifier.weight(1f)); Text("${value * 100 / max}%", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = 0f..max.toFloat(), enabled = enabled)
    }
}

@Composable
fun VSlider(value: Float, min: Float, max: Float, enabled: Boolean, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val active = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier
            .pointerInput(min, max, enabled) {
                detectTapGestures { if (enabled) onChange(max - (it.y / size.height).coerceIn(0f, 1f) * (max - min)) }
            }
            .pointerInput(min, max, enabled) {
                detectVerticalDragGestures { change, _ ->
                    if (enabled) {
                        change.consume()
                        onChange(max - (change.position.y / size.height).coerceIn(0f, 1f) * (max - min))
                    }
                }
            }
    ) {
        val cx = size.width / 2
        val y = size.height * (1f - (value - min) / (max - min))
        val zeroY = size.height * (1f - (0f - min) / (max - min))
        drawLine(track, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 10f, cap = StrokeCap.Round)
        drawLine(active, Offset(cx, zeroY), Offset(cx, y), strokeWidth = 10f, cap = StrokeCap.Round)
        drawCircle(active, 14f, Offset(cx, y))
    }
}
