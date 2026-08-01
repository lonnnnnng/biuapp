package com.lonnnnnng.biu.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lonnnnnng.biu.appContainer
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.data.bilibili.AccountLibrarySection
import com.lonnnnnng.biu.data.bilibili.BilibiliAccount
import com.lonnnnnng.biu.data.bilibili.BilibiliApiException
import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import com.lonnnnnng.biu.data.bilibili.BilibiliCreatorRelation
import com.lonnnnnng.biu.data.bilibili.BilibiliDynamicItem
import com.lonnnnnng.biu.data.bilibili.BilibiliFavoriteFolder
import com.lonnnnnng.biu.data.bilibili.BilibiliLibraryVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliOnlineHistoryCursor
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideoDetail
import com.lonnnnnng.biu.data.bilibili.CreatorFeedPolicy
import com.lonnnnnng.biu.data.bilibili.CreatorFeedTabState
import com.lonnnnnng.biu.data.bilibili.RecommendFeed
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity
import com.lonnnnnng.biu.data.local.AudioDownloadTaskEntity
import com.lonnnnnng.biu.data.local.AppThemeMode
import com.lonnnnnng.biu.data.local.AppListDensity
import com.lonnnnnng.biu.data.local.AppTextScale
import com.lonnnnnng.biu.data.local.AppVideoLayout
import com.lonnnnnng.biu.data.local.LocalAudio
import com.lonnnnnng.biu.data.local.LocalAudioDownloadMetadataPolicy
import com.lonnnnnng.biu.data.local.LocalAudioDirectory
import com.lonnnnnng.biu.data.local.VideoDownloadTaskEntity
import com.lonnnnnng.biu.data.local.toTrack
import com.lonnnnnng.biu.data.lyrics.LrcParser
import com.lonnnnnng.biu.data.lyrics.LyricsDocument
import com.lonnnnnng.biu.data.lyrics.LyricsRateLimitedException
import com.lonnnnnng.biu.data.lyrics.LyricsSearchResult
import com.lonnnnnng.biu.data.lyrics.LyricsSource
import com.lonnnnnng.biu.data.update.AppUpdate
import com.lonnnnnng.biu.data.update.AppVersionPolicy
import com.lonnnnnng.biu.download.AudioDownloadRequest
import com.lonnnnnng.biu.download.AudioDownloadService
import com.lonnnnnng.biu.download.AudioDownloadStatus
import com.lonnnnnng.biu.download.DownloadBatchEnqueueResult
import com.lonnnnnng.biu.download.DownloadNetworkPreference
import com.lonnnnnng.biu.download.DownloadTempFilePolicy
import com.lonnnnnng.biu.download.FavoriteBatchDownloadKind
import com.lonnnnnng.biu.download.FavoriteBatchDownloadPolicy
import com.lonnnnnng.biu.download.FavoriteDownloadPage
import com.lonnnnnng.biu.download.VideoDownloadRequest
import com.lonnnnnng.biu.download.VideoDownloadService
import com.lonnnnnng.biu.download.VideoDownloadStatus
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
    DYNAMIC("动态"),
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

enum class LyricsLoadStatus {
    IDLE,
    LOADING,
    LOADED,
    EMPTY,
    ERROR,
}

data class LyricsUiState(
    val cacheKey: String = "",
    val status: LyricsLoadStatus = LyricsLoadStatus.IDLE,
    val document: LyricsDocument? = null,
    val searchResults: List<LyricsSearchResult> = emptyList(),
    val isSearchLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
    val searchErrorMessage: String? = null,
)

enum class CreatorCenterTab(val label: String) {
    SEARCH("用户搜索"),
    FOLLOWING("我的关注"),
}

data class CreatorCenterUiState(
    val tab: CreatorCenterTab = CreatorCenterTab.FOLLOWING,
    val searchKeyword: String = "",
    val searchResults: List<BilibiliCreator> = emptyList(),
    val searchNextPage: Int? = null,
    val followingCreators: List<BilibiliCreator> = emptyList(),
    val followingNextPage: Int? = null,
    val selectedCreator: BilibiliCreator? = null,
    val relation: BilibiliCreatorRelation = BilibiliCreatorRelation.UNKNOWN,
    val videos: List<BilibiliVideo> = emptyList(),
    val videosNextPage: Int? = null,
    val isListLoading: Boolean = false,
    val isListLoadingMore: Boolean = false,
    val isProfileLoading: Boolean = false,
    val isVideosLoadingMore: Boolean = false,
    val isRelationMutating: Boolean = false,
)

data class DynamicFeedUiState(
    val items: List<BilibiliDynamicItem> = emptyList(),
    val nextOffset: String? = null,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val mutatingIds: Set<String> = emptySet(),
)

data class BiuUiState(
    val section: MainSection = MainSection.RECOMMEND,
    val feed: RecommendFeed = RecommendFeed.COMPREHENSIVE,
    val recommendations: List<BilibiliVideo> = emptyList(),
    val recommendationHasMore: Boolean = false,
    val creatorFeedTabs: List<CreatorFeedTabState> = emptyList(),
    val selectedCreatorFeedMid: Long? = null,
    val followedCreators: List<BilibiliCreator> = emptyList(),
    val selectedCreators: List<BilibiliCreator> = emptyList(),
    val creatorCenter: CreatorCenterUiState = CreatorCenterUiState(),
    val dynamicFeed: DynamicFeedUiState = DynamicFeedUiState(),
    val searchResults: List<BilibiliVideo> = emptyList(),
    val submittedKeyword: String = "",
    val account: BilibiliAccount = BilibiliAccount(false, "", ""),
    val librarySection: AccountLibrarySection = AccountLibrarySection.FAVORITES,
    val createdFavoriteFolders: List<BilibiliFavoriteFolder> = emptyList(),
    val collectedFavoriteFolders: List<BilibiliFavoriteFolder> = emptyList(),
    val selectedFavoriteFolder: BilibiliFavoriteFolder? = null,
    val libraryVideos: List<BilibiliLibraryVideo> = emptyList(),
    val favoriteNextPage: Int? = null,
    val onlineHistoryQuery: String = "",
    val onlineHistoryNextCursor: BilibiliOnlineHistoryCursor? = null,
    val onlineHistoryNextSearchPage: Int? = null,
    val onlineHistoryHasMore: Boolean = false,
    val favoriteBatchFolder: BilibiliFavoriteFolder? = null,
    val favoriteBatchVideos: List<BilibiliLibraryVideo> = emptyList(),
    val localHistory: List<PlaybackHistoryEntity> = emptyList(),
    val localAudio: List<LocalAudio> = emptyList(),
    val audioDownloads: List<AudioDownloadTaskEntity> = emptyList(),
    val videoDownloads: List<VideoDownloadTaskEntity> = emptyList(),
    val downloadNetworkPreference: DownloadNetworkPreference = DownloadNetworkPreference.ANY_VALIDATED,
    val localAudioDirectory: LocalAudioDirectory? = null,
    val pageSelection: VideoPageSelection? = null,
    val lyrics: LyricsUiState = LyricsUiState(),
    val qualityPreference: AudioQualityPreference = AudioQualityPreference.HIGHEST,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val listDensity: AppListDensity = AppListDensity.STANDARD,
    val textScale: AppTextScale = AppTextScale.STANDARD,
    val videoLayout: AppVideoLayout = AppVideoLayout.LIST,
    val reportPlayHistory: Boolean = true,
    val isFeedLoading: Boolean = true,
    val isFeedLoadingMore: Boolean = false,
    val isCreatorConfigLoading: Boolean = false,
    val isCreatorConfigSaving: Boolean = false,
    val isSearchLoading: Boolean = false,
    val isAccountLoading: Boolean = true,
    val isLibraryLoading: Boolean = false,
    val isFavoriteLoadingMore: Boolean = false,
    val isFavoriteMutating: Boolean = false,
    val isOnlineHistoryLoadingMore: Boolean = false,
    val isOnlineHistoryMutating: Boolean = false,
    val isFavoriteBatchLoading: Boolean = false,
    val isFavoriteBatchSubmitting: Boolean = false,
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
    private var recommendationNextPage: Int? = null
    private var recommendationGeneration: Long = 0L
    private var favoriteBatchLoadJob: Job? = null
    private var favoriteFolderJob: Job? = null
    private var onlineHistoryJob: Job? = null
    private var localAudioJob: Job? = null
    private var localAudioDirectoryInitializationJob: Job? = null
    private var creatorListJob: Job? = null
    private var creatorProfileJob: Job? = null
    private var creatorRelationJob: Job? = null
    private var dynamicFeedJob: Job? = null
    private var lyricsLoadJob: Job? = null
    private var lyricsSearchJob: Job? = null
    private var lyricsSaveJob: Job? = null
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
        viewModelScope.launch {
            container.videoDownloadRepository.tasks.collect { tasks ->
                mutableState.update { it.copy(videoDownloads = tasks) }
            }
        }
        viewModelScope.launch {
            container.downloadNetworkPreferenceRepository.preference.collect { preference ->
                mutableState.update { it.copy(downloadNetworkPreference = preference) }
            }
        }
        viewModelScope.launch {
            container.playbackPreferenceRepository.preferences.collect { preferences ->
                mutableState.update { it.copy(reportPlayHistory = preferences.reportPlayHistory) }
            }
        }
        viewModelScope.launch {
            container.themePreferenceRepository.mode.collect { mode ->
                mutableState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            container.displayPreferenceRepository.preferences.collect { preferences ->
                mutableState.update {
                    it.copy(
                        listDensity = preferences.listDensity,
                        textScale = preferences.textScale,
                        videoLayout = preferences.videoLayout,
                    )
                }
            }
        }
        viewModelScope.launch {
            container.audioDownloadRecovery.await()
            if (container.audioDownloadRepository.nextQueued() != null) {
                AudioDownloadService.drain(getApplication<Application>().applicationContext)
            }
        }
        viewModelScope.launch {
            container.videoDownloadRecovery.await()
            if (container.videoDownloadRepository.nextQueued() != null) {
                VideoDownloadService.drain(getApplication<Application>().applicationContext)
            }
        }
    }

    fun selectSection(section: MainSection) {
        mutableState.update { it.copy(section = section) }
        if (
            section == MainSection.DYNAMIC &&
            state.value.account.isLoggedIn &&
            state.value.dynamicFeed.items.isEmpty() &&
            !state.value.dynamicFeed.isLoading
        ) {
            loadDynamicFeed(reset = true)
        }
    }

    fun loadDynamicFeed(reset: Boolean = true) {
        val current = state.value
        if (!current.account.isLoggedIn) {
            mutableState.update { it.copy(dynamicFeed = DynamicFeedUiState()) }
            return
        }
        val feed = current.dynamicFeed
        if (feed.isLoading || feed.isLoadingMore) return
        val offset = if (reset) null else feed.nextOffset ?: return
        if (reset) dynamicFeedJob?.cancel()
        mutableState.update { state ->
            state.copy(
                dynamicFeed = state.dynamicFeed.copy(
                    items = state.dynamicFeed.items,
                    nextOffset = state.dynamicFeed.nextOffset,
                    hasMore = state.dynamicFeed.hasMore,
                    isLoading = reset,
                    isLoadingMore = !reset,
                ),
                message = null,
            )
        }
        dynamicFeedJob = viewModelScope.launch {
            try {
                val page = repository.dynamicFeed(offset)
                mutableState.update { state ->
                    val items = if (reset) {
                        page.items
                    } else {
                        (state.dynamicFeed.items + page.items).distinctBy(BilibiliDynamicItem::id)
                    }
                    state.copy(
                        dynamicFeed = state.dynamicFeed.copy(
                            items = items,
                            nextOffset = page.nextOffset,
                            hasMore = page.hasMore && page.nextOffset != null,
                            isLoading = false,
                            isLoadingMore = false,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { state ->
                    state.copy(
                        dynamicFeed = state.dynamicFeed.copy(isLoading = false, isLoadingMore = false),
                        message = DynamicInteractionPolicy.errorMessage(error, "动态加载失败"),
                    )
                }
            }
        }
    }

    fun loadMoreDynamicFeed() {
        loadDynamicFeed(reset = false)
    }

    fun toggleDynamicLike(item: BilibiliDynamicItem) {
        val current = state.value
        if (!current.account.isLoggedIn) {
            mutableState.update { it.copy(message = "登录后才能点赞动态") }
            return
        }
        val latest = current.dynamicFeed.items.firstOrNull { dynamic -> dynamic.id == item.id } ?: return
        if (latest.isLikeForbidden || latest.id in current.dynamicFeed.mutatingIds) return
        val mutation = DynamicInteractionPolicy.beginLikeMutation(latest)
        mutableState.update { state ->
            state.copy(
                dynamicFeed = state.dynamicFeed.copy(
                    items = state.dynamicFeed.items.replaceDynamic(mutation.optimistic),
                    mutatingIds = state.dynamicFeed.mutatingIds + latest.id,
                ),
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                repository.updateDynamicLike(latest.id, mutation.optimistic.isLiked)
                mutableState.update { state ->
                    state.copy(
                        dynamicFeed = state.dynamicFeed.copy(
                            mutatingIds = state.dynamicFeed.mutatingIds - latest.id,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { state ->
                    val visibleItem = state.dynamicFeed.items.firstOrNull { dynamic -> dynamic.id == latest.id }
                    // long: 刷新动态可能先于失败响应完成；只回滚仍处于本次乐观快照的卡片，避免旧请求覆盖更新后的服务端状态。
                    val items = if (visibleItem == mutation.optimistic) {
                        state.dynamicFeed.items.replaceDynamic(mutation.rollback())
                    } else {
                        state.dynamicFeed.items
                    }
                    state.copy(
                        dynamicFeed = state.dynamicFeed.copy(
                            items = items,
                            mutatingIds = state.dynamicFeed.mutatingIds - latest.id,
                        ),
                        message = DynamicInteractionPolicy.errorMessage(error, "动态点赞失败"),
                    )
                }
            }
        }
    }

    fun tripleDynamic(item: BilibiliDynamicItem) {
        val current = state.value
        if (!current.account.isLoggedIn) {
            mutableState.update { it.copy(message = "登录后才能一键三连") }
            return
        }
        val latest = current.dynamicFeed.items.firstOrNull { dynamic -> dynamic.id == item.id } ?: return
        if (latest.id in current.dynamicFeed.mutatingIds) return
        mutableState.update { state ->
            state.copy(
                dynamicFeed = state.dynamicFeed.copy(
                    mutatingIds = state.dynamicFeed.mutatingIds + latest.id,
                ),
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                val result = repository.tripleLike(latest.video.bvid)
                mutableState.update { state ->
                    val visibleItem = state.dynamicFeed.items.firstOrNull { dynamic -> dynamic.id == latest.id }
                    state.copy(
                        dynamicFeed = state.dynamicFeed.copy(
                            items = visibleItem?.let { currentItem ->
                                state.dynamicFeed.items.replaceDynamic(
                                    DynamicInteractionPolicy.applyTriple(currentItem, result),
                                )
                            } ?: state.dynamicFeed.items,
                            mutatingIds = state.dynamicFeed.mutatingIds - latest.id,
                        ),
                        message = buildString {
                            append("三连完成")
                            if (result.coined && result.coinCount > 0) append(" · ${result.coinCount} 枚硬币")
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { state ->
                    state.copy(
                        dynamicFeed = state.dynamicFeed.copy(
                            mutatingIds = state.dynamicFeed.mutatingIds - latest.id,
                        ),
                        message = DynamicInteractionPolicy.errorMessage(error, "一键三连失败"),
                    )
                }
            }
        }
    }

    fun prepareLyrics(source: BilibiliTrackSource?, mediaId: String) {
        if (mediaId.isBlank()) {
            lyricsLoadJob?.cancel()
            lyricsSearchJob?.cancel()
            lyricsSaveJob?.cancel()
            mutableState.update { it.copy(lyrics = LyricsUiState()) }
            return
        }
        val cacheKey = source?.let { "${it.bvid}:${it.cid}" } ?: "media:$mediaId"
        if (state.value.lyrics.cacheKey == cacheKey) {
            updateLyrics(cacheKey) { lyrics ->
                lyrics.copy(
                    searchResults = emptyList(),
                    hasSearched = false,
                    searchErrorMessage = null,
                )
            }
            return
        }
        lyricsLoadJob?.cancel()
        lyricsSearchJob?.cancel()
        lyricsSaveJob?.cancel()
        mutableState.update {
            it.copy(
                lyrics = LyricsUiState(
                    cacheKey = cacheKey,
                    status = LyricsLoadStatus.LOADING,
                ),
            )
        }
        lyricsLoadJob = viewModelScope.launch {
            try {
                val cached = container.lyricsCacheRepository.find(cacheKey)
                if (cached != null) {
                    updateLyrics(cacheKey) { lyrics ->
                        lyrics.copy(status = LyricsLoadStatus.LOADED, document = cached)
                    }
                    return@launch
                }
                // long: 远端歌词只能由用户确认搜索后请求；这里仅检查本地缓存，避免打开播放页就访问第三方服务。
                updateLyrics(cacheKey) { lyrics -> lyrics.copy(status = LyricsLoadStatus.IDLE) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                updateLyrics(cacheKey) { lyrics ->
                    lyrics.copy(
                        status = LyricsLoadStatus.ERROR,
                        errorMessage = error.userMessage("歌词缓存读取失败"),
                    )
                }
            }
        }
    }

    fun searchLyrics(query: String) {
        val cacheKey = state.value.lyrics.cacheKey
        val normalizedQuery = query.trim()
        if (cacheKey.isBlank() || normalizedQuery.isBlank()) return
        lyricsSearchJob?.cancel()
        updateLyrics(cacheKey) { lyrics ->
            lyrics.copy(
                searchResults = emptyList(),
                isSearchLoading = true,
                hasSearched = true,
                searchErrorMessage = null,
            )
        }
        lyricsSearchJob = viewModelScope.launch {
            try {
                val results = container.lrclibRepository.search(normalizedQuery)
                updateLyrics(cacheKey) { lyrics ->
                    lyrics.copy(searchResults = results, isSearchLoading = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val message = if (error is LyricsRateLimitedException) {
                    error.retryAfterSeconds
                        ?.let { seconds -> "请求过于频繁，请 $seconds 秒后重试" }
                        ?: "请求过于频繁，请稍后重试"
                } else {
                    error.userMessage("歌词搜索失败")
                }
                updateLyrics(cacheKey) { lyrics ->
                    lyrics.copy(isSearchLoading = false, searchErrorMessage = message)
                }
            }
        }
    }

    fun selectLyrics(result: LyricsSearchResult) {
        val cacheKey = state.value.lyrics.cacheKey
        if (cacheKey.isBlank()) return
        val lines = LrcParser.parse(result.syncedLyrics)
        if (lines.isEmpty()) {
            updateLyrics(cacheKey) { lyrics -> lyrics.copy(searchErrorMessage = "这条结果不包含有效时间轴") }
            return
        }
        val document = LyricsDocument(
            cacheKey = cacheKey,
            source = LyricsSource.LRCLIB,
            lines = lines,
            rawLyrics = result.syncedLyrics,
            providerId = result.id,
            trackName = result.trackName,
            artistName = result.artistName,
            isUserSelected = true,
        )
        lyricsLoadJob?.cancel()
        lyricsSaveJob?.cancel()
        lyricsSaveJob = viewModelScope.launch {
            try {
                container.lyricsCacheRepository.save(document)
                updateLyrics(cacheKey) { lyrics ->
                    lyrics.copy(
                        status = LyricsLoadStatus.LOADED,
                        document = document,
                        searchResults = emptyList(),
                        hasSearched = false,
                        searchErrorMessage = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                updateLyrics(cacheKey) { lyrics ->
                    lyrics.copy(searchErrorMessage = error.userMessage("歌词保存失败"))
                }
            }
        }
    }

    private fun updateLyrics(cacheKey: String, transform: (LyricsUiState) -> LyricsUiState) {
        mutableState.update { current ->
            // long: 切歌后旧网络响应可能迟到，只有请求键仍对应当前 bvid/cid 才允许写回界面状态。
            if (current.lyrics.cacheKey != cacheKey) current else current.copy(lyrics = transform(current.lyrics))
        }
    }

    fun loadRecommendations(feed: RecommendFeed = state.value.feed) {
        if (!creatorSelectionInitialized) return
        val current = state.value
        if (current.selectedCreators.isEmpty()) {
            loadHomeFeed(
                selectedCreators = emptyList(),
                feed = feed,
                preserveCurrentVideos = feed == current.feed && current.recommendations.isNotEmpty(),
            )
        } else {
            current.selectedCreatorFeedMid?.let { mid -> loadCreatorFeedTab(mid, append = false) }
        }
    }

    fun selectCreatorFeed(mid: Long) {
        val current = state.value
        if (current.selectedCreatorFeedMid == mid) return
        val tab = current.creatorFeedTabs.firstOrNull { item -> item.creator.mid == mid } ?: return
        if (recommendationsJob?.isActive == true) {
            recommendationsJob?.cancel()
            recommendationsJob = null
            recommendationGeneration += 1L
        }
        mutableState.update {
            it.copy(
                selectedCreatorFeedMid = mid,
                creatorFeedTabs = it.creatorFeedTabs.map { item ->
                    item.copy(isLoading = false, isLoadingMore = false)
                },
            )
        }
        if (!tab.hasLoaded) {
            loadCreatorFeedTab(mid, append = false)
        }
    }

    fun loadMoreRecommendations() {
        val current = state.value
        if (current.selectedCreators.isNotEmpty()) {
            val mid = current.selectedCreatorFeedMid ?: return
            val tab = current.creatorFeedTabs.firstOrNull { item -> item.creator.mid == mid } ?: return
            if (!tab.hasMore || tab.isLoading || tab.isLoadingMore || recommendationsJob?.isActive == true) return
            loadCreatorFeedTab(mid, append = true)
            return
        }
        if (
            !creatorSelectionInitialized ||
            current.isFeedLoading ||
            current.isFeedLoadingMore ||
            !current.recommendationHasMore ||
            recommendationsJob?.isActive == true
        ) {
            return
        }
        val generation = recommendationGeneration
        val feed = current.feed
        mutableState.update { it.copy(isFeedLoadingMore = true, message = null) }
        recommendationsJob = viewModelScope.launch {
            try {
                val page = recommendationNextPage ?: return@launch finishRecommendationPagination(generation)
                val result = repository.recommendations(feed, page)
                if (generation != recommendationGeneration) return@launch
                recommendationNextPage = if (result.hasMore) result.page + 1 else null
                mutableState.update { latest ->
                    latest.copy(
                        recommendations = (latest.recommendations + result.videos)
                            .distinctBy(BilibiliVideo::bvid),
                        recommendationHasMore = result.hasMore,
                        isFeedLoadingMore = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (generation == recommendationGeneration) {
                    mutableState.update {
                        it.copy(
                            isFeedLoadingMore = false,
                            message = error.userMessage("更多推荐加载失败"),
                        )
                    }
                }
            }
        }
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

    fun selectCreatorCenterTab(tab: CreatorCenterTab) {
        if (state.value.creatorCenter.tab != tab) creatorListJob?.cancel()
        mutableState.update { current ->
            current.copy(
                creatorCenter = current.creatorCenter.copy(
                    tab = tab,
                    isListLoading = false,
                    isListLoadingMore = false,
                ),
            )
        }
        if (tab == CreatorCenterTab.FOLLOWING && state.value.creatorCenter.followingCreators.isEmpty()) {
            loadCreatorCenterFollowing(reset = true)
        }
    }

    fun searchCreators(keyword: String, loadMore: Boolean = false) {
        val normalizedKeyword = if (loadMore) state.value.creatorCenter.searchKeyword else keyword.trim()
        if (normalizedKeyword.isBlank()) {
            mutableState.update { it.copy(message = "请输入 UP 主名称") }
            return
        }
        val nextPage = if (loadMore) state.value.creatorCenter.searchNextPage else 1
        if (nextPage == null || state.value.creatorCenter.isListLoading || state.value.creatorCenter.isListLoadingMore) return
        if (!loadMore) creatorListJob?.cancel()
        mutableState.update { current ->
            current.copy(
                creatorCenter = current.creatorCenter.copy(
                    tab = CreatorCenterTab.SEARCH,
                    searchKeyword = normalizedKeyword,
                    searchResults = if (loadMore) current.creatorCenter.searchResults else emptyList(),
                    searchNextPage = if (loadMore) current.creatorCenter.searchNextPage else null,
                    isListLoading = !loadMore,
                    isListLoadingMore = loadMore,
                ),
                message = null,
            )
        }
        creatorListJob = viewModelScope.launch {
            try {
                val page = repository.searchCreators(normalizedKeyword, nextPage)
                mutableState.update { current ->
                    if (current.creatorCenter.searchKeyword != normalizedKeyword) return@update current
                    val creators = if (loadMore) {
                        (current.creatorCenter.searchResults + page.creators).distinctBy(BilibiliCreator::mid)
                    } else {
                        page.creators
                    }
                    current.copy(
                        creatorCenter = current.creatorCenter.copy(
                            searchResults = creators,
                            searchNextPage = (page.page + 1).takeIf { page.hasMore },
                            isListLoading = false,
                            isListLoadingMore = false,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { current ->
                    current.copy(
                        creatorCenter = current.creatorCenter.copy(
                            isListLoading = false,
                            isListLoadingMore = false,
                        ),
                        message = error.userMessage("UP 主搜索失败"),
                    )
                }
            }
        }
    }

    fun clearCreatorSearch() {
        creatorListJob?.cancel()
        mutableState.update { current ->
            current.copy(
                creatorCenter = current.creatorCenter.copy(
                    searchKeyword = "",
                    searchResults = emptyList(),
                    searchNextPage = null,
                    isListLoading = false,
                    isListLoadingMore = false,
                ),
            )
        }
    }

    fun loadCreatorCenterFollowing(reset: Boolean = false) {
        val account = state.value.account
        if (!account.isLoggedIn || account.mid <= 0L) {
            mutableState.update { current ->
                current.copy(
                    creatorCenter = current.creatorCenter.copy(
                        followingCreators = emptyList(),
                        followingNextPage = null,
                        isListLoading = false,
                        isListLoadingMore = false,
                    ),
                )
            }
            return
        }
        val nextPage = if (reset) 1 else state.value.creatorCenter.followingNextPage ?: return
        if (state.value.creatorCenter.isListLoading || state.value.creatorCenter.isListLoadingMore) return
        if (reset) creatorListJob?.cancel()
        mutableState.update { current ->
            current.copy(
                creatorCenter = current.creatorCenter.copy(
                    tab = CreatorCenterTab.FOLLOWING,
                    followingCreators = if (reset) emptyList() else current.creatorCenter.followingCreators,
                    followingNextPage = if (reset) null else current.creatorCenter.followingNextPage,
                    isListLoading = reset,
                    isListLoadingMore = !reset,
                ),
                message = null,
            )
        }
        creatorListJob = viewModelScope.launch {
            try {
                val page = repository.followingCreatorsPage(account.mid, nextPage)
                mutableState.update { current ->
                    val creators = if (reset) {
                        page.creators
                    } else {
                        (current.creatorCenter.followingCreators + page.creators).distinctBy(BilibiliCreator::mid)
                    }
                    current.copy(
                        creatorCenter = current.creatorCenter.copy(
                            followingCreators = creators,
                            followingNextPage = (page.page + 1).takeIf { page.hasMore },
                            isListLoading = false,
                            isListLoadingMore = false,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { current ->
                    current.copy(
                        creatorCenter = current.creatorCenter.copy(
                            isListLoading = false,
                            isListLoadingMore = false,
                        ),
                        message = error.userMessage("关注列表加载失败"),
                    )
                }
            }
        }
    }

    fun openCreatorProfile(creator: BilibiliCreator) {
        creatorProfileJob?.cancel()
        mutableState.update { current ->
            current.copy(
                creatorCenter = current.creatorCenter.copy(
                    selectedCreator = creator,
                    relation = BilibiliCreatorRelation.UNKNOWN,
                    videos = emptyList(),
                    videosNextPage = null,
                    isProfileLoading = true,
                    isVideosLoadingMore = false,
                    isRelationMutating = false,
                ),
                message = null,
            )
        }
        creatorProfileJob = viewModelScope.launch {
            try {
                // long: 资料、投稿与关系互不依赖，并行读取可明显缩短进入空间后的首屏等待时间。
                val profileDeferred = async { runCatching { repository.creatorProfile(creator.mid) } }
                val videosDeferred = async { runCatching { repository.creatorVideoPage(creator, page = 1) } }
                val relationDeferred = async {
                    if (state.value.account.isLoggedIn) runCatching { repository.creatorRelation(creator.mid) }
                    else Result.success(BilibiliCreatorRelation.NONE)
                }
                val profileResult = profileDeferred.await()
                val videosResult = videosDeferred.await()
                val relationResult = relationDeferred.await()
                listOf(profileResult.exceptionOrNull(), videosResult.exceptionOrNull(), relationResult.exceptionOrNull())
                    .filterIsInstance<CancellationException>()
                    .firstOrNull()
                    ?.let { throw it }
                val page = videosResult.getOrNull()
                val loadedProfile = profileResult.getOrNull()?.copy(
                    followerCount = creator.followerCount,
                    videoCount = page?.total ?: creator.videoCount,
                ) ?: creator.copy(videoCount = page?.total ?: creator.videoCount)
                mutableState.update { current ->
                    if (current.creatorCenter.selectedCreator?.mid != creator.mid) return@update current
                    current.copy(
                        creatorCenter = current.creatorCenter.copy(
                            selectedCreator = loadedProfile,
                            relation = relationResult.getOrDefault(BilibiliCreatorRelation.UNKNOWN),
                            videos = page?.videos.orEmpty(),
                            videosNextPage = page?.let { result -> (result.page + 1).takeIf { result.hasMore } },
                            isProfileLoading = false,
                        ),
                        message = when {
                            profileResult.isFailure && videosResult.isFailure -> "UP 主空间加载失败"
                            state.value.account.isLoggedIn && relationResult.isFailure -> "关注状态读取失败，请点击按钮重试"
                            else -> current.message
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { current ->
                    current.copy(
                        creatorCenter = current.creatorCenter.copy(isProfileLoading = false),
                        message = error.userMessage("UP 主空间加载失败"),
                    )
                }
            }
        }
    }

    fun closeCreatorProfile() {
        creatorProfileJob?.cancel()
        creatorRelationJob?.cancel()
        mutableState.update { current ->
            current.copy(
                creatorCenter = current.creatorCenter.copy(
                    selectedCreator = null,
                    relation = BilibiliCreatorRelation.UNKNOWN,
                    videos = emptyList(),
                    videosNextPage = null,
                    isProfileLoading = false,
                    isVideosLoadingMore = false,
                    isRelationMutating = false,
                ),
            )
        }
    }

    fun loadMoreCreatorVideos() {
        val center = state.value.creatorCenter
        val creator = center.selectedCreator ?: return
        val nextPage = center.videosNextPage ?: return
        if (center.isProfileLoading || center.isVideosLoadingMore) return
        mutableState.update { current ->
            current.copy(creatorCenter = current.creatorCenter.copy(isVideosLoadingMore = true), message = null)
        }
        creatorProfileJob = viewModelScope.launch {
            try {
                val page = repository.creatorVideoPage(creator, nextPage)
                mutableState.update { current ->
                    if (current.creatorCenter.selectedCreator?.mid != creator.mid) return@update current
                    current.copy(
                        creatorCenter = current.creatorCenter.copy(
                            videos = (current.creatorCenter.videos + page.videos).distinctBy(BilibiliVideo::bvid),
                            videosNextPage = (page.page + 1).takeIf { page.hasMore },
                            isVideosLoadingMore = false,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { current ->
                    current.copy(
                        creatorCenter = current.creatorCenter.copy(isVideosLoadingMore = false),
                        message = error.userMessage("更多投稿加载失败"),
                    )
                }
            }
        }
    }

    fun toggleCreatorRelation() {
        val current = state.value
        val creator = current.creatorCenter.selectedCreator ?: return
        if (!current.account.isLoggedIn) {
            mutableState.update { it.copy(message = "登录后才能关注 UP 主") }
            return
        }
        if (current.creatorCenter.relation == BilibiliCreatorRelation.BLOCKED || current.creatorCenter.isRelationMutating) return
        if (current.creatorCenter.relation == BilibiliCreatorRelation.UNKNOWN) {
            mutableState.update { state ->
                state.copy(creatorCenter = state.creatorCenter.copy(isRelationMutating = true), message = null)
            }
            creatorRelationJob = viewModelScope.launch {
                try {
                    val relation = repository.creatorRelation(creator.mid)
                    mutableState.update { state ->
                        state.copy(
                            creatorCenter = state.creatorCenter.copy(
                                relation = relation,
                                isRelationMutating = false,
                            ),
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    mutableState.update { state ->
                        state.copy(
                            creatorCenter = state.creatorCenter.copy(isRelationMutating = false),
                            message = error.userMessage("关注状态读取失败"),
                        )
                    }
                }
            }
            return
        }
        val following = !current.creatorCenter.relation.isFollowing
        mutableState.update { state ->
            state.copy(creatorCenter = state.creatorCenter.copy(isRelationMutating = true), message = null)
        }
        creatorRelationJob = viewModelScope.launch {
            try {
                repository.modifyCreatorRelation(creator.mid, following)
                val confirmedRelation = runCatching { repository.creatorRelation(creator.mid) }.getOrNull()
                // long: 关系查询可能短暂读到写前缓存，写接口成功时以用户刚执行的动作收敛按钮状态。
                val relation = when {
                    following && confirmedRelation?.isFollowing != true -> BilibiliCreatorRelation.FOLLOWING
                    !following && confirmedRelation?.isFollowing == true -> BilibiliCreatorRelation.NONE
                    else -> confirmedRelation ?: if (following) BilibiliCreatorRelation.FOLLOWING else BilibiliCreatorRelation.NONE
                }
                val selectionSyncError = if (!following) {
                    // long: 取关后首页不能继续保留已经失效的 UP 范围，远端成功后立即同步清理 Room 中对应 UID。
                    try {
                        container.creatorSelectionRepository.replaceAll(
                            state.value.selectedCreators.filterNot { selected -> selected.mid == creator.mid },
                        )
                        null
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        error
                    }
                } else null
                mutableState.update { state ->
                    val centerCreators = if (following) {
                        (listOf(creator) + state.creatorCenter.followingCreators).distinctBy(BilibiliCreator::mid)
                    } else {
                        state.creatorCenter.followingCreators.filterNot { followed -> followed.mid == creator.mid }
                    }
                    val configCreators = if (following) {
                        (state.followedCreators + creator).distinctBy(BilibiliCreator::mid)
                    } else {
                        state.followedCreators.filterNot { followed -> followed.mid == creator.mid }
                    }
                    state.copy(
                        followedCreators = configCreators,
                        creatorCenter = state.creatorCenter.copy(
                            followingCreators = centerCreators,
                            relation = relation,
                            isRelationMutating = false,
                        ),
                        message = when {
                            following -> "已关注 ${creator.name}"
                            selectionSyncError != null -> selectionSyncError.userMessage("已取消关注，但首页范围同步失败")
                            else -> "已取消关注 ${creator.name}"
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { state ->
                    state.copy(
                        creatorCenter = state.creatorCenter.copy(isRelationMutating = false),
                        message = error.userMessage(if (following) "关注失败" else "取消关注失败"),
                    )
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

    fun selectThemeMode(mode: AppThemeMode) {
        val previousMode = state.value.themeMode
        if (previousMode == mode) return
        mutableState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            runCatching { container.themePreferenceRepository.save(mode) }
                .onFailure { error ->
                    mutableState.update { current ->
                        // long: 快速连续切换时，旧保存任务失败不能覆盖用户随后选择的新主题。
                        if (current.themeMode != mode) {
                            current
                        } else {
                            current.copy(
                                themeMode = previousMode,
                                message = error.userMessage("保存主题设置失败"),
                            )
                        }
                    }
                }
        }
    }

    fun selectListDensity(density: AppListDensity) {
        val previousDensity = state.value.listDensity
        if (previousDensity == density) return
        mutableState.update { it.copy(listDensity = density) }
        viewModelScope.launch {
            runCatching { container.displayPreferenceRepository.saveListDensity(density) }
                .onFailure { error ->
                    mutableState.update { current ->
                        // long: 连续切换显示密度时，早先失败的写入不能撤销用户刚做出的新选择。
                        if (current.listDensity != density) {
                            current
                        } else {
                            current.copy(
                                listDensity = previousDensity,
                                message = error.userMessage("保存列表密度失败"),
                            )
                        }
                    }
                }
        }
    }

    fun selectTextScale(scale: AppTextScale) {
        val previousScale = state.value.textScale
        if (previousScale == scale) return
        mutableState.update { it.copy(textScale = scale) }
        viewModelScope.launch {
            runCatching { container.displayPreferenceRepository.saveTextScale(scale) }
                .onFailure { error ->
                    mutableState.update { current ->
                        // long: 字号与密度独立保存；只在当前选择仍是失败目标时回滚，避免覆盖后续选择。
                        if (current.textScale != scale) {
                            current
                        } else {
                            current.copy(
                                textScale = previousScale,
                                message = error.userMessage("保存字体大小失败"),
                            )
                        }
                    }
                }
        }
    }

    fun selectVideoLayout(layout: AppVideoLayout) {
        val previousLayout = state.value.videoLayout
        if (previousLayout == layout) return
        mutableState.update { it.copy(videoLayout = layout) }
        viewModelScope.launch {
            runCatching { container.displayPreferenceRepository.saveVideoLayout(layout) }
                .onFailure { error ->
                    mutableState.update { current ->
                        // long: 推荐布局可快速来回切换，旧请求失败时只能回滚仍停留在失败目标的当前选择。
                        if (current.videoLayout != layout) {
                            current
                        } else {
                            current.copy(
                                videoLayout = previousLayout,
                                message = error.userMessage("保存推荐布局失败"),
                            )
                        }
                    }
                }
        }
    }

    fun setReportPlayHistory(enabled: Boolean) {
        mutableState.update { it.copy(reportPlayHistory = enabled) }
        viewModelScope.launch {
            runCatching { container.playbackPreferenceRepository.saveReportPlayHistory(enabled) }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(reportPlayHistory = !enabled, message = error.userMessage("保存播放历史设置失败"))
                    }
                }
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
                        if (state.value.section == MainSection.DYNAMIC) loadDynamicFeed(reset = true)
                        state.value.creatorCenter.selectedCreator?.let(::openCreatorProfile)
                    } else {
                        clearOnlineLibrary()
                        mutableState.update { current ->
                            current.copy(
                                followedCreators = emptyList(),
                                creatorCenter = current.creatorCenter.copy(
                                    followingCreators = emptyList(),
                                    followingNextPage = null,
                                    relation = BilibiliCreatorRelation.NONE,
                                    isListLoading = false,
                                    isListLoadingMore = false,
                                    isRelationMutating = false,
                                ),
                                dynamicFeed = DynamicFeedUiState(),
                                isCreatorConfigLoading = false,
                            )
                        }
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
                    AccountLibrarySection.DOWNLOADS,
                ),
                selectedFavoriteFolder = if (section == AccountLibrarySection.FAVORITES) it.selectedFavoriteFolder else null,
                favoriteNextPage = if (section == AccountLibrarySection.FAVORITES) it.favoriteNextPage else null,
                isFavoriteLoadingMore = false,
                libraryVideos = if (section in setOf(
                        AccountLibrarySection.LOCAL_HISTORY,
                        AccountLibrarySection.LOCAL_MUSIC,
                        AccountLibrarySection.DOWNLOADS,
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
        if (section == AccountLibrarySection.DOWNLOADS) return
        if (section == AccountLibrarySection.LOCAL_MUSIC) {
            loadLocalAudio()
            return
        }
        if (section == AccountLibrarySection.ONLINE_HISTORY) {
            loadOnlineHistory(reset = true)
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
                        // long: 两个收藏分组来自独立接口，并行加载可避免“我收藏的”拖慢整个账号页首屏。
                        val (createdFolders, collectedFolders) = favoriteFolders(account.mid)
                        mutableState.update {
                            it.copy(
                                createdFavoriteFolders = createdFolders,
                                collectedFavoriteFolders = collectedFolders,
                                selectedFavoriteFolder = null,
                                libraryVideos = emptyList(),
                                favoriteNextPage = null,
                                isLibraryLoading = false,
                            )
                        }
                    }
                    AccountLibrarySection.ONLINE_HISTORY -> Unit
                    AccountLibrarySection.LOCAL_HISTORY -> Unit
                    AccountLibrarySection.LOCAL_MUSIC -> Unit
                    AccountLibrarySection.DOWNLOADS -> Unit
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

    fun startVideoDownload(request: VideoDownloadRequest) {
        val existing = state.value.videoDownloads.firstOrNull { task -> task.taskId == request.taskId }
        if (existing?.downloadStatus == VideoDownloadStatus.COMPLETED) {
            mutableState.update { it.copy(message = "该分 P 视频已下载到 Movies/Biu") }
            return
        }
        VideoDownloadService.start(getApplication<Application>().applicationContext, request)
        mutableState.update {
            it.copy(
                message = if (existing?.downloadStatus == VideoDownloadStatus.PAUSED) {
                    "正在恢复视频下载"
                } else {
                    "已加入视频下载"
                },
            )
        }
    }

    fun resumeVideoDownload(taskId: String) {
        VideoDownloadService.resume(getApplication<Application>().applicationContext, taskId)
    }

    fun pauseVideoDownload(taskId: String) {
        VideoDownloadService.pause(getApplication<Application>().applicationContext, taskId)
    }

    fun cancelVideoDownload(taskId: String) {
        VideoDownloadService.cancel(getApplication<Application>().applicationContext, taskId)
    }

    fun openFavoriteBatchDownload(folder: BilibiliFavoriteFolder) {
        // long: 用户可能快速切换收藏夹；取消旧分页请求可避免迟到结果覆盖当前弹层的资源列表。
        favoriteBatchLoadJob?.cancel()
        mutableState.update {
            it.copy(
                favoriteBatchFolder = folder,
                favoriteBatchVideos = emptyList(),
                isFavoriteBatchLoading = true,
                isFavoriteBatchSubmitting = false,
                message = null,
            )
        }
        favoriteBatchLoadJob = viewModelScope.launch {
            try {
                val videos = repository.favoriteVideosAll(folder)
                mutableState.update { current ->
                    if (current.favoriteBatchFolder?.id != folder.id) {
                        current
                    } else {
                        current.copy(favoriteBatchVideos = videos, isFavoriteBatchLoading = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { current ->
                    if (current.favoriteBatchFolder?.id != folder.id) {
                        current
                    } else {
                        current.copy(
                            isFavoriteBatchLoading = false,
                            message = error.userMessage("批量下载列表加载失败"),
                        )
                    }
                }
            }
        }
    }

    fun dismissFavoriteBatchDownload() {
        if (state.value.isFavoriteBatchSubmitting) return
        favoriteBatchLoadJob?.cancel()
        favoriteBatchLoadJob = null
        mutableState.update {
            it.copy(
                favoriteBatchFolder = null,
                favoriteBatchVideos = emptyList(),
                isFavoriteBatchLoading = false,
            )
        }
    }

    fun startFavoriteBatchDownload(
        kind: FavoriteBatchDownloadKind,
        videos: List<BilibiliLibraryVideo>,
    ) {
        if (state.value.isFavoriteBatchSubmitting) return
        val selectedVideos = videos.distinctBy { item -> item.video.bvid }
        if (selectedVideos.isEmpty()) {
            mutableState.update { it.copy(message = "请至少选择一个收藏资源") }
            return
        }
        val qualityPreference = state.value.qualityPreference
        mutableState.update { it.copy(isFavoriteBatchSubmitting = true, message = null) }
        viewModelScope.launch {
            try {
                val pageResults = resolveFavoriteDownloadPages(selectedVideos, qualityPreference)
                val resolvedPages = pageResults.flatMap { result -> result.getOrNull().orEmpty() }
                if (resolvedPages.isEmpty()) {
                    throw pageResults.firstNotNullOfOrNull(Result<List<FavoriteDownloadPage>>::exceptionOrNull)
                        ?: IllegalStateException("所选资源没有可下载分 P")
                }
                val filesDir = getApplication<Application>().filesDir
                val enqueueResult = when (kind) {
                    FavoriteBatchDownloadKind.AUDIO -> {
                        val result = container.audioDownloadRepository.enqueueAll(
                            requests = resolvedPages.map(FavoriteDownloadPage::toAudioRequest),
                            tempFilePath = { taskId ->
                                DownloadTempFilePolicy.audio(filesDir, taskId).absolutePath
                            },
                        )
                        if (result.queuedCount > 0) {
                            AudioDownloadService.drain(getApplication<Application>().applicationContext)
                        }
                        result
                    }
                    FavoriteBatchDownloadKind.VIDEO -> {
                        val result = container.videoDownloadRepository.enqueueAll(
                            requests = resolvedPages.map(FavoriteDownloadPage::toVideoRequest),
                            tempFiles = { taskId -> DownloadTempFilePolicy.video(filesDir, taskId) },
                        )
                        if (result.queuedCount > 0) {
                            VideoDownloadService.drain(getApplication<Application>().applicationContext)
                        }
                        result
                    }
                }
                val failedResources = pageResults.count(Result<List<FavoriteDownloadPage>>::isFailure)
                mutableState.update {
                    it.copy(
                        favoriteBatchFolder = null,
                        favoriteBatchVideos = emptyList(),
                        isFavoriteBatchLoading = false,
                        isFavoriteBatchSubmitting = false,
                        message = batchDownloadMessage(kind, enqueueResult, failedResources),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        isFavoriteBatchSubmitting = false,
                        message = error.userMessage("批量下载任务创建失败"),
                    )
                }
            }
        }
    }

    fun retryFailedAudioDownloads() {
        viewModelScope.launch {
            val count = container.audioDownloadRepository.retryFailedTasks()
            if (count > 0) AudioDownloadService.drain(getApplication<Application>().applicationContext)
            mutableState.update {
                it.copy(message = if (count > 0) "已重新加入 $count 个失败音频任务" else "没有失败的音频任务")
            }
        }
    }

    fun retryFailedVideoDownloads() {
        viewModelScope.launch {
            val count = container.videoDownloadRepository.retryFailedTasks()
            if (count > 0) VideoDownloadService.drain(getApplication<Application>().applicationContext)
            mutableState.update {
                it.copy(message = if (count > 0) "已重新加入 $count 个失败视频任务" else "没有失败的视频任务")
            }
        }
    }

    fun setDownloadUnmeteredOnly(enabled: Boolean) {
        viewModelScope.launch {
            container.downloadNetworkPreferenceRepository.setUnmeteredOnly(enabled)
            val appContext = getApplication<Application>().applicationContext
            if (state.value.audioDownloads.any { task -> task.downloadStatus in AUDIO_ACTIVE_DOWNLOAD_STATUSES }) {
                AudioDownloadService.refreshNetworkConstraint(appContext)
            }
            if (state.value.videoDownloads.any { task -> task.downloadStatus in VIDEO_ACTIVE_DOWNLOAD_STATUSES }) {
                VideoDownloadService.refreshNetworkConstraint(appContext)
            }
            mutableState.update {
                it.copy(message = if (enabled) "下载已限制为 Wi-Fi 或其他非计费网络" else "下载可使用任意已验证网络")
            }
        }
    }

    fun openFavoriteFolder(folder: BilibiliFavoriteFolder) {
        favoriteFolderJob?.cancel()
        mutableState.update {
            it.copy(
                selectedFavoriteFolder = folder,
                isLibraryLoading = true,
                isFavoriteLoadingMore = false,
                favoriteNextPage = null,
                libraryVideos = emptyList(),
                message = null,
            )
        }
        loadFavoriteFolderPage(folder = folder, page = 1, reset = true)
    }

    fun loadMoreFavoriteFolder() {
        val current = state.value
        val folder = current.selectedFavoriteFolder ?: return
        val nextPage = current.favoriteNextPage ?: return
        if (current.isLibraryLoading || current.isFavoriteLoadingMore || current.isFavoriteMutating) return
        loadFavoriteFolderPage(folder = folder, page = nextPage, reset = false)
    }

    fun closeFavoriteFolder() {
        favoriteFolderJob?.cancel()
        favoriteFolderJob = null
        mutableState.update {
            it.copy(
                selectedFavoriteFolder = null,
                libraryVideos = emptyList(),
                favoriteNextPage = null,
                isLibraryLoading = false,
                isFavoriteLoadingMore = false,
            )
        }
    }

    fun createFavoriteFolder(title: String) {
        if (!beginFavoriteMutation()) return
        viewModelScope.launch {
            runCatching { repository.createFavoriteFolder(title) }
                .onSuccess { folder ->
                    mutableState.update { current ->
                        current.copy(
                            createdFavoriteFolders = listOf(folder) + current.createdFavoriteFolders
                                .filterNot { item -> item.id == folder.id },
                            isFavoriteMutating = false,
                            message = "已新建收藏夹“${folder.title}”",
                        )
                    }
                }
                .onFailure { error -> handleFavoriteMutationFailure(error, "新建收藏夹失败") }
        }
    }

    fun renameFavoriteFolder(folder: BilibiliFavoriteFolder, title: String) {
        if (!requireManagedFavoriteFolder(folder) || !beginFavoriteMutation()) return
        val normalizedTitle = title.trim()
        viewModelScope.launch {
            runCatching { repository.renameFavoriteFolder(folder.id, normalizedTitle) }
                .onSuccess {
                    mutableState.update { current ->
                        val renamed = folder.copy(title = normalizedTitle)
                        current.copy(
                            createdFavoriteFolders = current.createdFavoriteFolders.map { item ->
                                if (item.id == folder.id) item.copy(title = normalizedTitle) else item
                            },
                            selectedFavoriteFolder = current.selectedFavoriteFolder
                                ?.let { selected -> if (selected.id == folder.id) renamed else selected },
                            isFavoriteMutating = false,
                            message = "收藏夹已重命名",
                        )
                    }
                }
                .onFailure { error -> handleFavoriteMutationFailure(error, "重命名收藏夹失败") }
        }
    }

    fun deleteFavoriteFolder(folder: BilibiliFavoriteFolder) {
        if (!requireManagedFavoriteFolder(folder) || !beginFavoriteMutation()) return
        viewModelScope.launch {
            runCatching { repository.deleteFavoriteFolder(folder.id) }
                .onSuccess {
                    if (state.value.selectedFavoriteFolder?.id == folder.id) {
                        // long: 删除当前打开的收藏夹后，详情请求已经失去归属，必须同步终止并收起加载态。
                        favoriteFolderJob?.cancel()
                        favoriteFolderJob = null
                    }
                    mutableState.update { current ->
                        val deletingSelected = current.selectedFavoriteFolder?.id == folder.id
                        current.copy(
                            createdFavoriteFolders = current.createdFavoriteFolders.filterNot { item -> item.id == folder.id },
                            selectedFavoriteFolder = current.selectedFavoriteFolder.takeUnless { deletingSelected },
                            libraryVideos = if (deletingSelected) emptyList() else current.libraryVideos,
                            favoriteNextPage = if (deletingSelected) null else current.favoriteNextPage,
                            isLibraryLoading = if (deletingSelected) false else current.isLibraryLoading,
                            isFavoriteLoadingMore = if (deletingSelected) false else current.isFavoriteLoadingMore,
                            isFavoriteMutating = false,
                            message = "收藏夹已删除",
                        )
                    }
                }
                .onFailure { error -> handleFavoriteMutationFailure(error, "删除收藏夹失败") }
        }
    }

    fun addVideoToFavorite(video: BilibiliVideo, folder: BilibiliFavoriteFolder) {
        if (!requireManagedFavoriteFolder(folder) || !beginFavoriteMutation()) return
        viewModelScope.launch {
            try {
                val aid = video.aid ?: repository.videoDetail(video.bvid).aid
                    ?: throw BilibiliApiException(-400, "视频缺少 aid")
                val membership = favoriteFolderMembership(aid, folder.id)
                if (membership?.containsVideo == true) {
                    mutableState.update { current ->
                        current.copy(
                            createdFavoriteFolders = current.createdFavoriteFolders.updateFolderCount(
                                folder,
                                membership.folder.mediaCount,
                            ),
                            isFavoriteMutating = false,
                            message = "已在“${folder.title}”中",
                        )
                    }
                    return@launch
                }
                repository.addVideoToFavorite(aid = aid, folderId = folder.id)
                mutableState.update { current ->
                    val currentCount = current.createdFavoriteFolders
                        .firstOrNull { existing -> existing.id == folder.id }
                        ?.mediaCount ?: folder.mediaCount
                    // long: list-all 在提交前给出该视频是否已存在；只有明确新增成功时才递增，规避重复收藏导致的虚高计数。
                    val refreshedCount = (membership?.folder?.mediaCount ?: currentCount) + 1
                    current.copy(
                        createdFavoriteFolders = current.createdFavoriteFolders.map { existing ->
                            if (existing.id == folder.id) existing.copy(mediaCount = refreshedCount) else existing
                        },
                        isFavoriteMutating = false,
                        message = "已加入“${folder.title}”",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                handleFavoriteMutationFailure(error, "加入收藏夹失败")
            }
        }
    }

    fun removeVideoFromFavorite(item: BilibiliLibraryVideo) {
        val folder = state.value.selectedFavoriteFolder ?: return
        if (!requireManagedFavoriteFolder(folder) || !beginFavoriteMutation()) return
        viewModelScope.launch {
            try {
                val aid = item.video.aid ?: repository.videoDetail(item.video.bvid).aid
                    ?: throw BilibiliApiException(-400, "视频缺少 aid")
                val membership = favoriteFolderMembership(aid, folder.id)
                repository.removeVideoFromFavorite(aid = aid, folderId = folder.id)
                mutableState.update { current ->
                    val currentCount = current.selectedFavoriteFolder?.mediaCount ?: folder.mediaCount
                    // long: 移出接口后的元数据存在短暂缓存，使用操作前的成员关系扣减才能让空夹标题立即归零。
                    val refreshedCount = if (membership?.containsVideo == true) {
                        (membership.folder.mediaCount - 1).coerceAtLeast(0)
                    } else {
                        (currentCount - 1).coerceAtLeast(0)
                    }
                    if (current.selectedFavoriteFolder?.id != folder.id) {
                        current.copy(
                            createdFavoriteFolders = current.createdFavoriteFolders.map { existing ->
                                if (existing.id == folder.id) existing.copy(mediaCount = refreshedCount) else existing
                            },
                            isFavoriteMutating = false,
                            message = "已从“${folder.title}”移出",
                        )
                    } else {
                        current.copy(
                            createdFavoriteFolders = current.createdFavoriteFolders.map { existing ->
                                if (existing.id == folder.id) existing.copy(mediaCount = refreshedCount) else existing
                            },
                            selectedFavoriteFolder = current.selectedFavoriteFolder.copy(mediaCount = refreshedCount),
                            libraryVideos = current.libraryVideos.filterNot { video -> video.video.bvid == item.video.bvid },
                            isFavoriteMutating = false,
                            message = "已从“${folder.title}”移出",
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                handleFavoriteMutationFailure(error, "移出收藏夹失败")
            }
        }
    }

    fun clearLocalHistory() {
        viewModelScope.launch {
            container.playbackHistoryRepository.clear()
        }
    }

    fun searchOnlineHistory(query: String) {
        val normalized = query.trim()
        mutableState.update {
            it.copy(
                onlineHistoryQuery = normalized,
                libraryVideos = emptyList(),
                onlineHistoryNextCursor = null,
                onlineHistoryNextSearchPage = null,
                onlineHistoryHasMore = false,
                message = null,
            )
        }
        loadOnlineHistory(reset = true)
    }

    fun loadMoreOnlineHistory() {
        val current = state.value
        if (current.librarySection != AccountLibrarySection.ONLINE_HISTORY ||
            !current.onlineHistoryHasMore ||
            current.isLibraryLoading ||
            current.isOnlineHistoryLoadingMore ||
            current.isOnlineHistoryMutating
        ) {
            return
        }
        loadOnlineHistory(reset = false)
    }

    fun deleteOnlineHistory(item: BilibiliLibraryVideo) {
        val historyKey = item.historyKey ?: return
        if (state.value.isOnlineHistoryMutating) return
        mutableState.update { it.copy(isOnlineHistoryMutating = true, message = null) }
        viewModelScope.launch {
            runCatching { repository.deleteOnlineHistory(historyKey) }
                .onSuccess {
                    mutableState.update { current ->
                        current.copy(
                            libraryVideos = current.libraryVideos.filterNot { video -> video.historyKey == historyKey },
                            isOnlineHistoryMutating = false,
                        )
                    }
                }
                .onFailure(::handleOnlineHistoryMutationFailure)
        }
    }

    fun clearOnlineHistory() {
        if (state.value.isOnlineHistoryMutating) return
        mutableState.update { it.copy(isOnlineHistoryMutating = true, message = null) }
        viewModelScope.launch {
            runCatching { repository.clearOnlineHistory() }
                .onSuccess {
                    mutableState.update { current ->
                        current.copy(
                            libraryVideos = emptyList(),
                            onlineHistoryNextCursor = null,
                            onlineHistoryNextSearchPage = null,
                            onlineHistoryHasMore = false,
                            isOnlineHistoryMutating = false,
                        )
                    }
                }
                .onFailure(::handleOnlineHistoryMutationFailure)
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

    private suspend fun resolveFavoriteDownloadPages(
        videos: List<BilibiliLibraryVideo>,
        qualityPreference: AudioQualityPreference,
    ): List<Result<List<FavoriteDownloadPage>>> = coroutineScope {
        val concurrency = Semaphore(FavoriteBatchDownloadPolicy.DETAIL_RESOLUTION_CONCURRENCY)
        // long: 批量创建只并发解析少量视频详情，既缩短等待时间，也避免一次选择大量收藏后同时轰击 Bilibili 接口。
        videos.map { item ->
            async {
                concurrency.withPermit {
                    try {
                        val detail = repository.videoDetail(item.video.bvid)
                        Result.success(FavoriteBatchDownloadPolicy.expand(item.video, detail, qualityPreference))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Result.failure(error)
                    }
                }
            }
        }.awaitAll()
    }

    private fun batchDownloadMessage(
        kind: FavoriteBatchDownloadKind,
        result: DownloadBatchEnqueueResult,
        failedResources: Int,
    ): String {
        val parts = buildList {
            if (result.queuedCount > 0) add("已加入 ${result.queuedCount} 个${kind.label}任务")
            if (result.skippedCompletedCount > 0) add("${result.skippedCompletedCount} 个已完成")
            if (result.skippedExistingCount > 0) add("${result.skippedExistingCount} 个已在队列")
            if (failedResources > 0) add("$failedResources 个资源解析失败")
        }
        return parts.joinToString("，").ifBlank { "没有新增${kind.label}任务" }
    }

    internal fun currentPlaybackQueue(queueId: Long? = null): PlaybackQueueSnapshot<Track>? {
        return playbackQueueSnapshots.current(queueId)
    }

    internal fun updatePlaybackQueuePosition(mediaId: String, positionMs: Long) {
        playbackQueueSnapshots.updateResumePosition(mediaId, positionMs)
    }

    internal fun removePlaybackQueueItem(mediaId: String) {
        stopProgressiveQueueForUserEdit()
        playbackQueueSnapshots.remove(mediaId)
    }

    internal fun movePlaybackQueueItemNext(mediaId: String, currentMediaId: String) {
        stopProgressiveQueueForUserEdit()
        playbackQueueSnapshots.moveNext(mediaId, currentMediaId)
    }

    internal fun clearPlaybackQueue() {
        stopProgressiveQueueForUserEdit()
        playbackQueueSnapshots.clear()
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

    private fun stopProgressiveQueueForUserEdit() {
        // long: 用户开始编辑队列后立即失效当前渐进补齐代数，避免后台解析完成的旧分 P 把刚删除或移动的条目重新插回。
        cancelPageQueueExpansion()
        playbackEventIds.incrementAndGet()
    }

    private fun publishLibraryVideos(videos: List<BilibiliLibraryVideo>) {
        mutableState.update { it.copy(libraryVideos = videos, isLibraryLoading = false) }
    }

    private suspend fun favoriteFolders(mid: Long): Pair<List<BilibiliFavoriteFolder>, List<BilibiliFavoriteFolder>> =
        coroutineScope {
            val created = async { repository.createdFavoriteFolders(mid) }
            val collected = async { repository.collectedFavoriteFolders(mid) }
            created.await() to collected.await()
        }

    private fun loadFavoriteFolderPage(folder: BilibiliFavoriteFolder, page: Int, reset: Boolean) {
        if (reset) favoriteFolderJob?.cancel()
        mutableState.update {
            it.copy(
                isLibraryLoading = reset,
                isFavoriteLoadingMore = !reset,
                message = null,
            )
        }
        favoriteFolderJob = viewModelScope.launch {
            try {
                val loaded = loadPlayableFavoritePage(folder, page)
                mutableState.update { current ->
                    val selected = current.selectedFavoriteFolder
                    if (selected?.id != folder.id || selected.group != folder.group || selected.type != folder.type) {
                        current
                    } else {
                        val videos = if (reset) {
                            loaded.videos
                        } else {
                            (current.libraryVideos + loaded.videos).distinctBy { item -> item.video.bvid }
                        }
                        current.copy(
                            libraryVideos = videos,
                            favoriteNextPage = loaded.nextPage,
                            createdFavoriteFolders = current.createdFavoriteFolders.updateFolderCount(folder, loaded.mediaCount),
                            collectedFavoriteFolders = current.collectedFavoriteFolders.updateFolderCount(folder, loaded.mediaCount),
                            selectedFavoriteFolder = selected.copy(
                                mediaCount = loaded.mediaCount ?: selected.mediaCount,
                            ),
                            isLibraryLoading = false,
                            isFavoriteLoadingMore = false,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update { current ->
                    if (current.selectedFavoriteFolder?.id != folder.id) {
                        current
                    } else {
                        current.copy(
                            isLibraryLoading = false,
                            isFavoriteLoadingMore = false,
                            message = error.userMessage("收藏夹内容加载失败"),
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadPlayableFavoritePage(
        folder: BilibiliFavoriteFolder,
        startPage: Int,
    ): FavoriteFolderLoadResult {
        var page = startPage.coerceAtLeast(1)
        repeat(MAX_EMPTY_FAVORITE_PAGES) {
            val result = repository.favoriteVideoPage(folder, page)
            if (result.videos.isNotEmpty() || !result.hasMore) {
                return FavoriteFolderLoadResult(
                    videos = result.videos,
                    nextPage = (page + 1).takeIf { result.hasMore },
                    mediaCount = result.mediaCount,
                )
            }
            // long: 某一页可能全部是失效资源；继续沿服务端分页读取，避免页面错误显示“暂无内容”。
            page += 1
        }
        throw BilibiliApiException(-429, "连续失效收藏内容过多，请稍后重试")
    }

    private fun beginFavoriteMutation(): Boolean {
        if (state.value.isFavoriteMutating) return false
        mutableState.update { it.copy(isFavoriteMutating = true, message = null) }
        return true
    }

    private suspend fun favoriteFolderMembership(
        aid: Long,
        folderId: Long,
    ) = state.value.account.mid
        .takeIf { mid -> mid > 0L }
        ?.let { mid -> repository.createdFavoriteFolderMemberships(mid, aid) }
        ?.firstOrNull { membership -> membership.folder.id == folderId }

    private fun requireManagedFavoriteFolder(folder: BilibiliFavoriteFolder): Boolean {
        if (folder.isUserManaged) return true
        mutableState.update { it.copy(message = "“我收藏的”和视频合集为只读内容") }
        return false
    }

    private fun handleFavoriteMutationFailure(error: Throwable, fallback: String) {
        if (error is BilibiliApiException && error.code == -101) {
            clearOnlineLibrary()
            mutableState.update {
                it.copy(
                    account = BilibiliAccount(false, "", ""),
                    isFavoriteMutating = false,
                    message = "登录已失效，请重新登录",
                )
            }
            refreshAccount()
            return
        }
        mutableState.update {
            it.copy(isFavoriteMutating = false, message = error.userMessage(fallback))
        }
    }

    private fun loadOnlineHistory(reset: Boolean) {
        val account = state.value.account
        if (!account.isLoggedIn) {
            mutableState.update {
                it.copy(isLibraryLoading = false, isOnlineHistoryLoadingMore = false)
            }
            return
        }
        if (reset) onlineHistoryJob?.cancel()
        val snapshot = state.value
        val query = snapshot.onlineHistoryQuery
        val cursor = if (reset) null else snapshot.onlineHistoryNextCursor
        val searchPage = if (query.isBlank() || reset) {
            1
        } else {
            snapshot.onlineHistoryNextSearchPage ?: return
        }
        mutableState.update {
            it.copy(
                isLibraryLoading = reset,
                isOnlineHistoryLoadingMore = !reset,
                message = null,
            )
        }
        onlineHistoryJob = viewModelScope.launch {
            try {
                val page = if (query.isBlank()) {
                    val result = repository.onlineHistory(cursor)
                    OnlineHistoryLoadResult(
                        videos = result.videos,
                        nextCursor = result.nextCursor,
                        nextSearchPage = null,
                        hasMore = result.hasMore,
                    )
                } else {
                    val result = repository.searchOnlineHistory(query, searchPage)
                    OnlineHistoryLoadResult(
                        videos = result.videos,
                        nextCursor = null,
                        nextSearchPage = result.nextSearchPage,
                        hasMore = result.hasMore,
                    )
                }
                mutableState.update { current ->
                    if (current.librarySection != AccountLibrarySection.ONLINE_HISTORY ||
                        current.onlineHistoryQuery != query
                    ) {
                        current
                    } else {
                        val videos = if (reset) page.videos else {
                            (current.libraryVideos + page.videos).distinctBy { item ->
                                item.historyKey ?: item.video.bvid
                            }
                        }
                        current.copy(
                            libraryVideos = videos,
                            onlineHistoryNextCursor = page.nextCursor,
                            onlineHistoryNextSearchPage = page.nextSearchPage,
                            onlineHistoryHasMore = page.hasMore,
                            isLibraryLoading = false,
                            isOnlineHistoryLoadingMore = false,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                handleOnlineHistoryLoadFailure(error)
            }
        }
    }

    private fun handleOnlineHistoryLoadFailure(error: Throwable) {
        if (error is BilibiliApiException && error.code == -101) {
            // long: 登录失效只清理账号在线区；Room 本地历史由独立 Flow 维护，必须继续可用。
            clearOnlineLibrary()
            mutableState.update {
                it.copy(
                    account = BilibiliAccount(false, "", ""),
                    message = "登录已失效，请重新登录",
                )
            }
            refreshAccount()
            return
        }
        mutableState.update {
            it.copy(
                isLibraryLoading = false,
                isOnlineHistoryLoadingMore = false,
                message = error.userMessage("在线历史加载失败"),
            )
        }
    }

    private fun handleOnlineHistoryMutationFailure(error: Throwable) {
        if (error is BilibiliApiException && error.code == -101) {
            handleOnlineHistoryLoadFailure(error)
            return
        }
        mutableState.update {
            it.copy(
                isOnlineHistoryMutating = false,
                message = error.userMessage("在线历史操作失败"),
            )
        }
    }

    private fun loadHomeFeed(
        selectedCreators: List<BilibiliCreator>,
        feed: RecommendFeed,
        preserveCurrentVideos: Boolean = false,
    ) {
        // long: 切换来源或手动刷新时取消旧请求，防止较慢的旧响应覆盖用户刚保存的新范围。
        recommendationsJob?.cancel()
        recommendationGeneration += 1L
        val generation = recommendationGeneration
        if (!preserveCurrentVideos) recommendationNextPage = null
        if (selectedCreators.isNotEmpty()) {
            val tabs = CreatorFeedPolicy.reconcileTabs(state.value.creatorFeedTabs, selectedCreators)
            val selectedMid = state.value.selectedCreatorFeedMid
                ?.takeIf { mid -> tabs.any { tab -> tab.creator.mid == mid } }
                ?: tabs.firstOrNull()?.creator?.mid
            mutableState.update {
                it.copy(
                    creatorFeedTabs = tabs,
                    selectedCreatorFeedMid = selectedMid,
                    isFeedLoading = false,
                    isFeedLoadingMore = false,
                    message = null,
                )
            }
            val selectedTab = tabs.firstOrNull { tab -> tab.creator.mid == selectedMid }
            if (selectedTab != null && !selectedTab.hasLoaded) {
                loadCreatorFeedTab(selectedTab.creator.mid, append = false)
            }
            return
        }
        mutableState.update {
            it.copy(
                feed = feed,
                recommendations = if (preserveCurrentVideos) it.recommendations else emptyList(),
                recommendationHasMore = if (preserveCurrentVideos) it.recommendationHasMore else false,
                creatorFeedTabs = emptyList(),
                selectedCreatorFeedMid = null,
                isFeedLoading = true,
                isFeedLoadingMore = false,
                message = null,
            )
        }
        recommendationsJob = viewModelScope.launch {
            try {
                val result = repository.recommendations(feed)
                if (generation != recommendationGeneration) return@launch
                recommendationNextPage = if (result.hasMore) result.page + 1 else null
                mutableState.update {
                    it.copy(
                        recommendations = result.videos,
                        recommendationHasMore = result.hasMore,
                        isFeedLoading = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (generation == recommendationGeneration) {
                    mutableState.update {
                        it.copy(isFeedLoading = false, message = error.userMessage("推荐加载失败"))
                    }
                }
            }
        }
    }

    private fun loadCreatorFeedTab(mid: Long, append: Boolean) {
        val tab = state.value.creatorFeedTabs.firstOrNull { item -> item.creator.mid == mid } ?: return
        val page = if (append) tab.nextPage ?: return else 1
        recommendationsJob?.cancel()
        recommendationGeneration += 1L
        val generation = recommendationGeneration
        mutableState.update { current ->
            current.copy(
                creatorFeedTabs = current.creatorFeedTabs.map { item ->
                    when (item.creator.mid) {
                        mid -> item.copy(
                            videos = item.videos,
                            nextPage = item.nextPage,
                            isLoading = !append,
                            isLoadingMore = append,
                        )
                        else -> item.copy(isLoading = false, isLoadingMore = false)
                    }
                },
                message = null,
            )
        }
        recommendationsJob = viewModelScope.launch {
            try {
                val result = repository.creatorVideoPage(tab.creator, page)
                if (generation != recommendationGeneration) return@launch
                mutableState.update { current ->
                    current.copy(
                        creatorFeedTabs = current.creatorFeedTabs.map { item ->
                            if (item.creator.mid == mid) {
                                CreatorFeedPolicy.applyPage(item, result, append)
                            } else {
                                item
                            }
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (generation == recommendationGeneration) {
                    mutableState.update { current ->
                        current.copy(
                            creatorFeedTabs = current.creatorFeedTabs.map { item ->
                                if (item.creator.mid == mid) {
                                    item.copy(isLoading = false, isLoadingMore = false)
                                } else {
                                    item
                                }
                            },
                            message = error.userMessage(if (append) "更多投稿加载失败" else "UP 主投稿加载失败"),
                        )
                    }
                }
            }
        }
    }

    private fun finishRecommendationPagination(generation: Long) {
        if (generation != recommendationGeneration) return
        mutableState.update {
            it.copy(recommendationHasMore = false, isFeedLoadingMore = false)
        }
    }

    private fun clearOnlineLibrary() {
        onlineHistoryJob?.cancel()
        onlineHistoryJob = null
        favoriteBatchLoadJob?.cancel()
        favoriteBatchLoadJob = null
        favoriteFolderJob?.cancel()
        favoriteFolderJob = null
        mutableState.update {
            it.copy(
                createdFavoriteFolders = emptyList(),
                collectedFavoriteFolders = emptyList(),
                selectedFavoriteFolder = null,
                libraryVideos = emptyList(),
                favoriteNextPage = null,
                onlineHistoryQuery = "",
                onlineHistoryNextCursor = null,
                onlineHistoryNextSearchPage = null,
                onlineHistoryHasMore = false,
                favoriteBatchFolder = null,
                favoriteBatchVideos = emptyList(),
                isFavoriteBatchLoading = false,
                isFavoriteBatchSubmitting = false,
                isFavoriteLoadingMore = false,
                isFavoriteMutating = false,
                isLibraryLoading = false,
                isOnlineHistoryLoadingMore = false,
                isOnlineHistoryMutating = false,
            )
        }
    }
}

private data class FavoriteFolderLoadResult(
    val videos: List<BilibiliLibraryVideo>,
    val nextPage: Int?,
    val mediaCount: Int?,
)

private fun List<BilibiliFavoriteFolder>.updateFolderCount(
    folder: BilibiliFavoriteFolder,
    mediaCount: Int?,
): List<BilibiliFavoriteFolder> {
    if (mediaCount == null) return this
    return map { existing ->
        if (existing.id == folder.id && existing.group == folder.group && existing.type == folder.type) {
            existing.copy(mediaCount = mediaCount)
        } else {
            existing
        }
    }
}

private fun List<BilibiliDynamicItem>.replaceDynamic(item: BilibiliDynamicItem): List<BilibiliDynamicItem> {
    return map { current -> if (current.id == item.id) item else current }
}

private data class OnlineHistoryLoadResult(
    val videos: List<BilibiliLibraryVideo>,
    val nextCursor: BilibiliOnlineHistoryCursor?,
    val nextSearchPage: Int?,
    val hasMore: Boolean,
)

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

private val AUDIO_ACTIVE_DOWNLOAD_STATUSES = setOf(
    AudioDownloadStatus.QUEUED,
    AudioDownloadStatus.RESOLVING,
    AudioDownloadStatus.DOWNLOADING,
    AudioDownloadStatus.PUBLISHING,
)

private val VIDEO_ACTIVE_DOWNLOAD_STATUSES = setOf(
    VideoDownloadStatus.QUEUED,
    VideoDownloadStatus.RESOLVING,
    VideoDownloadStatus.DOWNLOADING_VIDEO,
    VideoDownloadStatus.DOWNLOADING_AUDIO,
    VideoDownloadStatus.MUXING,
    VideoDownloadStatus.PUBLISHING,
)

private const val MAX_EMPTY_FAVORITE_PAGES = 100
