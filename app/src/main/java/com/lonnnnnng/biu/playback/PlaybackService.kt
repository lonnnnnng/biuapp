package com.lonnnnnng.biu.playback

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.lonnnnnng.biu.appContainer
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.PlaybackMediaMode
import com.lonnnnnng.biu.core.model.PlaybackStreamMetadata
import com.lonnnnnng.biu.core.model.PlaybackVideoQuality
import com.lonnnnnng.biu.core.model.bilibiliSource
import com.lonnnnnng.biu.core.model.playbackMediaMode
import com.lonnnnnng.biu.core.model.playbackStreamMetadata
import com.lonnnnnng.biu.core.model.toMediaItem
import com.lonnnnnng.biu.core.model.toTrackOrNull
import com.lonnnnnng.biu.core.model.withAudioPlaybackStream
import com.lonnnnnng.biu.core.model.withVideoPlaybackStreams
import com.lonnnnnng.biu.data.bilibili.BilibiliApiException
import com.lonnnnnng.biu.data.bilibili.DashVideoCodecPreference
import com.lonnnnnng.biu.data.local.PlaybackQueueRecord
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val endEventClock = PlaybackEndEventClock()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshInFlight = false
    private var mediaModeSwitchInFlight = false
    private var codecRecoveryInFlight = false
    private var retryConsumedForCurrentItem = false
    private val codecRecoveryPolicy = PlaybackCodecRecoveryPolicy()
    private var recordedMediaId: String? = null
    private var progressPersistenceJob: Job? = null
    private var queuePersistenceJob: Job? = null
    private var playbackPreferencesPersistenceJob: Job? = null
    private var reportPlayHistoryEnabled = true
    private var activeHeartbeatSession: ActiveHeartbeatSession? = null
    private val heartbeatMutex = Mutex()
    private val queuePersistenceGeneration = AtomicLong(0L)
    private val queuePersistenceMutex = Mutex()
    private var restoringPlaybackQueue = false
    private lateinit var mediaSourceFactory: DefaultMediaSourceFactory
    // long: Redmi 的 MTK HEVC 在正常解码和资源 flush 两条路径都已确认会原生崩溃；AVC 硬解配合 Player 自愈是当前真机可持续出画面的路径。
    private val videoCodecPreference = DashVideoCodecPreference.AVC
    private val defaultVideoMaxQualityId: Int? by lazy {
        if (defaultAvcDecoderIsMtk()) MTK_DEFAULT_VIDEO_MAX_QUALITY_ID else null
    }
    private val isMediaReplacementInFlight: Boolean
        get() = refreshInFlight || mediaModeSwitchInFlight || codecRecoveryInFlight
    private val sessionCallback = object : MediaSession.Callback {
        @UnstableApi
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val trustPolicy = ControllerTrustPolicy(packageName, applicationInfo.uid)
            val isAllowed =
                trustPolicy.isAllowed(
                    controllerPackage = controller.packageName,
                    controllerUid = controller.uid,
                    isSystemTrusted = controller.isTrusted,
                )
            if (!isAllowed) return MediaSession.ConnectionResult.reject()

            val defaultResult = super.onConnect(session, controller)
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(defaultResult.availablePlayerCommands)
                .setAvailableSessionCommands(
                    defaultResult.availableSessionCommands.buildUpon()
                        .add(PlaybackSessionCommands.setMediaMode)
                        .add(PlaybackSessionCommands.setVideoQuality)
                        .build(),
                )
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            return when (customCommand) {
                PlaybackSessionCommands.setMediaMode -> {
                    val requestedMode = PlaybackSessionCommands.requestedMediaMode(args)
                        ?: return immediateSessionResult(SessionError.ERROR_BAD_VALUE, "播放类型无效")
                    switchCurrentPlaybackMediaMode(requestedMode)
                }
                PlaybackSessionCommands.setVideoQuality -> {
                    val qualityId = PlaybackSessionCommands.requestedVideoQualityId(args)
                        ?: return immediateSessionResult(SessionError.ERROR_BAD_VALUE, "视频画质无效")
                    switchCurrentVideoQuality(qualityId)
                }
                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        @UnstableApi
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            val oldMediaItem = oldPosition.mediaItem
            if (
                oldMediaItem != null &&
                PlaybackProgressPersistencePolicy.shouldPersistTransition(
                    oldMediaId = oldMediaItem.mediaId,
                    newMediaId = newPosition.mediaItem?.mediaId.orEmpty(),
                )
            ) {
                reportPlaybackHeartbeat(
                    mediaItem = oldMediaItem,
                    event = PlaybackHeartbeatEvent.END,
                    positionMs = oldPosition.positionMs,
                    durationMs = durationForMediaItem(oldPosition.mediaItemIndex, oldPosition.positionMs),
                )
                // long: 自动播完、手动切 P 或替换队列都不会保证进入暂停态，跨媒体项时必须把旧 cid 的最终位置单独落盘。
                persistProgress(
                    mediaItem = oldMediaItem,
                    positionMs = oldPosition.positionMs,
                    durationMs = durationForMediaItem(oldPosition.mediaItemIndex, oldPosition.positionMs),
                )
            }
            // long: 同一媒体项内的 seek 也要立即保存队列位置，否则强制停止后会回到拖动前的时间点。
            persistPlaybackQueueNow()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            schedulePlaybackQueuePersistence()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            schedulePlaybackPreferencesPersistence()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            schedulePlaybackPreferencesPersistence()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            schedulePlaybackPreferencesPersistence()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                // long: 合并 MP4 只用于音频播放时不应创建无用视频解码器；切到视频模式再恢复视频轨选择。
                setVideoTrackEnabled(player, item.playbackMediaMode() == PlaybackMediaMode.VIDEO)
            }
            if (!isMediaReplacementInFlight) {
                retryConsumedForCurrentItem = false
                codecRecoveryPolicy.resetForExternalMediaItemTransition()
            }
            val isNewPlaybackRequest =
                !isMediaReplacementInFlight && reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            if (isNewPlaybackRequest) recordedMediaId = null
            if (!isMediaReplacementInFlight && player?.isPlaying == true && mediaItem != null) {
                // long: 分 P 队列切换时播放态可能始终为 true，必须在媒体项变化回调中单独登记新 cid 的播放历史。
                recordStartedIfNeeded(mediaItem)
                if (!isMediaReplacementInFlight) {
                    reportPlaybackHeartbeat(
                        mediaItem = mediaItem,
                        event = PlaybackHeartbeatEvent.START,
                        positionMs = player?.currentPosition ?: 0L,
                        durationMs = player?.duration ?: C.TIME_UNSET,
                    )
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                val mediaItem = player?.currentMediaItem ?: return
                recordStartedIfNeeded(mediaItem)
                startProgressPersistence()
                reportPlaybackHeartbeat(
                    mediaItem = mediaItem,
                    event = PlaybackHeartbeatEvent.START,
                    positionMs = player?.currentPosition ?: 0L,
                    durationMs = player?.duration ?: C.TIME_UNSET,
                )
            } else {
                stopProgressPersistence()
                persistCurrentProgress()
                val activePlayer = player
                if (
                    !isMediaReplacementInFlight &&
                    activePlayer != null &&
                    !activePlayer.playWhenReady &&
                    activePlayer.playbackState != Player.STATE_ENDED
                ) {
                    activePlayer.currentMediaItem?.let { mediaItem ->
                        reportPlaybackHeartbeat(
                            mediaItem = mediaItem,
                            event = PlaybackHeartbeatEvent.PAUSE,
                            positionMs = activePlayer.currentPosition,
                            durationMs = activePlayer.duration,
                        )
                    }
                }
            }
            persistPlaybackQueueNow()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                stopProgressPersistence()
                // long: 播放结束是一次性业务事件；递增 ID 可供后续自动续播层去重，不能依赖可重复的展示文案。
                endEventClock.recordEnded()
                persistCurrentProgress()
                persistPlaybackQueueNow()
                player?.currentMediaItem?.let { mediaItem ->
                    reportPlaybackHeartbeat(
                        mediaItem = mediaItem,
                        event = PlaybackHeartbeatEvent.END,
                        positionMs = player?.currentPosition ?: 0L,
                        durationMs = player?.duration ?: C.TIME_UNSET,
                    )
                }
            }
        }

        @UnstableApi
        override fun onPlayerError(error: PlaybackException) {
            error.httpFailure()?.let { failure ->
                // long: 日志只记录 CDN 主机和状态码，不输出带签名的完整 DASH URL、Cookie 或账号信息。
                Log.w(LOG_TAG, "DASH request failed: host=${failure.host}, code=${failure.code}")
            }
            if (error.isExpiredDashUrlError()) {
                refreshCurrentBilibiliTrack(error.failedDashUrl())
            } else {
                recoverPlayerFromCodecFailure(error)
            }
        }
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        // long: 组合数据源同时支持 Bilibili HTTPS 和 MediaStore content Uri，本地音乐无需复制到应用私有目录。
        val dataSourceFactory = DefaultDataSource.Factory(
            this,
            OkHttpDataSource.Factory(appContainer.bilibiliHttpClient),
        )
        mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        val exoPlayer = createExoPlayer()

        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(sessionCallback)
            .build()
        restorePlaybackPreferences()
        observePlaybackPreferences()
        restorePlaybackQueue()
    }

    @UnstableApi
    private fun createExoPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)
            // long: Redmi 的 MTK OMX 异步队列在高 profile DASH 轨上会在播放中让 mediaserver 崩溃；同步适配器仍保留 Media3 的解码器回退能力。
            .forceDisableMediaCodecAsynchronousQueueing()
            .setMediaCodecSelector(mtkAvcSafeCodecSelector())
        return ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                // long: 手机默认播放音频；禁用视频轨可避免合并 MP4 按视频缓冲阈值等待，视频模式切换时由服务重新启用。
                setTrackSelectionParameters(
                    trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                        .build(),
                )
                setAudioAttributes(AudioAttributes.DEFAULT, true)
                setHandleAudioBecomingNoisy(true)
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(playerListener)
            }
    }

    private fun mtkAvcSafeCodecSelector(): MediaCodecSelector {
        return MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val defaultDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder,
            )
            if (
                mimeType == MimeTypes.VIDEO_H264 &&
                defaultDecoders.firstOrNull()?.name?.startsWith("OMX.MTK.") == true
            ) {
                // long: c2 软件 AVC 已在真机 ih264d_format_convert 中原生崩溃并黑屏；过滤该 fallback，保留能在重建后继续出画面的 MTK AVC。
                defaultDecoders.filterNot { decoder -> decoder.name == "c2.android.avc.decoder" }
            } else {
                defaultDecoders
            }
        }
    }

    private fun defaultAvcDecoderIsMtk(): Boolean {
        return runCatching {
            MediaCodecSelector.DEFAULT.getDecoderInfos(
                MimeTypes.VIDEO_H264,
                false,
                false,
            ).firstOrNull()?.name?.startsWith("OMX.MTK.") == true
        }.getOrDefault(false)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        stopProgressPersistence()
        queuePersistenceJob?.cancel()
        queuePersistenceJob = null
        playbackPreferencesPersistenceJob?.cancel()
        playbackPreferencesPersistenceJob = null
        serviceScope.cancel()
        player?.removeListener(playerListener)
        mediaSession?.release()
        player?.release()
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun recordStartedIfNeeded(mediaItem: MediaItem) {
        val mediaId = mediaItem.mediaId.takeIf(String::isNotBlank)
        if (mediaId == recordedMediaId) return
        recordedMediaId = mediaId
        serviceScope.launch {
            // long: 只有解码器真正进入播放态后才写历史，403、解析失败或未开始播放的点击不会污染记录。
            appContainer.playbackHistoryRepository.recordStarted(mediaItem)
        }
    }

    @UnstableApi
    private fun recoverPlayerFromCodecFailure(error: PlaybackException) {
        val failedPlayer = player ?: return
        val mediaId = failedPlayer.currentMediaItem?.mediaId.orEmpty()
        if (
            !codecRecoveryPolicy.tryBeginRecovery(
                mediaId = mediaId,
                errorCode = error.errorCode,
                causeTypeNames = error.causeTypeNames(),
                nowElapsedMs = SystemClock.elapsedRealtime(),
                isMtkVideoPlayback =
                    failedPlayer.currentMediaItem?.playbackMediaMode() == PlaybackMediaMode.VIDEO &&
                        defaultVideoMaxQualityId != null,
            )
        ) {
            return
        }

        codecRecoveryInFlight = true
        val snapshot = PlaybackPlayerRecoverySnapshot.capture(failedPlayer)
        if (snapshot == null) {
            codecRecoveryInFlight = false
            codecRecoveryPolicy.finishRecovery()
            return
        }

        Log.w(LOG_TAG, "Rebuilding player after codec failure: code=${error.errorCode}")
        val replacementPlayer = runCatching {
            createExoPlayer().apply {
                repeatMode = snapshot.repeatMode
                shuffleModeEnabled = snapshot.shuffleModeEnabled
                playbackParameters = snapshot.playbackParameters
                volume = snapshot.volume
                setMediaSources(
                    snapshot.mediaItems.map(::createPlaybackMediaSource),
                    snapshot.currentIndex,
                    snapshot.currentPositionMs,
                )
                playWhenReady = snapshot.playWhenReady
            }
        }.getOrElse { recoveryError ->
            player = failedPlayer
            codecRecoveryInFlight = false
            codecRecoveryPolicy.finishRecovery()
            Log.e(LOG_TAG, "Player rebuild after codec failure failed", recoveryError)
            return
        }
        val activeSession = mediaSession
        if (activeSession == null) {
            replacementPlayer.removeListener(playerListener)
            replacementPlayer.release()
            codecRecoveryInFlight = false
            codecRecoveryPolicy.finishRecovery()
            return
        }

        failedPlayer.removeListener(playerListener)
        val sessionSwapError = runCatching {
            // long: MediaSession 保持不变，系统锁屏控件与现有 MediaController 无需重连；只替换已被 DEAD_OBJECT 污染的底层 Player。
            activeSession.setPlayer(replacementPlayer)
        }.exceptionOrNull()
        if (sessionSwapError != null) {
            replacementPlayer.removeListener(playerListener)
            replacementPlayer.release()
            failedPlayer.addListener(playerListener)
            codecRecoveryInFlight = false
            codecRecoveryPolicy.finishRecovery()
            Log.e(LOG_TAG, "Player rebuild after codec failure failed", sessionSwapError)
            return
        }

        player = replacementPlayer
        runCatching { failedPlayer.release() }
            .onFailure { releaseError ->
                // long: Session 已经切到新 Player 后不能回滚；旧 codec 释放异常只做脱敏记录，避免把控制器重新指回损坏实例。
                Log.w(LOG_TAG, "Failed player release after codec recovery: type=${releaseError::class.java.simpleName}")
            }
        serviceScope.launch {
            try {
                // long: MTK OMX 原生进程死亡后需要短暂拉起时间；延迟 prepare 可避免新 Player 立即撞上尚未恢复的 codec 服务。
                delay(CODEC_SERVICE_RECOVERY_DELAY_MS)
                if (player === replacementPlayer) {
                    replacementPlayer.prepare()
                    Log.i(LOG_TAG, "Player rebuilt after codec failure")
                }
            } finally {
                codecRecoveryInFlight = false
                codecRecoveryPolicy.finishRecovery()
            }
        }
    }

    private fun startProgressPersistence() {
        if (progressPersistenceJob?.isActive == true) return
        progressPersistenceJob = serviceScope.launch {
            while (isActive) {
                delay(PROGRESS_PERSIST_INTERVAL_MS)
                // long: 长音频播放中周期落盘，把进程异常退出时最多丢失的进度控制在一个保存周期内。
                persistCurrentProgress()
                persistPlaybackQueueNow()
                val activePlayer = player
                activePlayer?.currentMediaItem?.let { mediaItem ->
                    reportPlaybackHeartbeat(
                        mediaItem = mediaItem,
                        event = PlaybackHeartbeatEvent.PROGRESS,
                        positionMs = activePlayer.currentPosition,
                        durationMs = activePlayer.duration,
                    )
                }
            }
        }
    }

    private fun stopProgressPersistence() {
        progressPersistenceJob?.cancel()
        progressPersistenceJob = null
    }

    private fun refreshCurrentBilibiliTrack(failedUrl: String?) {
        val activePlayer = player ?: return
        val currentItem = activePlayer.currentMediaItem ?: return
        val source = currentItem.bilibiliSource() ?: return
        if (isMediaReplacementInFlight || retryConsumedForCurrentItem) return

        refreshInFlight = true
        retryConsumedForCurrentItem = true
        val target = PlaybackMediaSwitchTarget.capture(activePlayer, currentItem, source.bvid, source.cid)
        serviceScope.launch {
            runCatching {
                resolvePlaybackStreamMetadata(
                    mode = currentItem.playbackMediaMode(),
                    source = source,
                    failedUrl = failedUrl,
                    requestedVideoQualityId = currentItem.playbackStreamMetadata()?.selectedVideoQualityId,
                )
            }.onSuccess { streamMetadata ->
                // long: 网络解析期间用户可能已切歌或编辑队列；只有当前 bvid/cid 和索引仍一致时才允许替换媒体源。
                if (target.matches(activePlayer)) {
                    applyPlaybackStreamMetadata(activePlayer, target, streamMetadata)
                }
            }
            refreshInFlight = false
        }
    }

    private fun switchCurrentPlaybackMediaMode(mode: PlaybackMediaMode): ListenableFuture<SessionResult> {
        val activePlayer = player
            ?: return immediateSessionResult(SessionError.ERROR_INVALID_STATE, "播放器尚未连接")
        val currentItem = activePlayer.currentMediaItem
            ?: return immediateSessionResult(SessionError.ERROR_INVALID_STATE, "当前没有播放内容")
        val source = currentItem.bilibiliSource()
            ?: return immediateSessionResult(SessionError.ERROR_NOT_SUPPORTED, "本地音频不支持视频播放")
        if (currentItem.playbackMediaMode() == mode) {
            return immediateSessionResult(SessionResult.RESULT_SUCCESS)
        }
        if (isMediaReplacementInFlight) {
            return immediateSessionResult(SessionError.ERROR_INVALID_STATE, "正在切换播放类型")
        }

        return replaceCurrentPlaybackStream(
            activePlayer = activePlayer,
            currentItem = currentItem,
            source = source,
            mode = mode,
            requestedVideoQualityId = null,
            failureMessage = if (mode == PlaybackMediaMode.VIDEO) "视频地址解析失败" else "音频地址解析失败",
        )
    }

    private fun switchCurrentVideoQuality(qualityId: Int): ListenableFuture<SessionResult> {
        val activePlayer = player
            ?: return immediateSessionResult(SessionError.ERROR_INVALID_STATE, "播放器尚未连接")
        val currentItem = activePlayer.currentMediaItem
            ?: return immediateSessionResult(SessionError.ERROR_INVALID_STATE, "当前没有播放内容")
        val source = currentItem.bilibiliSource()
            ?: return immediateSessionResult(SessionError.ERROR_NOT_SUPPORTED, "本地音频不支持视频画质切换")
        val currentStream = currentItem.playbackStreamMetadata()
        if (currentStream?.mode != PlaybackMediaMode.VIDEO) {
            return immediateSessionResult(SessionError.ERROR_INVALID_STATE, "请先切换到视频播放")
        }
        if (currentStream.selectedVideoQualityId == qualityId) {
            return immediateSessionResult(SessionResult.RESULT_SUCCESS)
        }
        if (currentStream.availableVideoQualities.none { quality -> quality.qualityId == qualityId }) {
            return immediateSessionResult(SessionError.ERROR_BAD_VALUE, "所选画质当前不可用")
        }
        if (isMediaReplacementInFlight) {
            return immediateSessionResult(SessionError.ERROR_INVALID_STATE, "正在切换视频画质")
        }

        return replaceCurrentPlaybackStream(
            activePlayer = activePlayer,
            currentItem = currentItem,
            source = source,
            mode = PlaybackMediaMode.VIDEO,
            requestedVideoQualityId = qualityId,
            failureMessage = "视频画质切换失败",
        )
    }

    private fun replaceCurrentPlaybackStream(
        activePlayer: ExoPlayer,
        currentItem: MediaItem,
        source: BilibiliTrackSource,
        mode: PlaybackMediaMode,
        requestedVideoQualityId: Int?,
        failureMessage: String,
    ): ListenableFuture<SessionResult> {
        mediaModeSwitchInFlight = true
        val target = PlaybackMediaSwitchTarget.capture(activePlayer, currentItem, source.bvid, source.cid)
        val future = SettableFuture.create<SessionResult>()
        serviceScope.launch {
            val result = runCatching {
                resolvePlaybackStreamMetadata(
                    mode = mode,
                    source = source,
                    failedUrl = null,
                    requestedVideoQualityId = requestedVideoQualityId,
                )
            }.fold(
                onSuccess = { streamMetadata ->
                    if (!target.matches(activePlayer)) {
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    } else {
                        // long: 切换只重建当前分 P 的媒体源，保留完整队列、进度、倍速、循环模式和用户原来的播放/暂停意图。
                        applyPlaybackStreamMetadata(activePlayer, target, streamMetadata)
                        retryConsumedForCurrentItem = false
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    }
                },
                onFailure = { error ->
                    val message = (error as? BilibiliApiException)?.message?.takeIf(String::isNotBlank) ?: failureMessage
                    SessionResult(SessionError.ERROR_IO, PlaybackSessionCommands.resultExtras(message))
                },
            )
            mediaModeSwitchInFlight = false
            future.set(result)
        }
        return future
    }

    private suspend fun resolvePlaybackStreamMetadata(
        mode: PlaybackMediaMode,
        source: BilibiliTrackSource,
        failedUrl: String?,
        requestedVideoQualityId: Int?,
    ): PlaybackStreamMetadata {
        return when (mode) {
            PlaybackMediaMode.AUDIO -> {
                val stream = appContainer.bilibiliRepository.resolveAudioStream(
                    source.bvid,
                    source.cid,
                    // long: 地址失效刷新也重新选择最高可用音质，避免旧媒体项把已移除的省流量档带回播放链路。
                    AudioQualityPreference.HIGHEST,
                )
                PlaybackStreamMetadata.audio(
                    audioUrl = failedUrl?.let(stream::replacementUrl) ?: stream.url,
                    qualityLabel = stream.qualityLabel,
                )
            }
            PlaybackMediaMode.VIDEO -> {
                val streams = appContainer.bilibiliRepository.resolveVideoPlaybackStreams(
                    source.bvid,
                    source.cid,
                    requestedVideoQualityId,
                    videoCodecPreference,
                    defaultVideoMaxQualityId,
                )
                PlaybackStreamMetadata.video(
                    audioUrl = failedUrl?.let(streams.audio::replacementUrl) ?: streams.audio.url,
                    videoUrl = failedUrl?.let(streams.video::replacementUrl) ?: streams.video.url,
                    audioQualityLabel = streams.audio.qualityLabel,
                    videoQualityLabel = streams.video.qualityLabel,
                    selectedVideoQualityId = streams.video.qualityId,
                    availableVideoQualities = streams.availableVideos.map { video ->
                        PlaybackVideoQuality(video.qualityId, video.qualityLabel)
                    },
                )
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun applyPlaybackStreamMetadata(
        activePlayer: ExoPlayer,
        target: PlaybackMediaSwitchTarget,
        stream: PlaybackStreamMetadata,
    ) {
        val currentItem = activePlayer.currentMediaItem ?: return
        val replacementItem = when (stream.mode) {
            PlaybackMediaMode.AUDIO -> currentItem.withAudioPlaybackStream(
                audioUrl = stream.audioUrl,
                qualityLabel = stream.audioQualityLabel,
            )
            PlaybackMediaMode.VIDEO -> currentItem.withVideoPlaybackStreams(
                audioUrl = stream.audioUrl,
                videoUrl = stream.videoUrl.orEmpty(),
                audioQualityLabel = stream.audioQualityLabel,
                videoQualityLabel = stream.videoQualityLabel,
                selectedVideoQualityId = stream.selectedVideoQualityId,
                availableVideoQualities = stream.availableVideoQualities,
            )
        }
        setVideoTrackEnabled(activePlayer, stream.mode == PlaybackMediaMode.VIDEO)
        if (stream.mode == PlaybackMediaMode.VIDEO) {
            val sources = (0 until activePlayer.mediaItemCount).map { index ->
                val item = if (index == target.mediaIndex) replacementItem else activePlayer.getMediaItemAt(index)
                createPlaybackMediaSource(item)
            }
            activePlayer.setMediaSources(sources, target.mediaIndex, target.positionMs)
        } else {
            activePlayer.replaceMediaItem(target.mediaIndex, replacementItem)
            activePlayer.seekTo(target.mediaIndex, target.positionMs)
        }
        activePlayer.prepare()
        // long: 暂停状态切视频只预加载画面，不得因为媒体源重建而擅自开始播放。
        if (target.playWhenReady) activePlayer.play() else activePlayer.pause()
    }

    private fun setVideoTrackEnabled(activePlayer: ExoPlayer?, enabled: Boolean) {
        activePlayer ?: return
        activePlayer.setTrackSelectionParameters(
            activePlayer.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !enabled)
                .build(),
        )
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun createPlaybackMediaSource(mediaItem: MediaItem): MediaSource {
        val stream = mediaItem.playbackStreamMetadata()
        if (stream?.mode != PlaybackMediaMode.VIDEO || stream.videoUrl.isNullOrBlank()) {
            return mediaSourceFactory.createMediaSource(mediaItem)
        }
        val videoItem = mediaItem.withVideoPlaybackStreams(
            audioUrl = stream.audioUrl,
            videoUrl = stream.videoUrl,
            audioQualityLabel = stream.audioQualityLabel,
            videoQualityLabel = stream.videoQualityLabel,
            selectedVideoQualityId = stream.selectedVideoQualityId,
            availableVideoQualities = stream.availableVideoQualities,
        )
        val audioItem = MediaItem.Builder()
            .setMediaId("${mediaItem.mediaId}:audio-companion")
            .setUri(stream.audioUrl)
            .build()
        return MergingMediaSource(
            true,
            true,
            mediaSourceFactory.createMediaSource(videoItem),
            mediaSourceFactory.createMediaSource(audioItem),
        )
    }

    private fun immediateSessionResult(resultCode: Int, message: String? = null): ListenableFuture<SessionResult> {
        return SettableFuture.create<SessionResult>().apply {
            set(
                if (message == null) {
                    SessionResult(resultCode)
                } else {
                    SessionResult(resultCode, PlaybackSessionCommands.resultExtras(message))
                },
            )
        }
    }

    private fun persistCurrentProgress() {
        val activePlayer = player ?: return
        val mediaItem = activePlayer.currentMediaItem ?: return
        persistProgress(mediaItem, activePlayer.currentPosition, activePlayer.duration)
    }

    private fun durationForMediaItem(mediaItemIndex: Int, fallbackPositionMs: Long): Long {
        val timeline = player?.currentTimeline
        val timelineDurationMs = if (timeline != null && mediaItemIndex in 0 until timeline.windowCount) {
            timeline.getWindow(mediaItemIndex, Timeline.Window()).durationMs
        } else {
            C.TIME_UNSET
        }
        return PlaybackProgressPersistencePolicy.durationMs(timelineDurationMs, fallbackPositionMs)
    }

    private fun persistProgress(mediaItem: MediaItem, positionMs: Long, durationMs: Long) {
        serviceScope.launch {
            // long: 保存具体媒体项而不是重新读取 currentMediaItem，避免分 P 转场后把上一 P 的进度写到下一 P。
            appContainer.playbackHistoryRepository.recordProgress(mediaItem, positionMs, durationMs)
        }
    }

    private fun restorePlaybackQueue() {
        serviceScope.launch {
            val restored = runCatching { appContainer.playbackQueueRepository.load() }.getOrNull() ?: return@launch
            val activePlayer = player ?: return@launch
            // long: 数据库读取期间用户可能已选择新内容；只允许旧快照填充空播放器，不能覆盖刚创建的新队列。
            if (activePlayer.mediaItemCount > 0) return@launch
            restoringPlaybackQueue = true
            try {
                activePlayer.setMediaItems(
                    restored.items.map { track -> track.toMediaItem() },
                    restored.currentIndex,
                    restored.currentPositionMs,
                )
                activePlayer.prepare()
            } finally {
                restoringPlaybackQueue = false
            }
        }
    }

    private fun restorePlaybackPreferences() {
        serviceScope.launch {
            val preferences = runCatching { appContainer.playbackPreferenceRepository.current() }.getOrNull()
                ?: PlaybackPreferences()
            val activePlayer = player ?: return@launch
            activePlayer.applyPlaybackMode(preferences.mode)
            activePlayer.setPlaybackSpeed(preferences.speed)
        }
    }

    private fun observePlaybackPreferences() {
        serviceScope.launch {
            appContainer.playbackPreferenceRepository.preferences.collect { preferences ->
                reportPlayHistoryEnabled = preferences.reportPlayHistory
                if (!preferences.reportPlayHistory) {
                    heartbeatMutex.withLock { activeHeartbeatSession = null }
                }
            }
        }
    }

    private fun reportPlaybackHeartbeat(
        mediaItem: MediaItem,
        event: PlaybackHeartbeatEvent,
        positionMs: Long,
        durationMs: Long,
    ) {
        val source = mediaItem.bilibiliSource()
        if (!PlaybackHeartbeatPolicy.isReportable(reportPlayHistoryEnabled, source != null)) return
        source ?: return
        serviceScope.launch {
            heartbeatMutex.withLock {
                // long: 开关关闭会清空会话；锁内再次校验可拦住此前已排队、但尚未开始上报的任务。
                if (!PlaybackHeartbeatPolicy.isReportable(reportPlayHistoryEnabled, hasBilibiliSource = true)) {
                    return@withLock
                }
                try {
                    val now = System.currentTimeMillis() / 1000L
                    var session = activeHeartbeatSession
                    if (event == PlaybackHeartbeatEvent.START && session?.mediaId != mediaItem.mediaId) {
                        val aid = source.aid
                            ?: appContainer.bilibiliRepository.videoDetail(source.bvid).aid
                            ?: return@withLock
                        session = ActiveHeartbeatSession(
                            mediaId = mediaItem.mediaId,
                            aid = aid,
                            bvid = source.bvid,
                            cid = source.cid,
                            session = UUID.randomUUID().toString().replace("-", ""),
                            startedAtEpochSeconds = now,
                        )
                        activeHeartbeatSession = session
                    }
                    session = activeHeartbeatSession
                    if (session == null || session.mediaId != mediaItem.mediaId) return@withLock
                    if (!PlaybackHeartbeatPolicy.shouldSend(event, now, session.lastSentAtEpochSeconds)) return@withLock
                    // long: aid 查询可能挂起；查询期间关闭开关时，不得继续提交本次播放记录。
                    if (!PlaybackHeartbeatPolicy.isReportable(reportPlayHistoryEnabled, hasBilibiliSource = true)) {
                        activeHeartbeatSession = null
                        return@withLock
                    }
                    val playedSeconds = (positionMs.coerceAtLeast(0L) / 1_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    val durationSeconds = durationMs
                        .takeIf { it != C.TIME_UNSET && it > 0L }
                        ?.div(1_000L)
                        ?.coerceAtMost(Int.MAX_VALUE.toLong())
                        ?.toInt()
                    session.maxPlayedSeconds = maxOf(session.maxPlayedSeconds, playedSeconds)
                    session.lastSentAtEpochSeconds = now
                    appContainer.bilibiliRepository.reportPlayHeartbeat(
                        aid = session.aid,
                        bvid = session.bvid,
                        cid = session.cid,
                        session = session.session,
                        startedAtEpochSeconds = session.startedAtEpochSeconds,
                        playedSeconds = playedSeconds,
                        maxPlayedSeconds = session.maxPlayedSeconds,
                        durationSeconds = durationSeconds,
                        playType = event.playType,
                    )
                    if (event == PlaybackHeartbeatEvent.END) activeHeartbeatSession = null
                } catch (error: Throwable) {
                    // long: 上报失败不能打断音频播放；日志只记录异常类型，不输出请求 URL、Cookie、CSRF 或账号标识。
                    val code = (error as? BilibiliApiException)?.code?.toString() ?: "none"
                    Log.w(LOG_TAG, "Playback heartbeat failed: event=${event.name}, code=$code, type=${error::class.java.simpleName}")
                    if (event == PlaybackHeartbeatEvent.END) activeHeartbeatSession = null
                }
            }
        }
    }

    private fun schedulePlaybackPreferencesPersistence() {
        playbackPreferencesPersistenceJob?.cancel()
        playbackPreferencesPersistenceJob = serviceScope.launch {
            // long: 一次模式切换会连续修改 repeat 与 shuffle，短暂合并后只写一次 DataStore，避免保存中间组合态。
            delay(PLAYBACK_PREFERENCES_PERSIST_DEBOUNCE_MS)
            val activePlayer = player ?: return@launch
            appContainer.playbackPreferenceRepository.save(
                mode = PlaybackMode.fromPlayer(activePlayer.repeatMode, activePlayer.shuffleModeEnabled),
                speed = activePlayer.playbackParameters.speed,
            )
        }
    }

    private fun schedulePlaybackQueuePersistence() {
        if (restoringPlaybackQueue) return
        queuePersistenceJob?.cancel()
        queuePersistenceJob = serviceScope.launch {
            // long: 200P 渐进补齐会连续触发 timeline 变化，短暂防抖后一次写完整快照，避免每新增一 P 都重写整表。
            delay(QUEUE_PERSIST_DEBOUNCE_MS)
            queuePersistenceJob = null
            persistPlaybackQueueNow()
        }
    }

    private fun persistPlaybackQueueNow() {
        queuePersistenceJob?.cancel()
        queuePersistenceJob = null
        val activePlayer = player ?: return
        val itemCount = activePlayer.mediaItemCount
        val snapshot = if (itemCount == 0) {
            null
        } else {
            val tracks = (0 until itemCount).mapNotNull { index ->
                activePlayer.getMediaItemAt(index).toTrackOrNull()
            }
            // long: 任一媒体项缺少 URI 或标识时保留上一份有效快照，不能用不完整列表静默覆盖用户队列。
            if (tracks.size != itemCount) return
            PlaybackQueueRecord(
                items = tracks,
                currentIndex = activePlayer.currentMediaItemIndex.coerceIn(tracks.indices),
                currentPositionMs = activePlayer.currentPosition.coerceAtLeast(0L),
            )
        }
        val generation = queuePersistenceGeneration.incrementAndGet()
        serviceScope.launch {
            queuePersistenceMutex.withLock {
                if (generation != queuePersistenceGeneration.get()) return@withLock
                if (snapshot == null) {
                    appContainer.playbackQueueRepository.clear()
                } else {
                    appContainer.playbackQueueRepository.replace(snapshot)
                }
            }
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

@UnstableApi
private fun PlaybackException.failedDashUrl(): String? {
    var current: Throwable? = this
    while (current != null) {
        if (current is HttpDataSource.InvalidResponseCodeException) {
            return current.dataSpec.uri.toString()
        }
        current = current.cause
    }
    return null
}

private fun Throwable.causeTypeNames(): List<String> {
    val typeNames = mutableListOf<String>()
    var current: Throwable? = this
    var depth = 0
    while (current != null && depth < MAX_CAUSE_CHAIN_DEPTH) {
        typeNames += current::class.java.name
        current = current.cause
        depth += 1
    }
    return typeNames
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

private data class PlaybackMediaSwitchTarget(
    val mediaId: String,
    val mediaIndex: Int,
    val bvid: String,
    val cid: Long,
    val positionMs: Long,
    val playWhenReady: Boolean,
) {
    fun matches(player: ExoPlayer): Boolean {
        val currentItem = player.currentMediaItem ?: return false
        val currentSource = currentItem.bilibiliSource() ?: return false
        return player.currentMediaItemIndex == mediaIndex &&
            currentItem.mediaId == mediaId &&
            currentSource.bvid == bvid &&
            currentSource.cid == cid
    }

    companion object {
        fun capture(player: ExoPlayer, mediaItem: MediaItem, bvid: String, cid: Long): PlaybackMediaSwitchTarget {
            return PlaybackMediaSwitchTarget(
                mediaId = mediaItem.mediaId,
                mediaIndex = player.currentMediaItemIndex,
                bvid = bvid,
                cid = cid,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                playWhenReady = player.playWhenReady,
            )
        }
    }
}

private data class PlaybackPlayerRecoverySnapshot(
    val mediaItems: List<MediaItem>,
    val currentIndex: Int,
    val currentPositionMs: Long,
    val playWhenReady: Boolean,
    val repeatMode: Int,
    val shuffleModeEnabled: Boolean,
    val playbackParameters: PlaybackParameters,
    val volume: Float,
) {
    companion object {
        fun capture(player: ExoPlayer): PlaybackPlayerRecoverySnapshot? {
            if (player.mediaItemCount == 0) return null
            val items = (0 until player.mediaItemCount).map(player::getMediaItemAt)
            return PlaybackPlayerRecoverySnapshot(
                mediaItems = items,
                currentIndex = player.currentMediaItemIndex.coerceIn(items.indices),
                currentPositionMs = player.currentPosition
                    .coerceAtLeast(0L)
                    .let { positionMs ->
                        player.duration
                            .takeIf { durationMs -> durationMs != C.TIME_UNSET && durationMs >= 0L }
                            ?.let(positionMs::coerceAtMost)
                            ?: positionMs
                    },
                playWhenReady = player.playWhenReady,
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                playbackParameters = player.playbackParameters,
                volume = player.volume,
            )
        }
    }
}

private data class ActiveHeartbeatSession(
    val mediaId: String,
    val aid: Long,
    val bvid: String,
    val cid: Long,
    val session: String,
    val startedAtEpochSeconds: Long,
    var maxPlayedSeconds: Int = 0,
    var lastSentAtEpochSeconds: Long? = null,
)

private const val LOG_TAG = "BiuPlayback"
private const val PROGRESS_PERSIST_INTERVAL_MS = 5_000L
private const val QUEUE_PERSIST_DEBOUNCE_MS = 1_000L
private const val PLAYBACK_PREFERENCES_PERSIST_DEBOUNCE_MS = 200L
private const val CODEC_SERVICE_RECOVERY_DELAY_MS = 350L
private const val MAX_CAUSE_CHAIN_DEPTH = 32
private const val MTK_DEFAULT_VIDEO_MAX_QUALITY_ID = 64
