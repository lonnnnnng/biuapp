package com.lonnnnnng.biu.playback

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.playbackPreferencesDataStore by preferencesDataStore(name = "playback_preferences")

enum class PlaybackMode(val label: String) {
    SEQUENTIAL("顺序播放"),
    REPEAT_ALL("列表循环"),
    SHUFFLE("随机播放"),
    REPEAT_ONE("单曲循环"),
    ;

    fun next(): PlaybackMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromPlayer(repeatMode: Int, shuffleEnabled: Boolean): PlaybackMode {
            return when {
                repeatMode == Player.REPEAT_MODE_ONE -> REPEAT_ONE
                shuffleEnabled -> SHUFFLE
                repeatMode == Player.REPEAT_MODE_ALL -> REPEAT_ALL
                else -> SEQUENTIAL
            }
        }
    }
}

enum class SleepTimerMode(val label: String) {
    OFF("未开启"),
    DEADLINE("定时停止"),
    TRACK_END("当前歌曲结束"),
    QUEUE_END("当前队列结束"),
}

data class PlaybackPreferences(
    val mode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    val speed: Float = PlaybackSpeedPolicy.DEFAULT,
    val reportPlayHistory: Boolean = true,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.OFF,
    val sleepTimerDeadlineEpochMs: Long = 0L,
    val fadeEnabled: Boolean = true,
    val volumeBalanceMode: VolumeBalanceMode = VolumeBalanceMode.OFF,
)

object SleepTimerPolicy {
    val minuteOptions = listOf(15, 30, 60)

    fun deadlineAfterMinutes(nowEpochMs: Long, minutes: Int): Long {
        require(minutes in minuteOptions) { "不支持的睡眠定时时长" }
        return nowEpochMs + minutes * 60_000L
    }

    fun remainingMs(deadlineEpochMs: Long, nowEpochMs: Long): Long =
        (deadlineEpochMs - nowEpochMs).coerceAtLeast(0L)

    fun shouldFinishAfterAutoTransition(
        mode: SleepTimerMode,
        completedMediaItemIndex: Int,
        lastQueueIndex: Int,
    ): Boolean {
        return when (mode) {
            SleepTimerMode.TRACK_END -> true
            SleepTimerMode.QUEUE_END -> lastQueueIndex >= 0 && completedMediaItemIndex == lastQueueIndex
            SleepTimerMode.OFF,
            SleepTimerMode.DEADLINE,
            -> false
        }
    }

    fun shouldFinishOnPlaybackEnded(mode: SleepTimerMode): Boolean =
        mode == SleepTimerMode.TRACK_END || mode == SleepTimerMode.QUEUE_END
}

object PlaybackSpeedPolicy {
    const val DEFAULT = 1f
    val options = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    fun normalize(value: Float): Float {
        if (!value.isFinite()) return DEFAULT
        return options.minByOrNull { option -> abs(option - value) } ?: DEFAULT
    }
}

class PlaybackPreferenceRepository(context: Context) {
    private val dataStore = context.applicationContext.playbackPreferencesDataStore

    val preferences: Flow<PlaybackPreferences> = dataStore.data.map { values ->
        PlaybackPreferences(
            mode = values[KEY_MODE]
                ?.let { stored -> PlaybackMode.entries.firstOrNull { it.name == stored } }
                ?: PlaybackMode.SEQUENTIAL,
            speed = PlaybackSpeedPolicy.normalize(values[KEY_SPEED] ?: PlaybackSpeedPolicy.DEFAULT),
            reportPlayHistory = values[KEY_REPORT_PLAY_HISTORY] ?: true,
            sleepTimerMode = values[KEY_SLEEP_TIMER_MODE]
                ?.let { stored -> SleepTimerMode.entries.firstOrNull { it.name == stored } }
                ?: SleepTimerMode.OFF,
            sleepTimerDeadlineEpochMs = values[KEY_SLEEP_TIMER_DEADLINE_EPOCH_MS] ?: 0L,
            fadeEnabled = values[KEY_FADE_ENABLED] ?: true,
            volumeBalanceMode = VolumeBalanceMode.fromStoredValue(values[KEY_VOLUME_BALANCE_MODE]),
        )
    }

    suspend fun current(): PlaybackPreferences = preferences.first()

    suspend fun save(mode: PlaybackMode, speed: Float) {
        dataStore.edit { values ->
            values[KEY_MODE] = mode.name
            values[KEY_SPEED] = PlaybackSpeedPolicy.normalize(speed)
        }
    }

    suspend fun saveReportPlayHistory(enabled: Boolean) {
        dataStore.edit { values -> values[KEY_REPORT_PLAY_HISTORY] = enabled }
    }

    suspend fun saveSleepTimer(mode: SleepTimerMode, deadlineEpochMs: Long = 0L) {
        require(mode != SleepTimerMode.DEADLINE || deadlineEpochMs > 0L) { "定时停止缺少截止时间" }
        dataStore.edit { values ->
            values[KEY_SLEEP_TIMER_MODE] = mode.name
            values[KEY_SLEEP_TIMER_DEADLINE_EPOCH_MS] = if (mode == SleepTimerMode.DEADLINE) deadlineEpochMs else 0L
        }
    }

    suspend fun clearSleepTimer() = saveSleepTimer(SleepTimerMode.OFF)

    suspend fun saveFadeEnabled(enabled: Boolean) {
        dataStore.edit { values -> values[KEY_FADE_ENABLED] = enabled }
    }

    suspend fun saveVolumeBalanceMode(mode: VolumeBalanceMode) {
        dataStore.edit { values -> values[KEY_VOLUME_BALANCE_MODE] = mode.name }
    }

    private companion object {
        val KEY_MODE = stringPreferencesKey("mode")
        val KEY_SPEED = floatPreferencesKey("speed")
        val KEY_REPORT_PLAY_HISTORY = booleanPreferencesKey("report_play_history")
        val KEY_SLEEP_TIMER_MODE = stringPreferencesKey("sleep_timer_mode")
        val KEY_SLEEP_TIMER_DEADLINE_EPOCH_MS = longPreferencesKey("sleep_timer_deadline_epoch_ms")
        val KEY_FADE_ENABLED = booleanPreferencesKey("fade_enabled")
        val KEY_VOLUME_BALANCE_MODE = stringPreferencesKey("volume_balance_mode")
    }
}

fun Player.applyPlaybackMode(mode: PlaybackMode) {
    // long: 四态模式由 repeat 与 shuffle 两个 Media3 属性共同表达，切换时必须同时重置，避免随机和单曲循环叠加。
    when (mode) {
        PlaybackMode.SEQUENTIAL -> {
            shuffleModeEnabled = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
        PlaybackMode.REPEAT_ALL -> {
            shuffleModeEnabled = false
            repeatMode = Player.REPEAT_MODE_ALL
        }
        PlaybackMode.SHUFFLE -> {
            shuffleModeEnabled = true
            repeatMode = Player.REPEAT_MODE_ALL
        }
        PlaybackMode.REPEAT_ONE -> {
            shuffleModeEnabled = false
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
}
