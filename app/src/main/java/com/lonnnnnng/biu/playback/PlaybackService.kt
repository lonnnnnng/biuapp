package com.lonnnnnng.biu.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val endEventClock = PlaybackEndEventClock()
    private val sessionCallback = object : MediaSession.Callback {
        @UnstableApi
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val trustPolicy = ControllerTrustPolicy(packageName, applicationInfo.uid)
            return if (
                trustPolicy.isAllowed(
                    controllerPackage = controller.packageName,
                    controllerUid = controller.uid,
                    isSystemTrusted = controller.isTrusted,
                )
            ) {
                super.onConnect(session, controller)
            } else {
                MediaSession.ConnectionResult.reject()
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // 播放结束是一次性业务事件；递增 ID 可供后续自动续播层去重，不能依赖可重复的展示文案。
                endEventClock.recordEnded()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this)
            .build()
            .apply {
                setAudioAttributes(AudioAttributes.DEFAULT, true)
                setHandleAudioBecomingNoisy(true)
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(playerListener)
            }

        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(sessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        player?.removeListener(playerListener)
        mediaSession?.release()
        player?.release()
        mediaSession = null
        player = null
        super.onDestroy()
    }
}

class ControllerTrustPolicy(
    private val applicationPackage: String,
    private val applicationUid: Int,
) {
    fun isAllowed(
        controllerPackage: String,
        controllerUid: Int,
        isSystemTrusted: Boolean,
    ): Boolean {
        // 系统媒体控件、蓝牙和可信伴生设备需要跨进程连接；普通第三方应用不能借此操控播放队列。
        return isSystemTrusted ||
            (controllerPackage == applicationPackage && controllerUid == applicationUid)
    }
}
