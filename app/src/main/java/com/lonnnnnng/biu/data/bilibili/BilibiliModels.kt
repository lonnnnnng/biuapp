package com.lonnnnnng.biu.data.bilibili

import com.lonnnnnng.biu.core.model.AudioQualityPreference
import java.net.URI

enum class RecommendFeed(val label: String) {
    MUSIC("音乐区"),
    POPULAR("音乐榜"),
}

data class BilibiliVideo(
    val bvid: String,
    val aid: Long?,
    val title: String,
    val author: String,
    val coverUrl: String,
    val durationSeconds: Int?,
    val playCount: Long?,
    val publishedAtEpochSeconds: Long? = null,
)

data class BilibiliCreator(
    val mid: Long,
    val name: String,
    val faceUrl: String,
)

data class BilibiliVideoPage(
    val cid: Long,
    val page: Int,
    val title: String,
    val durationSeconds: Int,
    val coverUrl: String?,
)

data class BilibiliVideoDetail(
    val bvid: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val pages: List<BilibiliVideoPage>,
    val aid: Long? = null,
)

data class BilibiliAccount(
    val isLoggedIn: Boolean,
    val name: String,
    val faceUrl: String,
    val mid: Long = 0L,
)

enum class AccountLibrarySection(val label: String) {
    FAVORITES("收藏夹"),
    ONLINE_HISTORY("在线历史"),
    LOCAL_HISTORY("本地历史"),
    LOCAL_MUSIC("本地音乐"),
}

/** long: 收藏入口来源直接对应账号页中的两个在线分组，避免 UI 再根据列表位置猜测数据归属。 */
enum class BilibiliFavoriteFolderGroup(val label: String) {
    CREATED("我创建的"),
    COLLECTED("我收藏的"),
}

/** long: B 站收藏资源类型决定详情接口；11 走收藏夹资源接口，21 走视频合集接口。 */
enum class BilibiliFavoriteFolderType(val apiValue: Int, val label: String) {
    VIDEO_FOLDER(11, "收藏夹"),
    VIDEO_COLLECTION(21, "视频合集"),
    UNKNOWN(-1, "未知内容"),
    ;

    companion object {
        fun fromApiValue(value: Int): BilibiliFavoriteFolderType {
            return entries.firstOrNull { type -> type.apiValue == value } ?: UNKNOWN
        }
    }
}

data class BilibiliFavoriteFolder(
    val id: Long,
    val title: String,
    val coverUrl: String,
    val mediaCount: Int,
    val type: BilibiliFavoriteFolderType = BilibiliFavoriteFolderType.VIDEO_FOLDER,
    val group: BilibiliFavoriteFolderGroup = BilibiliFavoriteFolderGroup.CREATED,
    val ownerMid: Long = 0L,
    val ownerName: String = "",
) {
    /** long: 只有账号本人创建的普通收藏夹支持写操作；收藏的视频合集和他人收藏夹始终只读。 */
    val isUserManaged: Boolean
        get() = group == BilibiliFavoriteFolderGroup.CREATED && type == BilibiliFavoriteFolderType.VIDEO_FOLDER
}

data class BilibiliFavoriteFolderMembership(
    val folder: BilibiliFavoriteFolder,
    val containsVideo: Boolean,
)

data class BilibiliLibraryVideo(
    val video: BilibiliVideo,
    val progressSeconds: Int? = null,
    val savedAtEpochSeconds: Long? = null,
    val historyKey: String? = null,
)

data class BilibiliOnlineHistoryCursor(
    val max: Long,
    val viewAtEpochSeconds: Long,
    val business: String,
)

data class BilibiliOnlineHistoryPage(
    val videos: List<BilibiliLibraryVideo>,
    val nextCursor: BilibiliOnlineHistoryCursor?,
    val hasMore: Boolean,
)

data class BilibiliOnlineHistorySearchPage(
    val videos: List<BilibiliLibraryVideo>,
    val nextSearchPage: Int?,
    val hasMore: Boolean,
)

data class BilibiliFavoriteVideoPage(
    val videos: List<BilibiliLibraryVideo>,
    val hasMore: Boolean,
    val mediaCount: Int? = null,
)

data class WbiKeys(
    val imgKey: String,
    val subKey: String,
)

data class DashAudioStream(
    val url: String,
    val bandwidth: Long,
    val codecs: String,
    val qualityLabel: String,
    val expiresAtEpochSeconds: Long?,
    val backupUrls: List<String> = emptyList(),
) {
    fun replacementUrl(failedUrl: String): String {
        return (listOf(url) + backupUrls).firstOrNull { candidate -> candidate != failedUrl } ?: url
    }
}

data class DashVideoStream(
    val url: String,
    val qualityId: Int,
    val bandwidth: Long,
    val codecs: String,
    val width: Int,
    val height: Int,
    val frameRate: Double,
    val qualityLabel: String,
    val expiresAtEpochSeconds: Long?,
    val backupUrls: List<String> = emptyList(),
)

data class DashDownloadStreams(
    val video: DashVideoStream,
    val audio: DashAudioStream,
)

object DashVideoSelector {
    fun select(streams: List<DashVideoStream>): DashVideoStream? {
        if (streams.isEmpty()) return null
        // long: 视频下载产物要能在尽可能多的 Android 设备直接播放；同编码族内取最高画质，但 AVC 的兼容性优先于更高画质的 HEVC/AV1。
        val preferredCodecPriority = streams.minOf(::codecPriority)
        return streams
            .asSequence()
            .filter { stream -> codecPriority(stream) == preferredCodecPriority }
            .maxWithOrNull(
                compareBy<DashVideoStream>(DashVideoStream::qualityId)
                    .thenBy { stream -> stream.width.toLong() * stream.height.toLong() }
                    .thenBy(DashVideoStream::bandwidth),
            )
    }

    private fun codecPriority(stream: DashVideoStream): Int {
        val codecs = stream.codecs.lowercase()
        return when {
            codecs.startsWith("avc1") || codecs.startsWith("avc3") -> 0
            codecs.startsWith("hev1") || codecs.startsWith("hvc1") -> 1
            codecs.startsWith("av01") -> 2
            else -> 3
        }
    }
}

object BilibiliText {
    private val htmlTag = Regex("<[^>]+>")
    private val entities = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&nbsp;" to " ",
    )

    fun plainTitle(value: String): String {
        return entities.entries.fold(htmlTag.replace(value, "")) { text, (entity, replacement) ->
            text.replace(entity, replacement)
        }.trim()
    }

    fun httpsUrl(value: String?): String {
        val normalized = value.orEmpty().trim()
        return when {
            normalized.startsWith("//") -> "https:$normalized"
            normalized.startsWith("http://") -> "https://${normalized.removePrefix("http://")}"
            else -> normalized
        }
    }

    fun firstHttpsUrl(vararg values: String?): String {
        return values.firstNotNullOfOrNull { value ->
            httpsUrl(value).takeIf(String::isNotBlank)
        }.orEmpty()
    }
}

object WbiKeyParser {
    fun fromImageUrls(imgUrl: String, subUrl: String): WbiKeys {
        return WbiKeys(
            imgKey = fileStem(imgUrl),
            subKey = fileStem(subUrl),
        ).also { keys ->
            require(keys.imgKey.isNotBlank() && keys.subKey.isNotBlank()) { "WBI key URL is invalid" }
        }
    }

    private fun fileStem(url: String): String {
        return url.substringAfterLast('/').substringBeforeLast('.', missingDelimiterValue = "")
    }
}

object DashAudioSelector {
    fun select(
        preference: AudioQualityPreference,
        flac: DashAudioStream?,
        dolby: List<DashAudioStream>,
        standard: List<DashAudioStream>,
    ): DashAudioStream? {
        return when (preference) {
            AudioQualityPreference.HIGHEST -> flac
                ?: dolby.maxByOrNull(DashAudioStream::bandwidth)
                ?: standard.maxByOrNull(DashAudioStream::bandwidth)
            AudioQualityPreference.DATA_SAVER -> standard.minByOrNull(DashAudioStream::bandwidth)
                ?: dolby.minByOrNull(DashAudioStream::bandwidth)
                ?: flac
        }
    }
}

object StreamUrlExpiry {
    fun epochSeconds(url: String): Long? {
        return runCatching {
            URI(url).rawQuery
                ?.split('&')
                ?.firstOrNull { parameter -> parameter.substringBefore('=') == "deadline" }
                ?.substringAfter('=')
                ?.toLongOrNull()
        }.getOrNull()
    }
}

class BilibiliApiException(
    val code: Int,
    message: String,
) : Exception("Bilibili API $code: $message")
