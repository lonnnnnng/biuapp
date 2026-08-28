@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.lonnnnnng.biu.playback

import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class VolumeBalanceMode(
    val label: String,
    val description: String,
    internal val ratio: Float,
    internal val thresholdDb: Float,
    internal val makeupGainDb: Float,
    internal val fallbackGainMb: Int,
) {
    OFF("关闭", "保持原始动态范围", 1f, 0f, 0f, 0),
    GENTLE("轻柔", "轻微收窄不同视频的音量差异", 1.4f, -18f, 1f, 120),
    STANDARD("标准", "在音量和动态之间保持平衡", 1.8f, -21f, 2f, 260),
    STRONG("明显", "更积极地压低音量跳变", 2.4f, -24f, 3f, 420),
    ;

    companion object {
        fun fromStoredValue(value: String?): VolumeBalanceMode =
            entries.firstOrNull { mode -> mode.name == value } ?: OFF
    }
}

object PlaybackFadePolicy {
    const val FADE_IN_DURATION_MS = 260L
    const val FADE_OUT_DURATION_MS = 220L
    private const val FADE_STEPS = 8

    fun volumeAt(start: Float, end: Float, step: Int, totalSteps: Int = FADE_STEPS): Float {
        if (totalSteps <= 0) return end.coerceIn(0f, 1f)
        val progress = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
        return (start + ((end - start) * progress)).coerceIn(0f, 1f)
    }

    fun stepCount(): Int = FADE_STEPS
}

/**
 * long: 把播放、暂停和切歌的音量过渡集中管理，避免 UI、MediaSession 和解码器恢复分别改 volume 造成互相覆盖。
 */
class PlaybackFadeController(private val scope: CoroutineScope) {
    private var fadeJob: Job? = null
    private var enabled = true

    fun setEnabled(value: Boolean, player: ExoPlayer?) {
        if (enabled == value) return
        enabled = value
        fadeJob?.cancel()
        fadeJob = null
        player?.volume = 1f
    }

    fun bind(player: ExoPlayer) {
        fadeJob?.cancel()
        fadeJob = null
        player.volume = 1f
    }

    fun unbind(player: ExoPlayer) {
        if (fadeJob?.isActive == true) fadeJob?.cancel()
        fadeJob = null
        player.volume = 1f
    }

    fun play(player: ExoPlayer) {
        fadeJob?.cancel()
        fadeJob = null
        if (player.playWhenReady) {
            // long: 系统可能重复发送 PLAY；已经处于播放意图时不能把音量重新拉到 0，避免无意义的听感抽动。
            player.play()
            return
        }
        if (!enabled) {
            player.volume = 1f
            player.play()
            return
        }
        player.volume = 0f
        player.play()
        fadeTo(player, 1f, PlaybackFadePolicy.FADE_IN_DURATION_MS)
    }

    fun pause(player: ExoPlayer) {
        fadeJob?.cancel()
        fadeJob = null
        if (!enabled || (!player.isPlaying && !player.playWhenReady)) {
            player.volume = 1f
            player.pause()
            return
        }
        fadeTo(player, 0f, PlaybackFadePolicy.FADE_OUT_DURATION_MS) {
            if (player.playWhenReady || player.isPlaying) player.pause()
            player.volume = 1f
        }
    }

    fun skip(player: ExoPlayer, action: () -> Unit) {
        fadeJob?.cancel()
        fadeJob = null
        if (!enabled) {
            action()
            player.volume = 1f
            player.play()
            return
        }
        if (!player.isPlaying && !player.playWhenReady) {
            // long: 上一曲/下一曲是明确的播放操作，即使原曲暂停，切换后的分 P 也必须立即开始播放。
            action()
            player.volume = 0f
            player.play()
            fadeTo(player, 1f, PlaybackFadePolicy.FADE_IN_DURATION_MS)
            return
        }
        fadeTo(player, 0f, PlaybackFadePolicy.FADE_OUT_DURATION_MS) {
            action()
            // long: 切歌本身代表继续播放，不能依赖切换前的 playWhenReady；系统控件可能在转场期间短暂清零它。
            player.play()
            fadeTo(player, 1f, PlaybackFadePolicy.FADE_IN_DURATION_MS)
        }
    }

    fun stop(player: ExoPlayer) {
        fadeJob?.cancel()
        fadeJob = null
        player.volume = 1f
        player.stop()
    }

    fun cancel() {
        fadeJob?.cancel()
        fadeJob = null
        scope.cancel()
    }

    private fun fadeTo(
        player: ExoPlayer,
        target: Float,
        durationMs: Long,
        onFinished: (() -> Unit)? = null,
    ) {
        fadeJob?.cancel()
        val start = player.volume.coerceIn(0f, 1f)
        val steps = PlaybackFadePolicy.stepCount()
        val stepDelayMs = (durationMs / steps).coerceAtLeast(1L)
        fadeJob = scope.launch {
            repeat(steps + 1) { step ->
                if (!isActive) return@launch
                player.volume = PlaybackFadePolicy.volumeAt(start, target, step, steps)
                if (step < steps) delay(stepDelayMs)
            }
            onFinished?.invoke()
        }
    }
}

/**
 * long: 系统通知、耳机和 Android Auto 通过 MediaSession 操作这个包装播放器，内部业务仍直接操作 ExoPlayer，避免恢复逻辑等待淡出动画。
 */
class FadingPlaybackPlayer(
    private val delegate: ExoPlayer,
    private val fadeController: PlaybackFadeController,
) : ForwardingPlayer(delegate) {
    override fun play() = fadeController.play(delegate)

    override fun pause() = fadeController.pause(delegate)

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) fadeController.play(delegate) else fadeController.pause(delegate)
    }

    override fun seekToNextMediaItem() = fadeController.skip(delegate, delegate::seekToNextMediaItem)

    override fun seekToNext() = fadeController.skip(delegate, delegate::seekToNext)

    override fun seekToPreviousMediaItem() =
        fadeController.skip(delegate, delegate::seekToPreviousMediaItem)

    override fun seekToPrevious() = fadeController.skip(delegate, delegate::seekToPrevious)

    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        if (mediaItemIndex == delegate.currentMediaItemIndex) {
            delegate.seekToDefaultPosition(mediaItemIndex)
        } else {
            fadeController.skip(delegate) { delegate.seekToDefaultPosition(mediaItemIndex) }
        }
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (mediaItemIndex == delegate.currentMediaItemIndex) {
            delegate.seekTo(mediaItemIndex, positionMs)
        } else {
            fadeController.skip(delegate) { delegate.seekTo(mediaItemIndex, positionMs) }
        }
    }

    override fun stop() = fadeController.stop(delegate)

    override fun release() {
        fadeController.unbind(delegate)
        super.release()
    }
}

/**
 * long: 音频 Session 可能在 ExoPlayer 重建后变化，效果必须随新 Session 释放和重建，否则旧效果会继续占用系统音频资源。
 */
class PlaybackLoudnessController {
    private var effect: AudioEffect? = null
    private var activeSessionId = C.AUDIO_SESSION_ID_UNSET
    private var activeMode = VolumeBalanceMode.OFF

    fun apply(mode: VolumeBalanceMode, audioSessionId: Int) {
        if (mode == activeMode && audioSessionId == activeSessionId) return
        release()
        activeMode = mode
        activeSessionId = audioSessionId
        if (mode == VolumeBalanceMode.OFF || audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return

        val createdEffect = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                DynamicsProcessing(0, audioSessionId, dynamicsConfig(mode)).also { it.enabled = true }
            } else {
                LoudnessEnhancer(audioSessionId).also {
                    it.setTargetGain(mode.fallbackGainMb)
                    it.enabled = true
                }
            }
        }.getOrElse { error ->
            // long: 主效果失败时必须返回 fallback 实例，不能让外层 getOrNull 把回退结果覆盖为空。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching {
                    LoudnessEnhancer(audioSessionId).also {
                        it.setTargetGain(mode.fallbackGainMb)
                        it.enabled = true
                    }
                }.onFailure { fallbackError ->
                    android.util.Log.w(
                        "BiuPlayback",
                        "Audio balance unavailable: ${error::class.java.simpleName}/${fallbackError::class.java.simpleName}",
                    )
                }.getOrNull()
            } else {
                null
            }
        }
        effect = createdEffect
    }

    fun release() {
        runCatching { effect?.release() }
        effect = null
        activeSessionId = C.AUDIO_SESSION_ID_UNSET
        activeMode = VolumeBalanceMode.OFF
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun dynamicsConfig(mode: VolumeBalanceMode): DynamicsProcessing.Config {
        val band = DynamicsProcessing.MbcBand(
            true,
            20_000f,
            20f,
            220f,
            mode.ratio,
            mode.thresholdDb,
            6f,
            -90f,
            1f,
            0f,
            mode.makeupGainDb,
        )
        val mbc = DynamicsProcessing.Mbc(true, true, 1).apply { setBand(0, band) }
        val limiter = DynamicsProcessing.Limiter(true, true, 0, 2f, 80f, 10f, -1f, 0f)
        return DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION,
            2,
            false,
            0,
            true,
            1,
            false,
            0,
            true,
        )
            .setMbcAllChannelsTo(mbc)
            .setLimiterAllChannelsTo(limiter)
            .build()
    }
}
