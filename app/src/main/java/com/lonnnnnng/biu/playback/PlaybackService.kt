package com.lonnnnnng.biu.playback

import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lonnnnnng.biu.appContainer
import com.lonnnnnng.biu.core.model.bilibiliSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val endEventClock = PlaybackEndEventClock()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshInFlight = false
    private var retryConsumedForCurrentItem = false
    private var recordedMediaId: String? = null
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
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (!refreshInFlight) retryConsumedForCurrentItem = false
            val isNewPlaybackRequest = !refreshInFlight && reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            if (isNewPlaybackRequest) recordedMediaId = null
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                val mediaItem = player?.currentMediaItem ?: return
                val mediaId = mediaItem.mediaId.takeIf(String::isNotBlank)
                if (mediaId != recordedMediaId) {
                    recordedMediaId = mediaId
                    serviceScope.launch {
                        // long: 只有解码器真正进入播放态后才写历史，403、解析失败或未开始播放的点击不会污染记录。
                        appContainer.playbackHistoryRepository.recordStarted(mediaItem)
                    }
                }
            } else {
                persistCurrentProgress()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // long: 播放结束是一次性业务事件；递增 ID 可供后续自动续播层去重，不能依赖可重复的展示文案。
                endEventClock.recordEnded()
                persistCurrentProgress()
            }
        }

        @UnstableApi
        override fun onPlayerError(error: PlaybackException) {
            error.httpFailure()?.let { failure ->
                // long: 日志只记录 CDN 主机和状态码，不输出带签名的完整 DASH URL、Cookie 或账号信息。
                Log.w(LOG_TAG, "DASH request failed: host=${failure.host}, code=${failure.code}")
            }
            if (error.isExpiredDashUrlError()) {
                refreshCurrentBilibiliTrack()
            }
        }
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        val dataSourceFactory = OkHttpDataSource.Factory(appContainer.bilibiliHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
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
        serviceScope.cancel()
        player?.removeListener(playerListener)
        mediaSession?.release()
        player?.release()
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun refreshCurrentBilibiliTrack() {
        val activePlayer = player ?: return
        val currentItem = activePlayer.currentMediaItem ?: return
        val source = currentItem.bilibiliSource() ?: return
        if (refreshInFlight || retryConsumedForCurrentItem) return

        refreshInFlight = true
        retryConsumedForCurrentItem = true
        val mediaIndex = activePlayer.currentMediaItemIndex
        val positionMs = activePlayer.currentPosition
        serviceScope.launch {
            runCatching {
                appContainer.bilibiliRepository.resolveAudioStream(
                    source.bvid,
                    source.cid,
                    source.qualityPreference,
                )
            }.onSuccess { stream ->
                // long: 主 CDN 失效时优先切换新解析地址或备用 CDN，同时保留标题、封面、队列位置和已播放进度。
                val failedUrl = currentItem.localConfiguration?.uri?.toString().orEmpty()
                val refreshedItem = currentItem.buildUpon().setUri(stream.replacementUrl(failedUrl)).build()
                activePlayer.replaceMediaItem(mediaIndex, refreshedItem)
                activePlayer.seekTo(mediaIndex, positionMs)
                activePlayer.prepare()
                activePlayer.play()
            }
            refreshInFlight = false
        }
    }

    private fun persistCurrentProgress() {
        val activePlayer = player ?: return
        val mediaItem = activePlayer.currentMediaItem ?: return
        val positionMs = activePlayer.currentPosition
        val durationMs = activePlayer.duration
        serviceScope.launch {
            // long: 暂停或播放结束时保存最后位置，用户下次进入本地历史即可判断是否听过和听到哪里。
            appContainer.playbackHistoryRepository.recordProgress(mediaItem, positionMs, durationMs)
        }
    }
}

@UnstableApi
private fun PlaybackException.httpFailure(): DashHttpFailure? {
    var current: Throwable? = this
    while (current != null) {
        if (current is HttpDataSource.InvalidResponseCodeException) {
            return DashHttpFailure(current.dataSpec.uri.host.orEmpty(), current.responseCode)
        }
        current = current.cause
    }
    return null
}

private fun PlaybackException.isExpiredDashUrlError(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is HttpDataSource.InvalidResponseCodeException) {
            return current.responseCode in setOf(403, 404, 410)
        }
        current = current.cause
    }
    return false
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
        // long: 系统媒体控件、蓝牙和可信伴生设备需要跨进程连接；普通第三方应用不能借此操控播放队列。
        return isSystemTrusted ||
            (controllerPackage == applicationPackage && controllerUid == applicationUid)
    }
}

private data class DashHttpFailure(val host: String, val code: Int)

private const val LOG_TAG = "BiuPlayback"
