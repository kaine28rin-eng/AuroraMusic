package com.aurora.music.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide audio effects bound to the ExoPlayer audio session.
 * The PlaybackService calls attach(); the UI drives the setters.
 */
object AudioFx {
    data class State(
        val available: Boolean = false,
        val enabled: Boolean = false,
        val freqsHz: List<Int> = emptyList(),
        val levels: List<Int> = emptyList(),   // millibels
        val minLevel: Int = -1500,
        val maxLevel: Int = 1500,
        val presets: List<String> = emptyList(),
        val preset: Int = -1,                  // -1 = custom
        val bass: Int = 0,                     // 0..1000
        val virtualizer: Int = 0,              // 0..1000
        val loudness: Int = 0,                 // millibels of gain, 0..1000
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var eq: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null
    private var prefs: android.content.SharedPreferences? = null

    fun attach(ctx: Context, sessionId: Int) {
        if (sessionId <= 0) return
        val p = ctx.applicationContext.getSharedPreferences("audiofx", Context.MODE_PRIVATE)
        prefs = p
        release()
        try {
            val e = Equalizer(0, sessionId).also { eq = it }
            val n = e.numberOfBands.toInt()
            val range = e.bandLevelRange
            val saved = p.getString("levels", null)?.split(",")?.mapNotNull { it.toIntOrNull() }
            val levels = if (saved != null && saved.size == n) saved else List(n) { e.getBandLevel(it.toShort()).toInt() }
            levels.forEachIndexed { i, l -> e.setBandLevel(i.toShort(), l.toShort()) }
            val enabled = p.getBoolean("enabled", false)
            val bass = p.getInt("bass", 0)
            val virtual = p.getInt("virt", 0)
            val loudness = p.getInt("loud", 0)
            runCatching { bassBoost = BassBoost(0, sessionId).also { if (it.strengthSupported) it.setStrength(bass.toShort()) } }
            runCatching { virt = Virtualizer(0, sessionId).also { if (it.strengthSupported) it.setStrength(virtual.toShort()) } }
            runCatching { loud = LoudnessEnhancer(sessionId).also { it.setTargetGain(loudness) } }
            _state.value = State(
                available = true,
                enabled = enabled,
                freqsHz = List(n) { e.getCenterFreq(it.toShort()) / 1000 },
                levels = levels,
                minLevel = range[0].toInt(),
                maxLevel = range[1].toInt(),
                presets = List(e.numberOfPresets.toInt()) { e.getPresetName(it.toShort()) },
                preset = p.getInt("preset", -1),
                bass = bass, virtualizer = virtual, loudness = loudness,
            )
            applyEnabled(enabled)
        } catch (t: Throwable) {
            _state.value = State(available = false)
        }
    }

    fun release() {
        eq?.release(); bassBoost?.release(); virt?.release(); loud?.release()
        eq = null; bassBoost = null; virt = null; loud = null
    }

    private fun applyEnabled(on: Boolean) {
        runCatching { eq?.enabled = on }
        runCatching { bassBoost?.enabled = on && _state.value.bass > 0 }
        runCatching { virt?.enabled = on && _state.value.virtualizer > 0 }
        runCatching { loud?.enabled = on && _state.value.loudness > 0 }
    }

    fun setEnabled(on: Boolean) {
        _state.value = _state.value.copy(enabled = on)
        applyEnabled(on)
        prefs?.edit()?.putBoolean("enabled", on)?.apply()
    }

    fun setBand(index: Int, level: Int) {
        val s = _state.value
        val l = level.coerceIn(s.minLevel, s.maxLevel)
        runCatching { eq?.setBandLevel(index.toShort(), l.toShort()) }
        val levels = s.levels.toMutableList().also { it[index] = l }
        _state.value = s.copy(levels = levels, preset = -1)
        prefs?.edit()?.putString("levels", levels.joinToString(","))?.putInt("preset", -1)?.apply()
    }

    fun usePreset(index: Int) {
        val e = eq ?: return
        runCatching { e.usePreset(index.toShort()) }
        val levels = List(e.numberOfBands.toInt()) { e.getBandLevel(it.toShort()).toInt() }
        _state.value = _state.value.copy(levels = levels, preset = index)
        prefs?.edit()?.putString("levels", levels.joinToString(","))?.putInt("preset", index)?.apply()
    }

    fun reset() {
        val s = _state.value
        s.levels.indices.forEach { setBand(it, 0) }
    }

    fun setBass(v: Int) {
        _state.value = _state.value.copy(bass = v)
        runCatching { bassBoost?.setStrength(v.toShort()); bassBoost?.enabled = _state.value.enabled && v > 0 }
        prefs?.edit()?.putInt("bass", v)?.apply()
    }

    fun setVirtualizer(v: Int) {
        _state.value = _state.value.copy(virtualizer = v)
        runCatching { virt?.setStrength(v.toShort()); virt?.enabled = _state.value.enabled && v > 0 }
        prefs?.edit()?.putInt("virt", v)?.apply()
    }

    fun setLoudness(v: Int) {
        _state.value = _state.value.copy(loudness = v)
        runCatching { loud?.setTargetGain(v); loud?.enabled = _state.value.enabled && v > 0 }
        prefs?.edit()?.putInt("loud", v)?.apply()
    }
}
