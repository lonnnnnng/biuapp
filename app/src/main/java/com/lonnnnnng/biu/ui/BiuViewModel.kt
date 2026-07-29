package com.lonnnnnng.biu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lonnnnnng.biu.appContainer
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.data.bilibili.AccountLibrarySection
import com.lonnnnnng.biu.data.bilibili.BilibiliAccount
import com.lonnnnnng.biu.data.bilibili.BilibiliFavoriteFolder
import com.lonnnnnng.biu.data.bilibili.BilibiliLibraryVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.RecommendFeed
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainSection(val label: String) {
    RECOMMEND("推荐"),
    SEARCH("搜索"),
    ACCOUNT("账号"),
}

data class PlaybackRequest(
    val eventId: Long,
    val track: Track,
    val startPositionMs: Long = 0L,
)

data class BiuUiState(
    val section: MainSection = MainSection.RECOMMEND,
    val feed: RecommendFeed = RecommendFeed.MUSIC,
    val recommendations: List<BilibiliVideo> = emptyList(),
    val searchResults: List<BilibiliVideo> = emptyList(),
    val submittedKeyword: String = "",
    val account: BilibiliAccount = BilibiliAccount(false, "", ""),
    val librarySection: AccountLibrarySection = AccountLibrarySection.FAVORITES,
    val favoriteFolders: List<BilibiliFavoriteFolder> = emptyList(),
    val selectedFavoriteFolder: BilibiliFavoriteFolder? = null,
    val libraryVideos: List<BilibiliLibraryVideo> = emptyList(),
    val localHistory: List<PlaybackHistoryEntity> = emptyList(),
    val qualityPreference: AudioQualityPreference = AudioQualityPreference.HIGHEST,
    val isFeedLoading: Boolean = true,
    val isSearchLoading: Boolean = false,
    val isAccountLoading: Boolean = true,
    val isLibraryLoading: Boolean = false,
    val resolvingBvid: String? = null,
    val message: String? = null,
    val playbackRequest: PlaybackRequest? = null,
)

class BiuViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.appContainer
    private val repository = container.bilibiliRepository
    private val playbackEventIds = AtomicLong(0L)
    private val mutableState = MutableStateFlow(BiuUiState())

    val state: StateFlow<BiuUiState> = mutableState.asStateFlow()

    init {
        loadRecommendations(RecommendFeed.MUSIC)
        refreshAccount()
        viewModelScope.launch {
            container.playbackHistoryRepository.recent.collect { history ->
                mutableState.update { it.copy(localHistory = history) }
            }
        }
    }

    fun selectSection(section: MainSection) {
        mutableState.update { it.copy(section = section) }
    }

    fun loadRecommendations(feed: RecommendFeed = state.value.feed) {
        mutableState.update { it.copy(feed = feed, isFeedLoading = true, message = null) }
        viewModelScope.launch {
            runCatching { repository.recommendations(feed) }
                .onSuccess { videos ->
                    mutableState.update { it.copy(recommendations = videos, isFeedLoading = false) }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isFeedLoading = false, message = error.userMessage("推荐加载失败"))
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
                section = MainSection.SEARCH,
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

    fun play(video: BilibiliVideo) {
        if (state.value.resolvingBvid != null) return
        val qualityPreference = state.value.qualityPreference
        mutableState.update { it.copy(resolvingBvid = video.bvid, message = null) }
        viewModelScope.launch {
            runCatching { repository.resolveTrack(video, qualityPreference = qualityPreference) }
                .onSuccess(::publishPlaybackRequest)
                .onFailure { error ->
                    mutableState.update {
                        it.copy(resolvingBvid = null, message = error.userMessage("播放地址解析失败"))
                    }
                }
        }
    }

    fun play(history: PlaybackHistoryEntity) {
        if (state.value.resolvingBvid != null) return
        mutableState.update { it.copy(resolvingBvid = history.bvid, message = null) }
        viewModelScope.launch {
            runCatching {
                repository.resolveTrack(history.source, history.title, history.artist, history.artworkUrl)
            }.onSuccess { track ->
                publishPlaybackRequest(track, PlaybackResumePolicy.startPositionMs(history.lastPositionMs, history.durationMs))
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

    fun consumePlaybackRequest(eventId: Long) {
        mutableState.update { current ->
            if (current.playbackRequest?.eventId == eventId) current.copy(playbackRequest = null) else current
        }
    }

    fun refreshAccount() {
        mutableState.update { it.copy(isAccountLoading = true) }
        viewModelScope.launch {
            runCatching { repository.account() }
                .onSuccess { account ->
                    mutableState.update { it.copy(account = account, isAccountLoading = false) }
                    if (account.isLoggedIn) loadLibrary(AccountLibrarySection.FAVORITES) else clearOnlineLibrary()
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isAccountLoading = false, message = error.userMessage("账号状态获取失败"))
                    }
                }
        }
    }

    fun loadLibrary(section: AccountLibrarySection) {
        mutableState.update {
            it.copy(
                librarySection = section,
                isLibraryLoading = section != AccountLibrarySection.LOCAL_HISTORY,
                selectedFavoriteFolder = if (section == AccountLibrarySection.FAVORITES) it.selectedFavoriteFolder else null,
                libraryVideos = if (section == AccountLibrarySection.LOCAL_HISTORY) it.libraryVideos else emptyList(),
                message = null,
            )
        }
        if (section == AccountLibrarySection.LOCAL_HISTORY) return
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
                    AccountLibrarySection.WATCH_LATER -> publishLibraryVideos(repository.watchLater())
                    AccountLibrarySection.ONLINE_HISTORY -> publishLibraryVideos(repository.onlineHistory())
                    AccountLibrarySection.LOCAL_HISTORY -> Unit
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(isLibraryLoading = false, message = error.userMessage("账号音乐库加载失败"))
                }
            }
        }
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

    private fun publishPlaybackRequest(track: Track, startPositionMs: Long = 0L) {
        mutableState.update {
            it.copy(
                resolvingBvid = null,
                playbackRequest = PlaybackRequest(playbackEventIds.incrementAndGet(), track, startPositionMs),
                message = track.qualityLabel?.let { quality -> "正在播放 · $quality" },
            )
        }
    }

    private fun publishLibraryVideos(videos: List<BilibiliLibraryVideo>) {
        mutableState.update { it.copy(libraryVideos = videos, isLibraryLoading = false) }
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
