package com.lonnnnnng.biu.download

import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideoDetail

enum class FavoriteBatchDownloadKind(val label: String) {
    AUDIO("音频"),
    VIDEO("视频"),
}

data class FavoriteDownloadPage(
    val source: BilibiliTrackSource,
    val currentTitle: String,
    val resourceTitle: String,
    val artist: String,
    val artworkUrl: String?,
) {
    fun toAudioRequest(): AudioDownloadRequest {
        return AudioDownloadRequest.create(source, currentTitle, resourceTitle, artist, artworkUrl)
    }

    fun toVideoRequest(): VideoDownloadRequest {
        return VideoDownloadRequest.create(source, currentTitle, resourceTitle, artist, artworkUrl)
    }
}

object FavoriteBatchDownloadPolicy {
    const val DETAIL_RESOLUTION_CONCURRENCY = 3

    fun expand(
        video: BilibiliVideo,
        detail: BilibiliVideoDetail,
        qualityPreference: AudioQualityPreference,
    ): List<FavoriteDownloadPage> {
        require(detail.pages.isNotEmpty()) { "视频没有可下载分 P" }
        val resourceTitle = detail.title.trim().ifBlank { video.title.trim() }.ifBlank { detail.bvid }
        val artist = detail.author.trim().ifBlank { video.author.trim() }
        val resourceArtwork = detail.coverUrl.takeIf(String::isNotBlank)
            ?: video.coverUrl.takeIf(String::isNotBlank)
        val isMultiPage = detail.pages.size > 1

        // long: 收藏夹中的一个资源可能对应数百个分 P；每个 cid 必须成为独立任务，才能单独断点续传、失败重试和生成正确文件名。
        return detail.pages.map { page ->
            FavoriteDownloadPage(
                source = BilibiliTrackSource(detail.bvid, page.cid, qualityPreference),
                currentTitle = if (isMultiPage) {
                    page.title.trim().ifBlank { "P${page.page}" }
                } else {
                    resourceTitle
                },
                resourceTitle = resourceTitle,
                artist = artist,
                artworkUrl = page.coverUrl?.takeIf(String::isNotBlank) ?: resourceArtwork,
            )
        }
    }
}
