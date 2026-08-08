package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.data.bilibili.BilibiliApiException
import com.lonnnnnng.biu.data.bilibili.BilibiliVideoDetail

enum class LocalPlaylistItemAvailability(val label: String) {
    CHECKING("正在检查"),
    AVAILABLE("可播放"),
    VIDEO_UNAVAILABLE("视频已失效"),
    PAGE_UNAVAILABLE("分 P 已失效"),
    LOCAL_FILE_UNAVAILABLE("本地文件不可用"),
    ERROR("状态检查失败，可尝试播放"),
    ;

    val preventsPlayback: Boolean
        get() = this == VIDEO_UNAVAILABLE || this == PAGE_UNAVAILABLE || this == LOCAL_FILE_UNAVAILABLE
}

internal data class LocalPlaylistPlaybackPlan(
    val tracks: List<Track>,
    val startIndex: Int,
    val skippedCount: Int,
)

internal object LocalPlaylistPlaybackPolicy {
    fun plan(resolvedTracks: List<Track?>, requestedIndex: Int): LocalPlaylistPlaybackPlan? {
        if (requestedIndex !in resolvedTracks.indices) return null
        val indexedTracks = resolvedTracks.mapIndexedNotNull { index, track -> track?.let { index to it } }
        if (indexedTracks.isEmpty()) return null

        // long: 用户请求位置失效时优先衔接后续歌曲；只有后方全失效才回到前面的首个可用项，避免整张歌单无法播放。
        val requestedTrack = indexedTracks.firstOrNull { (index, _) -> index >= requestedIndex }
            ?: indexedTracks.first()
        val tracks = indexedTracks.map { (_, track) -> track }
        val startIndex = indexedTracks.indexOfFirst { (index, _) -> index == requestedTrack.first }
        return LocalPlaylistPlaybackPlan(
            tracks = tracks,
            startIndex = startIndex,
            skippedCount = resolvedTracks.size - tracks.size,
        )
    }
}

internal object LocalPlaylistAvailabilityPolicy {
    fun fromBilibiliDetail(
        detailResult: Result<BilibiliVideoDetail>,
        cid: Long,
    ): LocalPlaylistItemAvailability {
        val error = detailResult.exceptionOrNull()
        if (error != null) {
            return if (error is BilibiliApiException && error.code in PERMANENT_VIDEO_UNAVAILABLE_CODES) {
                LocalPlaylistItemAvailability.VIDEO_UNAVAILABLE
            } else {
                LocalPlaylistItemAvailability.ERROR
            }
        }
        val detail = detailResult.getOrNull() ?: return LocalPlaylistItemAvailability.ERROR
        return if (detail.pages.any { page -> page.cid == cid }) {
            LocalPlaylistItemAvailability.AVAILABLE
        } else {
            LocalPlaylistItemAvailability.PAGE_UNAVAILABLE
        }
    }

    fun fromLocalUri(readable: Boolean): LocalPlaylistItemAvailability {
        return if (readable) {
            LocalPlaylistItemAvailability.AVAILABLE
        } else {
            LocalPlaylistItemAvailability.LOCAL_FILE_UNAVAILABLE
        }
    }

    // long: 风控、未登录和限流都可能以 API 错误返回，只有明确代表稿件永久不可用的错误码才阻止播放。
    private val PERMANENT_VIDEO_UNAVAILABLE_CODES = setOf(-404, 62002, 62004, 62012)
}
