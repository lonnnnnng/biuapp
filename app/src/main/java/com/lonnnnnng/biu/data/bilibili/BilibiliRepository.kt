package com.lonnnnnng.biu.data.bilibili

import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.Track
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class BilibiliRepository(
    private val client: OkHttpClient,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000L },
    private val apiBase: HttpUrl = API_BASE.toHttpUrl(),
) {
    private val wbiKeyMutex = Mutex()
    private var cachedWbiKeys: CachedWbiKeys? = null

    suspend fun recommendations(feed: RecommendFeed, page: Int = 1): List<BilibiliVideo> {
        return when (feed) {
            RecommendFeed.MUSIC -> regionRecommendations(page)
            RecommendFeed.POPULAR -> popularRecommendations(page)
        }
    }

    suspend fun searchVideos(keyword: String, page: Int = 1): List<BilibiliVideo> {
        val root = request(
            path = "/x/web-interface/wbi/search/type",
            parameters = mapOf(
                "search_type" to "video",
                "keyword" to keyword,
                "page" to page,
                "page_size" to 24,
                "order" to "totalrank",
                "tids" to 3,
            ),
            useWbi = true,
        ).requireSuccess()
        return root.optJSONObject("data")
            ?.optJSONArray("result")
            .toObjects()
            .mapNotNull(::parseSearchVideo)
    }

    suspend fun account(): BilibiliAccount {
        val root = request("/x/web-interface/nav")
        val code = root.optInt("code", Int.MIN_VALUE)
        if (code != 0 && code != -101) {
            throw BilibiliApiException(code, root.optString("message", "账号状态获取失败"))
        }
        val data = root.optJSONObject("data") ?: JSONObject()
        return BilibiliAccount(
            isLoggedIn = data.optBoolean("isLogin", false),
            name = data.optString("uname"),
            faceUrl = BilibiliText.httpsUrl(data.optString("face")),
            mid = data.optLong("mid", 0L),
        )
    }

    suspend fun followingCreators(mid: Long): List<BilibiliCreator> {
        require(mid > 0L) { "登录账号缺少 mid" }
        val creators = mutableListOf<BilibiliCreator>()
        var page = 1
        var total = Int.MAX_VALUE
        // long: 候选范围必须覆盖账号的完整关注列表，以接口 total 和空页共同收敛，避免关注超过 1000 位时静默漏人。
        while (creators.size < total) {
            val root = request(
                path = "/x/relation/followings",
                parameters = mapOf(
                    "vmid" to mid,
                    "pn" to page,
                    "ps" to FOLLOWING_PAGE_SIZE,
                    "order_type" to "",
                ),
            ).requireSuccess()
            val data = root.optJSONObject("data") ?: break
            total = data.optInt("total", 0).coerceAtLeast(0)
            val pageCreators = data.optJSONArray("list")
                .toObjects()
                .mapNotNull(::parseFollowingCreator)
            creators += pageCreators
            if (pageCreators.isEmpty()) break
            page += 1
        }
        return creators.distinctBy(BilibiliCreator::mid)
    }

    suspend fun creatorVideos(creator: BilibiliCreator, page: Int = 1): List<BilibiliVideo> {
        require(creator.mid > 0L) { "UP 主 mid 无效" }
        // long: 首页只读取每位 UP 最新一页投稿，再做跨 UP 时间线合并，控制刷新耗时和接口调用数量。
        val root = request(
            path = "/x/space/wbi/arc/search",
            parameters = mapOf(
                "mid" to creator.mid,
                "pn" to page,
                "ps" to CREATOR_VIDEO_PAGE_SIZE,
                "order" to "pubdate",
            ),
            useWbi = true,
        ).requireSuccess()
        return root.optJSONObject("data")
            ?.optJSONObject("list")
            ?.optJSONArray("vlist")
            .toObjects()
            .mapNotNull { item -> parseCreatorVideo(item, creator) }
    }

    suspend fun favoriteFolders(mid: Long, page: Int = 1): List<BilibiliFavoriteFolder> {
        require(mid > 0L) { "登录账号缺少 mid" }
        val root = request(
            path = "/x/v3/fav/folder/created/list",
            parameters = mapOf("up_mid" to mid, "pn" to page, "ps" to 20),
        ).requireSuccess()
        return root.optJSONObject("data")
            ?.optJSONArray("list")
            .toObjects()
            .mapNotNull(::parseFavoriteFolder)
    }

    suspend fun favoriteVideos(folderId: Long, page: Int = 1): List<BilibiliLibraryVideo> {
        return favoriteVideoPage(folderId, page).videos
    }

    suspend fun favoriteVideoPage(folderId: Long, page: Int = 1): BilibiliFavoriteVideoPage {
        val root = request(
            path = "/x/v3/fav/resource/list",
            parameters = mapOf(
                "media_id" to folderId,
                "pn" to page,
                "ps" to FAVORITE_PAGE_SIZE,
                "order" to "mtime",
                "platform" to "web",
            ),
        ).requireSuccess()
        val data = root.optJSONObject("data") ?: JSONObject()
        val rawMedias = data.optJSONArray("medias")
        val videos = rawMedias.toObjects().mapNotNull(::parseFavoriteVideo)
        return BilibiliFavoriteVideoPage(
            videos = videos,
            // long: 失效稿件会被播放器过滤，是否继续翻页必须使用接口原始 has_more，不能按过滤后的可播放数量提前停止。
            hasMore = if (data.has("has_more")) {
                data.optBoolean("has_more", false)
            } else {
                (rawMedias?.length() ?: 0) >= FAVORITE_PAGE_SIZE
            },
        )
    }

    suspend fun favoriteVideosAll(folderId: Long): List<BilibiliLibraryVideo> {
        val videos = mutableListOf<BilibiliLibraryVideo>()
        var page = 1
        while (page <= MAX_FAVORITE_PAGES) {
            val result = favoriteVideoPage(folderId, page)
            videos += result.videos
            if (!result.hasMore) return videos
            page += 1
        }
        throw BilibiliApiException(-429, "收藏夹分页过多，请缩小下载范围")
    }

    suspend fun watchLater(page: Int = 1): List<BilibiliLibraryVideo> {
        val root = request(
            path = "/x/v2/history/toview/web",
            parameters = mapOf("pn" to page, "ps" to 20),
            useWbi = true,
        ).requireSuccess()
        return root.optJSONObject("data")
            ?.optJSONArray("list")
            .toObjects()
            .mapNotNull(::parseWatchLaterVideo)
    }

    suspend fun onlineHistory(): List<BilibiliLibraryVideo> {
        val root = request(
            path = "/x/web-interface/history/cursor",
            parameters = mapOf(
                "max" to 0,
                "view_at" to 0,
                "type" to "archive",
                "ps" to 20,
            ),
        ).requireSuccess()
        return root.optJSONObject("data")
            ?.optJSONArray("list")
            .toObjects()
            .mapNotNull(::parseOnlineHistoryVideo)
    }

    suspend fun videoDetail(bvid: String): BilibiliVideoDetail {
        val root = request(
            path = "/x/web-interface/view",
            parameters = mapOf("bvid" to bvid),
        ).requireSuccess()
        val data = root.getJSONObject("data")
        val cover = BilibiliText.httpsUrl(data.optString("pic"))
        val pages = data.optJSONArray("pages")
            .toObjects()
            .mapNotNull { page ->
                val cid = page.optLong("cid", 0L)
                if (cid == 0L) return@mapNotNull null
                BilibiliVideoPage(
                    cid = cid,
                    page = page.optInt("page", 1),
                    title = BilibiliText.plainTitle(page.optString("part")),
                    durationSeconds = page.optInt("duration", 0),
                    coverUrl = BilibiliText.httpsUrl(page.optString("first_frame")).ifBlank { null },
                )
            }
        return BilibiliVideoDetail(
            bvid = data.optString("bvid", bvid),
            title = BilibiliText.plainTitle(data.optString("title")),
            author = data.optJSONObject("owner")?.optString("name").orEmpty(),
            coverUrl = cover,
            pages = pages,
        )
    }

    suspend fun resolveTrack(
        video: BilibiliVideo,
        pageIndex: Int = 0,
        qualityPreference: AudioQualityPreference = AudioQualityPreference.HIGHEST,
    ): Track {
        val detail = videoDetail(video.bvid)
        return resolveTrack(video, detail, pageIndex, qualityPreference)
    }

    suspend fun resolveTracks(
        video: BilibiliVideo,
        qualityPreference: AudioQualityPreference = AudioQualityPreference.HIGHEST,
    ): List<Track> {
        return resolveTracks(video, videoDetail(video.bvid), qualityPreference)
    }

    suspend fun resolveTracks(
        video: BilibiliVideo,
        detail: BilibiliVideoDetail,
        qualityPreference: AudioQualityPreference = AudioQualityPreference.HIGHEST,
    ): List<Track> {
        if (detail.pages.isEmpty()) throw BilibiliApiException(-404, "视频没有可播放分 P")
        return detail.pages.mapIndexed { pageIndex, _ ->
            resolveTrack(video, detail, pageIndex, qualityPreference)
        }
    }

    internal suspend fun resolveTrack(
        video: BilibiliVideo,
        detail: BilibiliVideoDetail,
        pageIndex: Int,
        qualityPreference: AudioQualityPreference,
    ): Track {
        val page = detail.pages.getOrNull(pageIndex)
            ?: throw BilibiliApiException(-404, "视频没有可播放分 P")
        val stream = resolveAudioStream(detail.bvid, page.cid, qualityPreference)
        val title = if (detail.pages.size > 1) {
            "${detail.title} · ${page.title.ifBlank { "第 ${page.page} P" }}"
        } else {
            detail.title
        }
        // long: 多 P 名称独立写入 Media3 subtitle，避免总视频标题过长时看不到当前播放的具体歌曲。
        val pageTitle = if (detail.pages.size > 1) {
            page.title.takeIf(String::isNotBlank)?.let { "P${page.page} · $it" } ?: "P${page.page}"
        } else {
            null
        }
        return Track(
            id = "${detail.bvid}:${page.cid}",
            title = title,
            artist = detail.author.ifBlank { video.author },
            streamUrl = stream.url,
            artworkUrl = page.coverUrl ?: detail.coverUrl.ifBlank { video.coverUrl },
            qualityLabel = stream.qualityLabel,
            pageTitle = pageTitle,
            source = BilibiliTrackSource(detail.bvid, page.cid, qualityPreference),
        )
    }

    suspend fun resolveTrack(
        source: BilibiliTrackSource,
        title: String,
        artist: String,
        artworkUrl: String?,
    ): Track {
        // long: 本地历史只持久化稳定的 bvid/cid；恢复时重新读取视频详情，才能按 cid 找回当前分 P 名称并同步给锁屏媒体卡片。
        val detail = videoDetail(source.bvid)
        val pageIndex = detail.pages.indexOfFirst { page -> page.cid == source.cid }
        if (pageIndex < 0) throw BilibiliApiException(-404, "历史对应的分 P 已不存在")
        return resolveTrack(
            video = BilibiliVideo(
                bvid = source.bvid,
                aid = null,
                title = title,
                author = artist,
                coverUrl = artworkUrl.orEmpty(),
                durationSeconds = null,
                playCount = null,
            ),
            detail = detail,
            pageIndex = pageIndex,
            qualityPreference = source.qualityPreference,
        )
    }

    suspend fun resolveAudioStream(
        bvid: String,
        cid: Long,
        qualityPreference: AudioQualityPreference = AudioQualityPreference.HIGHEST,
    ): DashAudioStream {
        val streams = resolveDashStreams(bvid, cid)
        return DashAudioSelector.select(qualityPreference, streams.flac, streams.dolby, streams.standard)
            ?: throw BilibiliApiException(-404, "没有可用音频流")
    }

    suspend fun resolveStandardAudioStream(bvid: String, cid: Long): DashAudioStream {
        val streams = resolveDashStreams(bvid, cid)
        // long: 首版离线音频直接发布为 m4a，必须选 B 站标准 AAC 轨；FLAC/杜比仍需容器转换，不能仅改扩展名后交给 MediaStore。
        return streams.standard.maxByOrNull(DashAudioStream::bandwidth)
            ?: throw BilibiliApiException(-404, "没有可下载的标准 AAC 音频")
    }

    suspend fun resolveVideoDownloadStreams(bvid: String, cid: Long): DashDownloadStreams {
        val streams = resolveDashStreams(bvid, cid)
        val video = DashVideoSelector.select(streams.video)
            ?: throw BilibiliApiException(-404, "没有可下载的视频轨")
        val audio = streams.standard.maxByOrNull(DashAudioStream::bandwidth)
            ?: throw BilibiliApiException(-404, "没有可下载的标准 AAC 音频")
        return DashDownloadStreams(video = video, audio = audio)
    }

    private suspend fun resolveDashStreams(bvid: String, cid: Long): ParsedDashStreams {
        val root = request(
            path = "/x/player/wbi/playurl",
            parameters = mapOf(
                "bvid" to bvid,
                "cid" to cid,
                "fnval" to 4048,
                "fnver" to 0,
                "fourk" to 1,
            ),
            useWbi = true,
        ).requireSuccess()
        val dash = root.optJSONObject("data")?.optJSONObject("dash")
            ?: throw BilibiliApiException(-404, "没有 DASH 音频")
        val flac = dash.optJSONObject("flac")
            ?.optJSONObject("audio")
            ?.let { parseAudio(it, "无损") }
        val dolby = dash.optJSONObject("dolby")
            ?.optJSONArray("audio")
            .toObjects()
            .mapNotNull { parseAudio(it, "杜比") }
        val standard = dash.optJSONArray("audio")
            .toObjects()
            .mapNotNull { parseAudio(it, "${it.optInt("bandwidth") / 1000} kbps") }
        val video = dash.optJSONArray("video")
            .toObjects()
            .mapNotNull(::parseVideo)
        return ParsedDashStreams(video = video, flac = flac, dolby = dolby, standard = standard)
    }

    private suspend fun regionRecommendations(page: Int): List<BilibiliVideo> {
        val root = request(
            path = "/x/web-interface/region/feed/rcmd",
            parameters = mapOf(
                "display_id" to page,
                "request_cnt" to 15,
                "from_region" to 1003,
                "device" to "web",
                "plat" to 30,
                "web_location" to "333.40138",
            ),
            useWbi = true,
        ).requireSuccess()
        return root.optJSONObject("data")
            ?.optJSONArray("archives")
            .toObjects()
            .mapNotNull(::parseRegionVideo)
    }

    private suspend fun popularRecommendations(page: Int): List<BilibiliVideo> {
        val root = request(
            path = "/x/centralization/interface/music/comprehensive/web/rank",
            parameters = mapOf(
                "pn" to page,
                "ps" to 20,
                "web_location" to "333.1351",
            ),
        ).requireSuccess()
        return root.optJSONObject("data")
            ?.optJSONArray("list")
            .toObjects()
            .mapNotNull(::parsePopularVideo)
    }

    private suspend fun request(
        path: String,
        parameters: Map<String, Any?> = emptyMap(),
        useWbi: Boolean = false,
    ): JSONObject {
        val url = if (useWbi) {
            val keys = currentWbiKeys()
            val signed = WbiSigner.sign(parameters, keys.imgKey, keys.subKey, nowEpochSeconds())
            buildUrl(path, emptyMap()).newBuilder()
                .encodedQuery(signed.encodedQuery)
                .build()
        } else {
            buildUrl(path, parameters)
        }
        return execute(url)
    }

    private suspend fun currentWbiKeys(): WbiKeys = wbiKeyMutex.withLock {
        cachedWbiKeys
            ?.takeIf { cached -> nowEpochSeconds() < cached.expiresAtEpochSeconds }
            ?.keys
            ?: fetchWbiKeys().also { keys ->
                cachedWbiKeys = CachedWbiKeys(keys, nowEpochSeconds() + WBI_CACHE_SECONDS)
            }
    }

    private suspend fun fetchWbiKeys(): WbiKeys {
        val root = execute(buildUrl("/x/web-interface/nav", emptyMap()))
        val wbi = root.optJSONObject("data")?.optJSONObject("wbi_img")
            ?: throw BilibiliApiException(root.optInt("code", -1), "无法获取 WBI key")
        return WbiKeyParser.fromImageUrls(
            imgUrl = wbi.optString("img_url"),
            subUrl = wbi.optString("sub_url"),
        )
    }

    private suspend fun execute(url: HttpUrl): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Bilibili HTTP ${response.code}")
            }
            runCatching { JSONObject(payload) }
                .getOrElse { throw IOException("Bilibili response is not JSON", it) }
        }
    }

    private fun buildUrl(path: String, parameters: Map<String, Any?>): HttpUrl {
        return apiBase.newBuilder()
            .addPathSegments(path.removePrefix("/"))
            .apply {
            parameters.forEach { (key, value) ->
                if (value != null) addQueryParameter(key, value.toString())
            }
        }.build()
    }

    private fun parseRegionVideo(item: JSONObject): BilibiliVideo? {
        val bvid = item.optString("bvid").takeIf(String::isNotBlank) ?: return null
        return BilibiliVideo(
            bvid = bvid,
            aid = item.optLongOrNull("aid"),
            title = BilibiliText.plainTitle(item.optString("title")),
            author = item.optJSONObject("author")?.optString("name").orEmpty(),
            // long: 音乐区推荐会随卡片版本切换封面字段，按兼容顺序取第一个有效地址，避免列表只显示占位色块。
            coverUrl = BilibiliText.firstHttpsUrl(
                item.optString("cover"),
                item.optString("cover_pic"),
                item.optString("pic"),
            ),
            durationSeconds = item.optIntOrNull("duration"),
            playCount = item.optJSONObject("stat")?.optLongOrNull("view"),
            publishedAtEpochSeconds = item.optLongOrNull("pubdate") ?: item.optLongOrNull("ctime"),
        )
    }

    private fun parsePopularVideo(item: JSONObject): BilibiliVideo? {
        val archive = item.optJSONObject("related_archive")
        val bvid = archive?.optString("bvid").orEmpty()
            .ifBlank { item.optString("bvid") }
            .takeIf(String::isNotBlank) ?: return null
        return BilibiliVideo(
            bvid = bvid,
            aid = archive?.optLongOrNull("aid") ?: item.optLongOrNull("aid"),
            title = BilibiliText.plainTitle(archive?.optString("title").orEmpty().ifBlank { item.optString("music_title") }),
            author = archive?.optString("username").orEmpty().ifBlank { item.optString("author") },
            coverUrl = BilibiliText.httpsUrl(archive?.optString("cover").orEmpty().ifBlank { item.optString("cover") }),
            durationSeconds = archive?.optIntOrNull("duration"),
            playCount = archive?.optLongOrNull("vv_count"),
            publishedAtEpochSeconds = archive?.optLongOrNull("pubdate")
                ?: archive?.optLongOrNull("pubtime")
                ?: item.optLongOrNull("pubdate"),
        )
    }

    private fun parseSearchVideo(item: JSONObject): BilibiliVideo? {
        val bvid = item.optString("bvid").takeIf(String::isNotBlank) ?: return null
        return BilibiliVideo(
            bvid = bvid,
            aid = item.optLongOrNull("aid"),
            title = BilibiliText.plainTitle(item.optString("title")),
            author = item.optString("author"),
            coverUrl = BilibiliText.httpsUrl(item.optString("pic")),
            durationSeconds = parseDuration(item.optString("duration")),
            playCount = item.optLongOrNull("play"),
            publishedAtEpochSeconds = item.optLongOrNull("pubdate"),
        )
    }

    internal fun parseFollowingCreator(item: JSONObject): BilibiliCreator? {
        // long: 配置需要离线展示已选 UP，因此关注响应同时固化 UID、名称和头像，而不是只保存查询参数 UID。
        val mid = item.optLong("mid", 0L).takeIf { it > 0L } ?: return null
        val name = item.optString("uname").trim().takeIf(String::isNotBlank) ?: return null
        return BilibiliCreator(
            mid = mid,
            name = name,
            faceUrl = BilibiliText.httpsUrl(item.optString("face")),
        )
    }

    internal fun parseCreatorVideo(item: JSONObject, creator: BilibiliCreator): BilibiliVideo? {
        // long: 空间投稿响应可能不回传作者名，使用已保存的 UP 资料兜底，保证合并列表第三行始终可识别来源。
        val bvid = item.optString("bvid").takeIf(String::isNotBlank) ?: return null
        return BilibiliVideo(
            bvid = bvid,
            aid = item.optLongOrNull("aid"),
            title = BilibiliText.plainTitle(item.optString("title")),
            author = item.optString("author").ifBlank { creator.name },
            coverUrl = BilibiliText.httpsUrl(item.optString("pic")),
            durationSeconds = parseDuration(item.optString("length")) ?: item.optIntOrNull("duration"),
            playCount = item.optLongOrNull("play"),
            publishedAtEpochSeconds = item.optLongOrNull("created") ?: item.optLongOrNull("pubdate"),
        )
    }

    internal fun parseFavoriteFolder(item: JSONObject): BilibiliFavoriteFolder? {
        val id = item.optLong("id", 0L).takeIf { it > 0L } ?: return null
        return BilibiliFavoriteFolder(
            id = id,
            title = BilibiliText.plainTitle(item.optString("title")).ifBlank { "未命名收藏夹" },
            coverUrl = BilibiliText.httpsUrl(item.optString("cover")),
            mediaCount = item.optInt("media_count", 0),
        )
    }

    internal fun parseFavoriteVideo(item: JSONObject): BilibiliLibraryVideo? {
        // long: 收藏夹还可能包含音频、合集和已失效稿件；当前播放器只接收可重新解析 DASH 的普通视频。
        if (item.optInt("type", 0) != 2 || item.optInt("attr", 0) != 0) return null
        val bvid = item.optString("bvid").ifBlank { item.optString("bv_id") }
            .takeIf(String::isNotBlank) ?: return null
        val video = BilibiliVideo(
            bvid = bvid,
            aid = item.optLongOrNull("id"),
            title = BilibiliText.plainTitle(item.optString("title")),
            author = item.optJSONObject("upper")?.optString("name").orEmpty(),
            coverUrl = BilibiliText.httpsUrl(item.optString("cover")),
            durationSeconds = item.optIntOrNull("duration"),
            playCount = item.optJSONObject("cnt_info")?.optLongOrNull("play"),
        )
        return BilibiliLibraryVideo(video, savedAtEpochSeconds = item.optLongOrNull("fav_time"))
    }

    internal fun parseWatchLaterVideo(item: JSONObject): BilibiliLibraryVideo? {
        val bvid = item.optString("bvid").takeIf(String::isNotBlank) ?: return null
        val video = BilibiliVideo(
            bvid = bvid,
            aid = item.optLongOrNull("aid"),
            title = BilibiliText.plainTitle(item.optString("title")),
            author = item.optJSONObject("owner")?.optString("name").orEmpty(),
            coverUrl = BilibiliText.httpsUrl(item.optString("pic")),
            durationSeconds = item.optIntOrNull("duration"),
            playCount = item.optJSONObject("stat")?.optLongOrNull("view"),
        )
        return BilibiliLibraryVideo(
            video = video,
            progressSeconds = item.optIntOrNull("progress"),
            savedAtEpochSeconds = item.optLongOrNull("add_at"),
        )
    }

    internal fun parseOnlineHistoryVideo(item: JSONObject): BilibiliLibraryVideo? {
        val history = item.optJSONObject("history") ?: return null
        val bvid = history.optString("bvid").takeIf(String::isNotBlank) ?: return null
        val video = BilibiliVideo(
            bvid = bvid,
            aid = history.optLongOrNull("oid"),
            title = BilibiliText.plainTitle(item.optString("title")),
            author = item.optString("author_name"),
            coverUrl = BilibiliText.firstHttpsUrl(item.optString("cover"), item.optJSONArray("covers")?.optString(0)),
            durationSeconds = item.optIntOrNull("duration"),
            playCount = null,
        )
        return BilibiliLibraryVideo(
            video = video,
            progressSeconds = item.optIntOrNull("progress"),
            savedAtEpochSeconds = item.optLongOrNull("view_at"),
        )
    }

    private fun parseAudio(item: JSONObject, qualityLabel: String): DashAudioStream? {
        val backupUrls = (item.optJSONArray("backupUrl").toStrings() + item.optJSONArray("backup_url").toStrings())
            .map(BilibiliText::httpsUrl)
            .filter(String::isNotBlank)
            .distinct()
        val url = BilibiliText.firstHttpsUrl(
            item.optString("baseUrl"),
            item.optString("base_url"),
            backupUrls.firstOrNull(),
        )
        if (url.isBlank()) return null
        return DashAudioStream(
            url = url,
            bandwidth = item.optLong("bandwidth", 0L),
            codecs = item.optString("codecs"),
            qualityLabel = qualityLabel,
            expiresAtEpochSeconds = StreamUrlExpiry.epochSeconds(url),
            backupUrls = backupUrls.filterNot { backupUrl -> backupUrl == url },
        )
    }

    private fun parseVideo(item: JSONObject): DashVideoStream? {
        val backupUrls = (item.optJSONArray("backupUrl").toStrings() + item.optJSONArray("backup_url").toStrings())
            .map(BilibiliText::httpsUrl)
            .filter(String::isNotBlank)
            .distinct()
        val url = BilibiliText.firstHttpsUrl(
            item.optString("baseUrl"),
            item.optString("base_url"),
            backupUrls.firstOrNull(),
        )
        if (url.isBlank()) return null
        val codecs = item.optString("codecs")
        val width = item.optInt("width", 0)
        val height = item.optInt("height", 0)
        val frameRate = parseFrameRate(item.optString("frameRate").ifBlank { item.optString("frame_rate") })
        val codecLabel = when {
            codecs.startsWith("avc", ignoreCase = true) -> "AVC"
            codecs.startsWith("hev", ignoreCase = true) || codecs.startsWith("hvc", ignoreCase = true) -> "HEVC"
            codecs.startsWith("av01", ignoreCase = true) -> "AV1"
            else -> codecs.substringBefore('.').uppercase().ifBlank { "视频" }
        }
        val resolutionLabel = height.takeIf { it > 0 }?.let { "${it}p" }
            ?: "清晰度 ${item.optInt("id", 0)}"
        val frameRateLabel = frameRate.takeIf { it >= 50.0 }?.roundToInt()?.let { " · ${it}fps" }.orEmpty()
        return DashVideoStream(
            url = url,
            qualityId = item.optInt("id", 0),
            bandwidth = item.optLong("bandwidth", 0L),
            codecs = codecs,
            width = width,
            height = height,
            frameRate = frameRate,
            qualityLabel = "$resolutionLabel$frameRateLabel · $codecLabel",
            expiresAtEpochSeconds = StreamUrlExpiry.epochSeconds(url),
            backupUrls = backupUrls.filterNot { backupUrl -> backupUrl == url },
        )
    }

    private fun JSONObject.requireSuccess(): JSONObject {
        val code = optInt("code", Int.MIN_VALUE)
        if (code != 0) throw BilibiliApiException(code, optString("message", "请求失败"))
        return this
    }

    private data class CachedWbiKeys(
        val keys: WbiKeys,
        val expiresAtEpochSeconds: Long,
    )

    private companion object {
        const val API_BASE = "https://api.bilibili.com/"
        const val FOLLOWING_PAGE_SIZE = 50
        const val CREATOR_VIDEO_PAGE_SIZE = 30
        const val FAVORITE_PAGE_SIZE = 20
        const val MAX_FAVORITE_PAGES = 100
        val WBI_CACHE_SECONDS = TimeUnit.HOURS.toSeconds(6)
    }
}

private data class ParsedDashStreams(
    val video: List<DashVideoStream>,
    val flac: DashAudioStream?,
    val dolby: List<DashAudioStream>,
    val standard: List<DashAudioStream>,
)

private fun parseFrameRate(value: String): Double {
    val numerator = value.substringBefore('/').toDoubleOrNull() ?: return 0.0
    val denominator = value.substringAfter('/', missingDelimiterValue = "1").toDoubleOrNull() ?: return 0.0
    return if (denominator > 0.0) numerator / denominator else 0.0
}

private fun JSONArray?.toObjects(): List<JSONObject> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let(::add)
        }
    }
}

private fun JSONArray?.toStrings(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }
}

private fun JSONObject.optLongOrNull(name: String): Long? {
    if (!has(name) || isNull(name)) return null
    return optString(name).toLongOrNull() ?: optLong(name).takeIf { it != 0L }
}

private fun JSONObject.optIntOrNull(name: String): Int? {
    if (!has(name) || isNull(name)) return null
    return optInt(name).takeIf { it != 0 }
}

private fun parseDuration(value: String): Int? {
    if (value.isBlank()) return null
    val parts = value.split(':').mapNotNull(String::toIntOrNull)
    return when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> value.toIntOrNull()
    }
}
