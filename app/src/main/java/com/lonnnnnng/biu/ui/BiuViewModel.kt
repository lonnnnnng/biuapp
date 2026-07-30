package com.lonnnnnng.biu.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lonnnnnng.biu.appContainer
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.data.bilibili.AccountLibrarySection
import com.lonnnnnng.biu.data.bilibili.BilibiliAccount
import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import com.lonnnnnng.biu.data.bilibili.BilibiliFavoriteFolder
import com.lonnnnnng.biu.data.bilibili.BilibiliLibraryVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideoDetail
import com.lonnnnnng.biu.data.bilibili.CreatorFeedPolicy
import com.lonnnnnng.biu.data.bilibili.HomeFeedMode
import com.lonnnnnng.biu.data.bilibili.RecommendFeed
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity
import com.lonnnnnng.biu.data.local.AudioDownloadTaskEntity
import com.lonnnnnng.biu.data.local.LocalAudio
import com.lonnnnnng.biu.data.local.LocalAudioDownloadMetadataPolicy
import com.lonnnnnng.biu.data.local.LocalAudioDirectory
import com.lonnnnnng.biu.data.local.toTrack
import com.lonnnnnng.biu.data.update.AppUpdate
import com.lonnnnnng.biu.data.update.AppVersionPolicy
import com.lonnnnnng.biu.download.AudioDownloadRequest
import com.lonnnnnng.biu.download.AudioDownloadService
import com.lonnnnnng.biu.download.AudioDownloadStatus
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class MainSection(val label: String) {
    RECOMMEND("推荐"),
    ACCOUNT("账号"),
}

internal sealed interface PlaybackCommand {
    val queueId: Long

    data class Replace(
        override val queueId: Long,
        val tracks: List<Track>,
        val startIndex: Int = 0,
        val startPositionMs: Long = 0L,
    ) : PlaybackCommand {
        init {
            require(tracks.isNotEmpty()) { "播放队列不能为空" }
            require(startIndex in tracks.indices) { "播放起始索引越界" }
        }
    }

    data class Expand(
        override val queueId: Long,
        val placement: QueuePlacement,
        val track: Track,
    ) : PlaybackCommand
}

data class VideoPageSelection(
    val video: BilibiliVideo,
    val detail: BilibiliVideoDetail,
)

data class BiuUiState(
    val section: MainSection = MainSection.RECOMMEND,
    val feed: RecommendFeed = RecommendFeed.MUSIC,
    val recommendations: List<BilibiliVideo> = emptyList(),
    val homeFeedMode: HomeFeedMode = HomeFeedMode.FALLBACK,
    val followedCreators: List<BilibiliCreator> = emptyList(),
    val selectedCreators: List<BilibiliCreator> = emptyList(),
    val searchResults: List<BilibiliVideo> = emptyList(),
    val submittedKeyword: String = "",
    val account: BilibiliAccount = BilibiliAccount(false, "", ""),
    val librarySection: AccountLibrarySection = AccountLibrarySection.FAVORITES,
    val favoriteFolders: List<BilibiliFavoriteFolder> = emptyList(),
    val selectedFavoriteFolder: BilibiliFavoriteFolder? = null,
    val libraryVideos: List<BilibiliLibraryVideo> = emptyList(),
    val localHistory: List<PlaybackHistoryEntity> = emptyList(),
    val localAudio: List<LocalAudio> = emptyList(),
    val audioDownloads: List<AudioDownloadTaskEntity> = emptyList(),
    val localAudioDirectory: LocalAudioDirectory? = null,
    val pageSelection: VideoPageSelection? = null,
    val qualityPreference: AudioQualityPreference = AudioQualityPreference.HIGHEST,
    val isFeedLoading: Boolean = true,
    val isCreatorConfigLoading: Boolean = false,
    val isCreatorConfigSaving: Boolean = false,
    val isSearchLoading: Boolean = false,
    val isAccountLoading: Boolean = true,
    val isLibraryLoading: Boolean = false,
    val isPageQueueLoading: Boolean = false,
    val isLocalAudioLoading: Boolean = false,
    val availableUpdate: AppUpdate? = null,
    val isUpdateChecking: Boolean = false,
    val resolvingBvid: String? = null,
    val message: String? = null,
)

class BiuViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.appContainer
    private val repository = container.bilibiliRepository
    private val playbackEventIds = AtomicLong(0L)
    private val playbackQueueSnapshots = PlaybackQueueSnapshotStore<Track>(Track::id)
    private val mutableState = MutableStateFlow(BiuUiState())
    private val mutablePlaybackCommands = Channel<PlaybackCommand>(Channel.UNLIMITED)
    private var pageQueueJob: Job? = null
    private var recommendationsJob: Job? = null
    private var localAudioJob: Job? = null
    private var localAudioDirectoryInitializationJob: Job? = null
    private var localAudioDirectoryInitialized = false
    private var creatorSelectionInitialized = false

    val state: StateFlow<BiuUiState> = mutableState.asStateFlow()
    internal val playbackCommands = mutablePlaybackCommands.receiveAsFlow()

    init {
        localAudioDirectoryInitializationJob = viewModelScope.launch {
            runCatching { container.localAudioDirectoryRepository.currentDirectory() }
                .onSuccess { directory ->
                    localAudioDirectoryInitialized = true
                    mutableState.update { it.copy(localAudioDirectory = directory) }
                    if (state.value.librarySection == AccountLibrarySection.LOCAL_MUSIC) {
                        loadLocalAudio()
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    localAudioDirectoryInitialized = true
                    mutableState.update {
                        it.copy(
                            isLocalAudioLoading = false,
                            message = if (it.librarySection == AccountLibrarySection.LOCAL_MUSIC) {
                                error.userMessage("读取本地音乐目录失败")
                            } else {
                                it.message
                            },
                        )
                    }
                }
        }
        checkForUpdate(manual = false)
        refreshAccount()
        viewModelScope.launch {
            container.creatorSelectionRepository.selected.collect { selectedCreators ->
                val changed = state.value.selectedCreators != selectedCreators
                mutableState.update {
                    it.copy(
                        selectedCreators = selectedCreators,
                        homeFeedMode = CreatorFeedPolicy.modeFor(selectedCreators),
                        isCreatorConfigSaving = false,
                    )
                }
                // long: 必须等本地配置首次读取完成再选首页来源，否则冷启动会先闪现旧推荐再切换“我的关注”。
                if (!creatorSelectionInitialized || changed) {
                    creatorSelectionInitialized = true
                    loadHomeFeed(selectedCreators, state.value.feed)
                }
            }
        }
        viewModelScope.launch {
            container.playbackHistoryRepository.recent.collect { history ->
                mutableState.update { it.copy(localHistory = history) }
            }
        }
        viewModelScope.launch {
            container.audioDownloadRepository.tasks.collect { tasks ->
                mutableState.update { it.copy(audioDownloads = tasks) }
            }
        }
    }

    fun selectSection(section: MainSection) {
        mutableState.update { it.copy(section = section) }
    }

    fun loadRecommendations(feed: RecommendFeed = state.value.feed) {
        if (!creatorSelectionInitialized) return
        loadHomeFeed(state.value.selectedCreators, feed)
    }

    fun loadFollowingCreators() {
        val account = state.value.account
        if (!account.isLoggedIn || account.mid <= 0L) {
            mutableState.update { it.copy(followedCreators = emptyList(), isCreatorConfigLoading = false) }
            return
        }
        // long: 候选数据只在用户打开配置时按需读取，避免每次启动都分页扫描完整关注列表。
        mutableState.update { it.copy(isCreatorConfigLoading = true, message = null) }
        viewModelScope.launch {
            runCatching { repository.followingCreators(account.mid) }
                .onSuccess { creators ->
                    mutableState.update { it.copy(followedCreators = creators, isCreatorConfigLoading = false) }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isCreatorConfigLoading = false, message = error.userMessage("关注列表加载失败"))
                    }
                }
        }
    }

    fun saveCreatorSelection(creators: List<BilibiliCreator>) {
        if (state.value.isCreatorConfigSaving) return
        // long: 保存结果由 Room Flow 统一回推并触发首页换源，避免 UI 与数据库分别维护两套选择状态。
        mutableState.update { it.copy(isCreatorConfigSaving = true, message = null) }
        viewModelScope.launch {
            runCatching { container.creatorSelectionRepository.replaceAll(creators) }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isCreatorConfigSaving = false, message = error.userMessage("首页范围保存失败"))
                    }
                }
        }
    }

    fun search(keyword: String) {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) {
            mutableState.update { it.copy(message = "请输入搜索关键词") }
            return
        }
        mutableState.update {
            it.copy(
                submittedKeyword = normalized,
                isSearchLoading = true,
                message = null,
            )
        }
        viewModelScope.launch {
            runCatching { repository.searchVideos(normalized) }
                .onSuccess { videos ->
                    mutableState.update { it.copy(searchResults = videos, isSearchLoading = false) }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isSearchLoading = false, message = error.userMessage("搜索失败"))
                    }
                }
        }
    }

    fun clearSearch() {
        mutableState.update {
            it.copy(
                submittedKeyword = "",
                searchResults = emptyList(),
                isSearchLoading = false,
            )
        }
    }

    fun play(video: BilibiliVideo) {
        if (state.value.resolvingBvid != null || state.value.isPageQueueLoading) return
        val qualityPreference = state.value.qualityPreference
        mutableState.update { it.copy(resolvingBvid = video.bvid, message = null) }
        viewModelScope.launch {
            try {
                val detail = repository.videoDetail(video.bvid)
                if (detail.pages.size > 1) {
                    // long: 多 P 视频先交给用户选择起始页，避免继续无提示地固定播放第一 P。
                    mutableState.update {
                        it.copy(
                            resolvingBvid = null,
                            pageSelection = VideoPageSelection(video, detail),
                        )
                    }
                } else {
                    cancelPageQueueExpansion()
                    val tracks = repository.resolveTracks(video, detail, qualityPreference)
                    publishPlaybackRequest(tracks)
                }
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(resolvingBvid = null, message = error.userMessage("播放地址解析失败"))
                }
            }
        }
    }

    fun playPageQueue(startIndex: Int) {
        val selection = state.value.pageSelection ?: return
        if (state.value.isPageQueueLoading) return
        if (startIndex !in selection.detail.pages.indices) {
            mutableState.update { it.copy(message = "分 P 索引无效") }
            return
        }
        val qualityPreference = state.value.qualityPreference
        cancelPageQueueExpansion()
        mutableState.update {
            it.copy(
                resolvingBvid = selection.video.bvid,
                isPageQueueLoading = true,
                message = null,
            )
        }
        pageQueueJob = viewModelScope.launch {
            var queueStarted = false
            try {
                var queueId = 0L
                ProgressivePageQueueLoader(
                    resolve = { pageIndex ->
                        repository.resolveTrack(selection.video, selection.detail, pageIndex, qualityPreference)
                    },
                ).load(
                    pageCount = selection.detail.pages.size,
                    startIndex = startIndex,
                    onSelected = { selectedTrack ->
                        queueId = publishPlaybackRequest(listOf(selectedTrack))
                        queueStarted = true
                    },
                    onExpansion = { expansion ->
                        val command = PlaybackCommand.Expand(
                            queueId = queueId,
                            placement = expansion.placement,
                            track = expansion.item,
                        )
                        if (playbackQueueSnapshots.expand(queueId, command.placement, command.track) != null) {
                            mutablePlaybackCommands.send(command)
                        }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        resolvingBvid = null,
                        isPageQueueLoading = false,
                        message = error.userMessage(
                            if (queueStarted) "部分分 P 加载失败，当前播放不受影响" else "分 P 播放地址解析失败",
                        ),
                    )
                }
            }
        }
    }

    fun dismissPageSelection() {
        if (state.value.isPageQueueLoading) return
        mutableState.update { it.copy(pageSelection = null) }
    }

    fun play(history: PlaybackHistoryEntity) {
        if (state.value.resolvingBvid != null) return
        cancelPageQueueExpansion()
        mutableState.update { it.copy(resolvingBvid = history.bvid, message = null) }
        viewModelScope.launch {
            runCatching {
                repository.resolveTrack(history.source, history.title, history.artist, history.artworkUrl)
            }.onSuccess { track ->
                publishPlaybackRequest(
                    tracks = listOf(track),
                    startPositionMs = PlaybackResumePolicy.startPositionMs(history.lastPositionMs, history.durationMs),
                )
            }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(resolvingBvid = null, message = error.userMessage("历史播放地址解析失败"))
                    }
                }
        }
    }

    fun selectQualityPreference(preference: AudioQualityPreference) {
        mutableState.update {
            it.copy(qualityPreference = preference, message = "播放音质 · ${preference.label}（下一次播放生效）")
        }
    }

    fun refreshAccount() {
        mutableState.update { it.copy(isAccountLoading = true) }
        viewModelScope.launch {
            runCatching { repository.account() }
                .onSuccess { account ->
                    mutableState.update { it.copy(account = account, isAccountLoading = false) }
                    if (account.isLoggedIn) {
                        loadLibrary(AccountLibrarySection.FAVORITES)
                    } else {
                        clearOnlineLibrary()
                        mutableState.update { it.copy(followedCreators = emptyList(), isCreatorConfigLoading = false) }
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isAccountLoading = false, message = error.userMessage("账号状态获取失败"))
                    }
                }
        }
    }

    fun checkForUpdate(manual: Boolean = true) {
        if (state.value.isUpdateChecking) return
        mutableState.update { it.copy(isUpdateChecking = true, message = null) }
        viewModelScope.launch {
            runCatching { container.appUpdateRepository.latestRelease() }
                .onSuccess { update ->
                    val currentVersion = getApplication<Application>().packageManager
                        .getPackageInfo(getApplication<Application>().packageName, 0)
                        .versionName
                        .orEmpty()
                    val hasUpdate = AppVersionPolicy.isNewer(update.version, currentVersion)
                    mutableState.update {
                        it.copy(
                            isUpdateChecking = false,
                            availableUpdate = update.takeIf { hasUpdate },
                            message = if (manual && !hasUpdate) "已是最新版本（$currentVersion）" else null,
                        )
                    }
                }
                .onFailure { error ->
                    // long: 启动时的自动检查失败保持静默，避免网络波动干扰用户进入首页；手动检查才明确反馈。
                    mutableState.update {
                        it.copy(
                            isUpdateChecking = false,
                            message = if (manual) error.userMessage("检查更新失败") else null,
                        )
                    }
                }
        }
    }

    fun dismissUpdate() {
        mutableState.update { it.copy(availableUpdate = null) }
    }

    fun loadLibrary(section: AccountLibrarySection) {
        mutableState.update {
            it.copy(
                librarySection = section,
                isLibraryLoading = section !in setOf(
                    AccountLibrarySection.LOCAL_HISTORY,
                    AccountLibrarySection.LOCAL_MUSIC,
                ),
                selectedFavoriteFolder = if (section == AccountLibrarySection.FAVORITES) it.selectedFavoriteFolder else null,
                libraryVideos = if (section in setOf(
                        AccountLibrarySection.LOCAL_HISTORY,
                        AccountLibrarySection.LOCAL_MUSIC,
                    )
                ) {
                    it.libraryVideos
                } else {
                    emptyList()
                },
                message = null,
            )
        }
        if (section == AccountLibrarySection.LOCAL_HISTORY) return
        if (section == AccountLibrarySection.LOCAL_MUSIC) {
            loadLocalAudio()
            return
        }
        val account = state.value.account
        if (!account.isLoggedIn) {
            mutableState.update { it.copy(isLibraryLoading = false) }
            return
        }
        viewModelScope.launch {
            runCatching<Unit> {
                when (section) {
                    AccountLibrarySection.FAVORITES -> {
                        val folders = repository.favoriteFolders(account.mid)
                        mutableState.update {
                            it.copy(
                                favoriteFolders = folders,
                                selectedFavoriteFolder = null,
                                libraryVideos = emptyList(),
                                isLibraryLoading = false,
                            )
                        }
                    }
                    AccountLibrarySection.ONLINE_HISTORY -> publishLibraryVideos(repository.onlineHistory())
                    AccountLibrarySection.LOCAL_HISTORY -> Unit
                    AccountLibrarySection.LOCAL_MUSIC -> Unit
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(isLibraryLoading = false, message = error.userMessage("账号音乐库加载失败"))
                }
            }
        }
    }

    fun showLocalAudioPermission() {
        mutableState.update {
            it.copy(
                librarySection = AccountLibrarySection.LOCAL_MUSIC,
                isLibraryLoading = false,
                isLocalAudioLoading = false,
                message = null,
            )
        }
    }

    fun localAudioPermissionDenied() {
        mutableState.update {
            it.copy(
                librarySection = AccountLibrarySection.LOCAL_MUSIC,
                isLocalAudioLoading = false,
                message = "需要音频权限才能读取本机音乐",
            )
        }
    }

    fun loadLocalAudio() {
        localAudioJob?.cancel()
        if (!localAudioDirectoryInitialized) {
            mutableState.update {
                it.copy(
                    librarySection = AccountLibrarySection.LOCAL_MUSIC,
                    isLocalAudioLoading = true,
                    message = null,
                )
            }
            return
        }
        val selectedDirectory = state.value.localAudioDirectory
        mutableState.update {
            it.copy(
                librarySection = AccountLibrarySection.LOCAL_MUSIC,
                isLocalAudioLoading = true,
                message = null,
            )
        }
        localAudioJob = viewModelScope.launch {
            val downloads = state.value.audioDownloads
            runCatching {
                LocalAudioDownloadMetadataPolicy.apply(
                    audio = container.localAudioRepository.audioTracks(selectedDirectory),
                    downloads = downloads,
                )
            }
                .onSuccess { audio ->
                    mutableState.update { it.copy(localAudio = audio, isLocalAudioLoading = false) }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    mutableState.update {
                        it.copy(
                            isLocalAudioLoading = false,
                            message = error.userMessage("本地音乐扫描失败"),
                        )
                    }
                }
        }
    }

    fun selectLocalAudioDirectory(treeUri: Uri) {
        localAudioJob?.cancel()
        mutableState.update {
            it.copy(
                librarySection = AccountLibrarySection.LOCAL_MUSIC,
                isLocalAudioLoading = true,
                message = null,
            )
        }
        viewModelScope.launch {
            localAudioDirectoryInitializationJob?.join()
            runCatching { container.localAudioDirectoryRepository.selectDirectory(treeUri) }
                .onSuccess { directory ->
                    mutableState.update { it.copy(localAudioDirectory = directory) }
                    loadLocalAudio()
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    mutableState.update {
                        it.copy(
                            isLocalAudioLoading = false,
                            message = error.userMessage("目录筛选失败"),
                        )
                    }
                }
        }
    }

    fun clearLocalAudioDirectory() {
        localAudioJob?.cancel()
        mutableState.update { it.copy(isLocalAudioLoading = true, message = null) }
        viewModelScope.launch {
            localAudioDirectoryInitializationJob?.join()
            runCatching { container.localAudioDirectoryRepository.clearDirectory() }
                .onSuccess {
                    mutableState.update { it.copy(localAudioDirectory = null) }
                    loadLocalAudio()
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    mutableState.update {
                        it.copy(
                            isLocalAudioLoading = false,
                            message = error.userMessage("清除目录筛选失败"),
                        )
                    }
                }
        }
    }

    fun play(audio: LocalAudio) {
        val audioItems = state.value.localAudio
        val startIndex = audioItems.indexOfFirst { it.mediaStoreId == audio.mediaStoreId }
        if (startIndex < 0) return
        cancelPageQueueExpansion()
        // long: 点击任意本地歌曲时把当前扫描结果整体作为队列，系统上一首/下一首可直接浏览本机音乐。
        publishPlaybackRequest(
            tracks = audioItems.map(LocalAudio::toTrack),
            startIndex = startIndex,
        )
    }

    fun startAudioDownload(request: AudioDownloadRequest) {
        val existing = state.value.audioDownloads.firstOrNull { task -> task.taskId == request.taskId }
        if (existing?.downloadStatus == AudioDownloadStatus.COMPLETED) {
            mutableState.update { it.copy(message = "该曲目已下载到 Music/Biu") }
            return
        }
        AudioDownloadService.start(getApplication<Application>().applicationContext, request)
        mutableState.update {
            it.copy(
                message = if (existing?.downloadStatus == AudioDownloadStatus.PAUSED) {
                    "正在恢复音频下载"
                } else {
                    "已加入音频下载"
                },
            )
        }
    }

    fun resumeAudioDownload(taskId: String) {
        AudioDownloadService.resume(getApplication<Application>().applicationContext, taskId)
    }

    fun pauseAudioDownload(taskId: String) {
        AudioDownloadService.pause(getApplication<Application>().applicationContext, taskId)
    }

    fun cancelAudioDownload(taskId: String) {
        AudioDownloadService.cancel(getApplication<Application>().applicationContext, taskId)
    }

    fun openFavoriteFolder(folder: BilibiliFavoriteFolder) {
        mutableState.update { it.copy(selectedFavoriteFolder = folder, isLibraryLoading = true, libraryVideos = emptyList()) }
        viewModelScope.launch {
            runCatching { repository.favoriteVideos(folder.id) }
                .onSuccess { videos ->
                    mutableState.update { it.copy(libraryVideos = videos, isLibraryLoading = false) }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isLibraryLoading = false, message = error.userMessage("收藏夹内容加载失败"))
                    }
                }
        }
    }

    fun closeFavoriteFolder() {
        mutableState.update { it.copy(selectedFavoriteFolder = null, libraryVideos = emptyList()) }
    }

    fun clearLocalHistory() {
        viewModelScope.launch {
            container.playbackHistoryRepository.clear()
        }
    }

    fun logout() {
        container.cookieStore.clear {
            container.cookieStore.flush()
            refreshAccount()
        }
    }

    fun clearMessage() {
        mutableState.update { it.copy(message = null) }
    }

    internal fun isActivePlaybackQueue(queueId: Long): Boolean = playbackEventIds.get() == queueId

    internal fun currentPlaybackQueue(queueId: Long? = null): PlaybackQueueSnapshot<Track>? {
        return playbackQueueSnapshots.current(queueId)
    }

    internal fun updatePlaybackQueuePosition(mediaId: String, positionMs: Long) {
        playbackQueueSnapshots.updateResumePosition(mediaId, positionMs)
    }

    private fun publishPlaybackRequest(
        tracks: List<Track>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L,
    ): Long {
        tracks.getOrNull(startIndex) ?: error("播放起始索引越界")
        val queueId = playbackEventIds.incrementAndGet()
        playbackQueueSnapshots.replace(queueId, tracks, startIndex, startPositionMs)
        mutablePlaybackCommands.trySend(
            PlaybackCommand.Replace(
                queueId = queueId,
                tracks = tracks,
                startIndex = startIndex,
                startPositionMs = startPositionMs,
            ),
        ).getOrThrow()
        mutableState.update {
            it.copy(
                resolvingBvid = null,
                pageSelection = null,
                isPageQueueLoading = false,
                // long: 切歌是高频操作，成功状态由播放器本身呈现，不再用 Snackbar 遮挡当前内容。
                message = null,
            )
        }
        return queueId
    }

    private fun cancelPageQueueExpansion() {
        pageQueueJob?.cancel()
        pageQueueJob = null
    }

    private fun publishLibraryVideos(videos: List<BilibiliLibraryVideo>) {
        mutableState.update { it.copy(libraryVideos = videos, isLibraryLoading = false) }
    }

    private fun loadHomeFeed(selectedCreators: List<BilibiliCreator>, feed: RecommendFeed) {
        // long: 切换来源或手动刷新时取消旧请求，防止较慢的旧响应覆盖用户刚保存的新范围。
        recommendationsJob?.cancel()
        mutableState.update {
            it.copy(
                feed = if (selectedCreators.isEmpty()) feed else it.feed,
                isFeedLoading = true,
                message = null,
            )
        }
        recommendationsJob = viewModelScope.launch {
            try {
                val videos = if (selectedCreators.isEmpty()) {
                    repository.recommendations(feed)
                } else {
                    loadCreatorFeed(selectedCreators)
                }
                mutableState.update { it.copy(recommendations = videos, isFeedLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(isFeedLoading = false, message = error.userMessage("推荐加载失败"))
                }
            }
        }
    }

    private suspend fun loadCreatorFeed(creators: List<BilibiliCreator>): List<BilibiliVideo> = coroutineScope {
        val concurrency = Semaphore(3)
        // long: 关注范围可能很大，限制同时访问空间投稿接口的数量，降低触发 Bilibili 风控的概率。
        val results = creators.map { creator ->
            async {
                concurrency.withPermit {
                    try {
                        Result.success(repository.creatorVideos(creator))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Result.failure(error)
                    }
                }
            }
        }.awaitAll()
        val successfulFeeds = results.mapNotNull(Result<List<BilibiliVideo>>::getOrNull)
        if (successfulFeeds.isEmpty()) {
            throw results.firstNotNullOfOrNull(Result<List<BilibiliVideo>>::exceptionOrNull)
                ?: IllegalStateException("没有可加载的关注 UP")
        }
        CreatorFeedPolicy.merge(successfulFeeds)
    }

    private fun clearOnlineLibrary() {
        mutableState.update {
            it.copy(
                favoriteFolders = emptyList(),
                selectedFavoriteFolder = null,
                libraryVideos = emptyList(),
                isLibraryLoading = false,
            )
        }
    }
}

internal object PlaybackResumePolicy {
    fun startPositionMs(lastPositionMs: Long, durationMs: Long): Long {
        if (lastPositionMs <= 0L) return 0L
        // long: 已接近结尾的曲目从头播放，避免恢复后立刻结束；未知总时长则保留可靠的正进度。
        return if (durationMs > 0L && durationMs - lastPositionMs <= 30_000L) 0L else lastPositionMs
    }
}

private fun Throwable.userMessage(fallback: String): String {
    return message?.takeIf(String::isNotBlank)?.let { "$fallback：$it" } ?: fallback
}
