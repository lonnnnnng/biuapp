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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class BilibiliRepository(
    private val client: OkHttpClient,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000L },
    private val apiBase: HttpUrl = API_BASE.toHttpUrl(),
    private val csrfProvider: () -> String? = { null },
) {
    private val wbiKeyMutex = Mutex()
    private var cachedWbiKeys: CachedWbiKeys? = null

    suspend fun recommendations(feed: RecommendFeed, page: Int = 1): BilibiliRecommendationPage {
        val normalizedPage = page.coerceAtLeast(1)
        return when (feed) {
            RecommendFeed.COMPREHENSIVE -> comprehensivePopularRecommendations(normalizedPage)
            RecommendFeed.WEEKLY -> weeklyPopularRecommendations(normalizedPage)
            RecommendFeed.RANKING -> rankingRecommendations(normalizedPage)
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

    suspend fun searchCreators(keyword: String, page: Int = 1): BilibiliCreatorPage {
        val normalizedKeyword = keyword.trim()
        require(normalizedKeyword.isNotBlank()) { "UP 主搜索关键词不能为空" }
        val normalizedPage = page.coerceAtLeast(1)
        val data = request(
            path = "/x/web-interface/wbi/search/type",
            parameters = mapOf(
                "search_type" to "bili_user",
                "keyword" to normalizedKeyword,
                "page" to normalizedPage,
            ),
            useWbi = true,
        ).requireSuccess().optJSONObject("data") ?: JSONObject()
        val creators = data.optJSONArray("result")
            .toObjects()
            .mapNotNull(::parseSearchCreator)
        val total = data.optInt("numResults", 0).coerceAtLeast(0)
        val pageCount = data.optInt("numPages", 0).coerceAtLeast(0)
        return BilibiliCreatorPage(
            creators = creators,
            page = normalizedPage,
            hasMore = if (pageCount > 0) normalizedPage < pageCount else creators.size >= CREATOR_SEARCH_PAGE_SIZE,
            total = total,
        )
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

    suspend fun followingCreatorsPage(mid: Long, page: Int = 1): BilibiliCreatorPage {
        require(mid > 0L) { "登录账号缺少 mid" }
        val normalizedPage = page.coerceAtLeast(1)
        val data = request(
            path = "/x/relation/followings",
            parameters = mapOf(
                "vmid" to mid,
                "pn" to normalizedPage,
                "ps" to FOLLOWING_PAGE_SIZE,
                "order_type" to "",
            ),
        ).requireSuccess().optJSONObject("data") ?: JSONObject()
        val total = data.optInt("total", 0).coerceAtLeast(0)
        val creators = data.optJSONArray("list")
            .toObjects()
            .mapNotNull(::parseFollowingCreator)
        return BilibiliCreatorPage(
            creators = creators,
            page = normalizedPage,
            hasMore = normalizedPage * FOLLOWING_PAGE_SIZE < total && creators.isNotEmpty(),
            total = total,
        )
    }

    suspend fun followingCreators(mid: Long): List<BilibiliCreator> {
        require(mid > 0L) { "登录账号缺少 mid" }
        val creators = mutableListOf<BilibiliCreator>()
        var page = 1
        var total = Int.MAX_VALUE
        // long: 候选范围必须覆盖账号的完整关注列表，以接口 total 和空页共同收敛，避免关注超过 1000 位时静默漏人。
        while (creators.size < total) {
            val result = followingCreatorsPage(mid = mid, page = page)
            total = result.total ?: 0
            creators += result.creators
            // long: 全量配置读取以实际累计数量收敛；不能只按页码乘 page size，否则接口偶发返回短页时会提前截断关注列表。
            if (result.creators.isEmpty() || creators.size >= total) break
            page += 1
        }
        return creators.distinctBy(BilibiliCreator::mid)
    }

    suspend fun creatorVideos(creator: BilibiliCreator, page: Int = 1): List<BilibiliVideo> {
        return creatorVideoPage(creator, page).videos
    }

    suspend fun creatorVideoPage(creator: BilibiliCreator, page: Int = 1): BilibiliCreatorVideoPage {
        require(creator.mid > 0L) { "UP 主 mid 无效" }
        val normalizedPage = page.coerceAtLeast(1)
        val data = request(
            path = "/x/space/wbi/arc/search",
            parameters = mapOf(
                "mid" to creator.mid,
                "pn" to normalizedPage,
                "ps" to CREATOR_VIDEO_PAGE_SIZE,
                "order" to "pubdate",
            ),
            useWbi = true,
        ).requireSuccess().optJSONObject("data") ?: JSONObject()
        val videos = data.optJSONObject("list")
            ?.optJSONArray("vlist")
            .toObjects()
            .mapNotNull { item -> parseCreatorVideo(item, creator) }
        val total = data.optJSONObject("page")?.optInt("count", 0)?.coerceAtLeast(0)
        return BilibiliCreatorVideoPage(
            videos = videos,
            page = normalizedPage,
            hasMore = if (total != null) {
                normalizedPage * CREATOR_VIDEO_PAGE_SIZE < total && videos.isNotEmpty()
            } else {
                videos.size >= CREATOR_VIDEO_PAGE_SIZE
            },
            total = total,
        )
    }

    suspend fun creatorProfile(mid: Long): BilibiliCreator {
        require(mid > 0L) { "UP 主 mid 无效" }
        val data = request(
            path = "/x/space/wbi/acc/info",
            parameters = mapOf("mid" to mid),
            useWbi = true,
        ).requireSuccess().optJSONObject("data") ?: throw BilibiliApiException(-1, "UP 主资料为空")
        return parseCreatorProfile(data) ?: throw BilibiliApiException(-1, "UP 主资料无效")
    }

    suspend fun dynamicFeed(offset: String? = null): BilibiliDynamicPage {
        val data = request(
            path = "/x/polymer/web-dynamic/v1/feed/all",
            parameters = buildMap {
                put("type", "video")
                put("platform", "web")
                put("web_location", "333.1365")
                put("features", DYNAMIC_FEATURES)
                offset?.takeIf(String::isNotBlank)?.let { put("offset", it) }
            },
        ).requireSuccess().optJSONObject("data") ?: JSONObject()
        val hasMore = data.optBoolean("has_more", false)
        return BilibiliDynamicPage(
            items = data.optJSONArray("items").toObjects().mapNotNull(::parseDynamicItem),
            // long: 动态接口可能返回失效或非视频项，翻页只能信任服务端游标，不能用过滤后的卡片数量判断。
            nextOffset = data.optString("offset").takeIf { hasMore && it.isNotBlank() },
            hasMore = hasMore,
        )
    }

    suspend fun updateDynamicLike(dynamicId: String, liked: Boolean) {
        require(dynamicId.isNotBlank()) { "动态 id 无效" }
        // long: 新版动态点赞只接受 JSON 正文，CSRF 仍位于 URL；沿用表单会返回参数错误 4100001。
        postJson(
            path = "/x/dynamic/feed/dyn/thumb",
            queryParameters = mapOf("csrf" to requireCsrf()),
            payload = JSONObject()
                .put("dyn_id_str", dynamicId)
                .put("up", if (liked) 1 else 2),
        ).requireSuccess()
    }

    suspend fun tripleLike(bvid: String): BilibiliTripleResult {
        require(bvid.isNotBlank()) { "视频 bvid 无效" }
        val data = postForm(
            path = "/x/web-interface/archive/like/triple",
            parameters = mapOf(
                "bvid" to bvid,
                "csrf" to requireCsrf(),
            ),
        ).requireSuccess().optJSONObject("data") ?: JSONObject()
        return BilibiliTripleResult(
            liked = data.optBoolean("like", false),
            coined = data.optBoolean("coin", false),
            favorited = data.optBoolean("fav", false),
            coinCount = data.optInt("multiply", 0).coerceAtLeast(0),
        )
    }

    suspend fun creatorRelation(mid: Long): BilibiliCreatorRelation {
        require(mid > 0L) { "UP 主 mid 无效" }
        val data = request(
            path = "/x/relation",
            parameters = mapOf("fid" to mid),
        ).requireSuccess().optJSONObject("data") ?: JSONObject()
        return BilibiliCreatorRelation.fromAttribute(data.optInt("attribute", -1))
    }

    suspend fun modifyCreatorRelation(mid: Long, following: Boolean) {
        require(mid > 0L) { "UP 主 mid 无效" }
        postForm(
            path = "/x/relation/modify",
            parameters = mapOf(
                "fid" to mid.toString(),
                "act" to if (following) "1" else "2",
                "re_src" to "11",
                "csrf" to requireCsrf(),
            ),
        ).requireSuccess()
    }

    suspend fun createdFavoriteFolders(mid: Long): List<BilibiliFavoriteFolder> {
        return favoriteFolders(
            mid = mid,
            path = "/x/v3/fav/folder/created/list",
            group = BilibiliFavoriteFolderGroup.CREATED,
            includeVideoCollections = false,
        )
    }

    suspend fun collectedFavoriteFolders(mid: Long): List<BilibiliFavoriteFolder> {
        return favoriteFolders(
            mid = mid,
            path = "/x/v3/fav/folder/collected/list",
            group = BilibiliFavoriteFolderGroup.COLLECTED,
            includeVideoCollections = true,
        )
    }

    suspend fun createFavoriteFolder(title: String): BilibiliFavoriteFolder {
        val normalizedTitle = requireFavoriteFolderTitle(title)
        val root = postForm(
            path = "/x/v3/fav/folder/add",
            parameters = mapOf(
                "title" to normalizedTitle,
                "privacy" to "0",
                "csrf" to requireCsrf(),
            ),
        ).requireSuccess()
        return root.optJSONObject("data")
            ?.let { data -> parseFavoriteFolder(data, BilibiliFavoriteFolderGroup.CREATED) }
            ?: throw BilibiliApiException(-1, "新建收藏夹后未返回收藏夹信息")
    }

    suspend fun renameFavoriteFolder(folderId: Long, title: String) {
        require(folderId > 0L) { "收藏夹 id 无效" }
        postForm(
            path = "/x/v3/fav/folder/edit",
            parameters = mapOf(
                "media_id" to folderId.toString(),
                "title" to requireFavoriteFolderTitle(title),
                "csrf" to requireCsrf(),
            ),
        ).requireSuccess()
    }

    suspend fun deleteFavoriteFolder(folderId: Long) {
        require(folderId > 0L) { "收藏夹 id 无效" }
        postForm(
            path = "/x/v3/fav/folder/del",
            parameters = mapOf(
                "media_ids" to folderId.toString(),
                "csrf" to requireCsrf(),
            ),
        ).requireSuccess()
    }

    suspend fun addVideoToFavorite(aid: Long, folderId: Long) {
        updateVideoFavorite(aid = aid, folderId = folderId, adding = true)
    }

    suspend fun removeVideoFromFavorite(aid: Long, folderId: Long) {
        updateVideoFavorite(aid = aid, folderId = folderId, adding = false)
    }

    suspend fun createdFavoriteFolderMemberships(
        mid: Long,
        aid: Long,
    ): List<BilibiliFavoriteFolderMembership> {
        require(mid > 0L) { "账号 mid 无效" }
        require(aid > 0L) { "视频 aid 无效" }
        val data = request(
            path = "/x/v3/fav/folder/created/list-all",
            parameters = mapOf(
                "up_mid" to mid,
                "type" to "2",
                "rid" to aid,
            ),
        ).requireSuccess().optJSONObject("data") ?: JSONObject()
        return data.optJSONArray("list").toObjects().mapNotNull { item ->
            parseFavoriteFolder(item, BilibiliFavoriteFolderGroup.CREATED)?.let { folder ->
                BilibiliFavoriteFolderMembership(
                    folder = folder,
                    containsVideo = item.optInt("fav_state", 0) == 1,
                )
            }
        }
    }

    private suspend fun favoriteFolders(
        mid: Long,
        path: String,
        group: BilibiliFavoriteFolderGroup,
        includeVideoCollections: Boolean,
    ): List<BilibiliFavoriteFolder> {
        require(mid > 0L) { "登录账号缺少 mid" }
        val folders = mutableListOf<BilibiliFavoriteFolder>()
        var page = 1
        while (page <= MAX_FAVORITE_FOLDER_PAGES) {
            val parameters = buildMap<String, Any?> {
                put("up_mid", mid)
                put("pn", page)
                put("ps", FAVORITE_FOLDER_PAGE_SIZE)
                // long: 只有 Web 平台响应会把用户收藏的视频合集并入“我收藏的”，缺少该参数会造成账号页数据不完整。
                if (includeVideoCollections) put("platform", "web")
            }
            val data = request(path = path, parameters = parameters)
                .requireSuccess()
                .optJSONObject("data") ?: break
            val rawItems = data.optJSONArray("list").toObjects()
            folders += rawItems.mapNotNull { item -> parseFavoriteFolder(item, group) }
            val total = data.optInt("count", 0).coerceAtLeast(0)
            val hasMore = when {
                data.has("has_more") -> data.optBoolean("has_more", false)
                total > 0 -> folders.size < total
                else -> rawItems.size >= FAVORITE_FOLDER_PAGE_SIZE
            }
            if (!hasMore || rawItems.isEmpty()) break
            page += 1
        }
        return folders.distinctBy { folder -> folder.group to folder.id }
    }

    suspend fun favoriteVideos(folder: BilibiliFavoriteFolder, page: Int = 1): List<BilibiliLibraryVideo> {
        return favoriteVideoPage(folder, page).videos
    }

    suspend fun favoriteVideos(folderId: Long, page: Int = 1): List<BilibiliLibraryVideo> {
        return favoriteVideoPage(folderId, page).videos
    }

    suspend fun favoriteVideoPage(folder: BilibiliFavoriteFolder, page: Int = 1): BilibiliFavoriteVideoPage {
        return when (folder.type) {
            BilibiliFavoriteFolderType.VIDEO_FOLDER -> favoriteVideoPage(folder.id, page)
            BilibiliFavoriteFolderType.VIDEO_COLLECTION -> favoriteCollectionVideoPage(folder.id, page)
            BilibiliFavoriteFolderType.UNKNOWN -> throw BilibiliApiException(-400, "无法识别收藏内容类型")
        }
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
        val info = data.optJSONObject("info")
        return BilibiliFavoriteVideoPage(
            videos = videos,
            // long: 失效稿件会被播放器过滤，是否继续翻页必须使用接口原始 has_more，不能按过滤后的可播放数量提前停止。
            hasMore = if (data.has("has_more")) {
                data.optBoolean("has_more", false)
            } else {
                (rawMedias?.length() ?: 0) >= FAVORITE_PAGE_SIZE
            },
            mediaCount = info
                ?.takeIf { metadata -> metadata.has("media_count") }
                ?.optInt("media_count", 0)
                ?.coerceAtLeast(0),
        )
    }

    private suspend fun favoriteCollectionVideoPage(collectionId: Long, page: Int): BilibiliFavoriteVideoPage {
        // long: 视频合集 id 不是普通收藏夹 media_id，必须走合集归档接口才能拿到每个可播放视频的 bvid。
        val root = request(
            path = "/x/space/fav/season/list",
            parameters = mapOf(
                "season_id" to collectionId,
                "pn" to page,
                "ps" to FAVORITE_PAGE_SIZE,
            ),
            useWbi = true,
        ).requireSuccess()
        val data = root.optJSONObject("data") ?: JSONObject()
        val rawMedias = data.optJSONArray("medias")
        val videos = rawMedias.toObjects().mapNotNull(::parseFavoriteCollectionVideo)
        val mediaCount = data.optJSONObject("info")
            ?.takeIf { metadata -> metadata.has("media_count") }
            ?.optInt("media_count", 0)
            ?.coerceAtLeast(0)
        val total = mediaCount ?: 0
        val rawCount = rawMedias?.length() ?: 0
        return BilibiliFavoriteVideoPage(
            videos = videos,
            hasMore = when {
                data.has("has_more") -> data.optBoolean("has_more", false)
                // long: 合集接口可能忽略分页并一次返回全部媒体；已覆盖总数时立即结束，避免批量下载重复入队。
                total > 0 && rawCount >= total -> false
                total > 0 -> rawCount == FAVORITE_PAGE_SIZE && page * FAVORITE_PAGE_SIZE < total
                else -> rawCount >= FAVORITE_PAGE_SIZE
            },
            mediaCount = mediaCount,
        )
    }

    suspend fun favoriteVideosAll(folder: BilibiliFavoriteFolder): List<BilibiliLibraryVideo> {
        val videos = mutableListOf<BilibiliLibraryVideo>()
        var page = 1
        while (page <= MAX_FAVORITE_PAGES) {
            val result = favoriteVideoPage(folder, page)
            videos += result.videos
            if (!result.hasMore) return videos
            page += 1
        }
        throw BilibiliApiException(-429, "收藏内容分页过多，请缩小下载范围")
    }

    suspend fun favoriteVideosAll(folderId: Long): List<BilibiliLibraryVideo> {
        return favoriteVideosAll(
            BilibiliFavoriteFolder(
                id = folderId,
                title = "",
                coverUrl = "",
                mediaCount = 0,
            ),
        )
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

    suspend fun onlineHistory(cursor: BilibiliOnlineHistoryCursor? = null): BilibiliOnlineHistoryPage {
        val root = request(
            path = "/x/web-interface/history/cursor",
            parameters = mapOf(
                "max" to (cursor?.max ?: 0),
                "view_at" to (cursor?.viewAtEpochSeconds ?: 0),
                "business" to cursor?.business,
                "type" to "archive",
                "ps" to HISTORY_PAGE_SIZE,
            ),
        ).requireSuccess()
        val data = root.optJSONObject("data") ?: JSONObject()
        val list = data.optJSONArray("list")
        val videos = list
            .toObjects()
            .mapNotNull(::parseOnlineHistoryVideo)
        val nextCursor = data.optJSONObject("cursor")?.let { value ->
            BilibiliOnlineHistoryCursor(
                max = value.optLong("max", 0L),
                viewAtEpochSeconds = value.optLong("view_at", 0L),
                business = value.optString("business"),
            )
        }?.takeIf { value ->
            list?.length()?.let { it > 0 } == true &&
                value.max > 0L &&
                value.viewAtEpochSeconds > 0L &&
                value.business.isNotBlank()
        }
        return BilibiliOnlineHistoryPage(
            videos = videos,
            nextCursor = nextCursor,
            hasMore = nextCursor != null,
        )
    }

    suspend fun searchOnlineHistory(keyword: String, page: Int = 1): BilibiliOnlineHistorySearchPage {
        val normalizedKeyword = keyword.trim()
        require(normalizedKeyword.isNotBlank()) { "在线历史搜索关键字不能为空" }
        val root = request(
            path = "/x/web-interface/history/search",
            parameters = mapOf(
                "keyword" to normalizedKeyword,
                "business" to "archive",
                "pn" to page.coerceAtLeast(1),
            ),
        ).requireSuccess()
        val data = root.optJSONObject("data") ?: JSONObject()
        val videos = data.optJSONArray("list")
            .toObjects()
            .mapNotNull(::parseOnlineHistoryVideo)
        val currentPage = data.optJSONObject("page")?.optInt("pn", page) ?: page
        val hasMore = data.optBoolean("has_more", false)
        return BilibiliOnlineHistorySearchPage(
            videos = videos,
            nextSearchPage = (currentPage + 1).takeIf { hasMore },
            hasMore = hasMore,
        )
    }

    suspend fun deleteOnlineHistory(historyKey: String) {
        require(historyKey.matches(HISTORY_KEY_PATTERN)) { "在线历史删除键无效" }
        postForm(
            path = "/x/v2/history/delete",
            parameters = mapOf("kid" to historyKey, "csrf" to requireCsrf()),
        ).requireSuccess()
    }

    suspend fun clearOnlineHistory() {
        postForm(
            path = "/x/v2/history/clear",
            parameters = mapOf("csrf" to requireCsrf()),
        ).requireSuccess()
    }

    suspend fun reportPlayHeartbeat(
        aid: Long,
        bvid: String,
        cid: Long,
        session: String,
        startedAtEpochSeconds: Long,
        playedSeconds: Int,
        maxPlayedSeconds: Int,
        durationSeconds: Int?,
        playType: Int,
    ) {
        val signedParameters = mapOf(
            "w_start_ts" to startedAtEpochSeconds,
            "w_aid" to aid,
            "w_dt" to 2,
            "w_realtime" to playedSeconds,
            "w_playedtime" to playedSeconds,
            "w_real_played_time" to playedSeconds,
            "w_video_duration" to durationSeconds,
            "w_last_play_progress_time" to maxPlayedSeconds,
            "web_location" to 1315873,
        )
        val keys = currentWbiKeys()
        val signed = WbiSigner.sign(signedParameters, keys.imgKey, keys.subKey, nowEpochSeconds())
        val url = buildUrl("/x/click-interface/web/heartbeat", emptyMap()).newBuilder()
            .encodedQuery(signed.encodedQuery)
            .build()
        postForm(
            url = url,
            parameters = buildMap {
                put("aid", aid.toString())
                put("bvid", bvid)
                put("cid", cid.toString())
                put("played_time", playedSeconds.toString())
                put("realtime", playedSeconds.toString())
                put("real_played_time", playedSeconds.toString())
                durationSeconds?.let { put("video_duration", it.toString()) }
                put("last_play_progress_time", maxPlayedSeconds.toString())
                put("max_play_progress_time", maxPlayedSeconds.toString())
                put("start_ts", startedAtEpochSeconds.toString())
                put("type", "3")
                put("sub_type", "0")
                put("dt", "2")
                put("outer", "0")
                put("play_type", playType.toString())
                put("session", session)
                put("csrf", requireCsrf())
            },
        ).requireSuccess()
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
            aid = data.optLongOrNull("aid"),
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
            source = BilibiliTrackSource(
                bvid = detail.bvid,
                cid = page.cid,
                qualityPreference = qualityPreference,
                aid = detail.aid ?: video.aid,
            ),
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

    suspend fun resolveVideoPlaybackStreams(
        bvid: String,
        cid: Long,
        qualityId: Int? = null,
        codecPreference: DashVideoCodecPreference = DashVideoCodecPreference.AVC,
        defaultMaxQualityId: Int? = null,
    ): DashVideoPlaybackStreams {
        val streams = resolveDashStreams(bvid, cid)
        val availableVideos = DashVideoSelector.selectableStreams(streams.video, codecPreference)
        val video = DashVideoSelector.selectForPlayback(
            streams = streams.video,
            qualityId = qualityId,
            codecPreference = codecPreference,
            defaultMaxQualityId = defaultMaxQualityId,
        )
            ?: throw BilibiliApiException(-404, if (qualityId == null) "没有可播放的视频轨" else "所选画质当前不可用")
        val audio = streams.standard.maxByOrNull(DashAudioStream::bandwidth)
            ?: throw BilibiliApiException(-404, "没有可播放的标准 AAC 音频")
        // long: UI 只接收清晰度与编码标签，带签名的 DASH URL 始终留在播放服务内，避免泄漏到展示层或日志。
        return DashVideoPlaybackStreams(
            video = video,
            audio = audio,
            availableVideos = availableVideos,
        )
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

    private suspend fun comprehensivePopularRecommendations(page: Int): BilibiliRecommendationPage {
        val data = request(
            path = "/x/web-interface/popular",
            parameters = mapOf(
                "pn" to page,
                "ps" to RECOMMENDATION_PAGE_SIZE,
            ),
        ).requireSuccess().optJSONObject("data") ?: JSONObject()
        val videos = data.optJSONArray("list")
            .toObjects()
            .mapNotNull(::parsePublicVideo)
        return BilibiliRecommendationPage(
            videos = videos,
            page = page,
            hasMore = !data.optBoolean("no_more", false) && videos.isNotEmpty(),
        )
    }

    private suspend fun weeklyPopularRecommendations(page: Int): BilibiliRecommendationPage {
        val issues = request("/x/web-interface/popular/series/list")
            .requireSuccess()
            .optJSONObject("data")
            ?.optJSONArray("list")
            .toObjects()
        val issue = issues.getOrNull(page - 1)
            ?: return BilibiliRecommendationPage(emptyList(), page, hasMore = false)
        val number = issue.optInt("number", 0)
        if (number <= 0) return BilibiliRecommendationPage(emptyList(), page, hasMore = false)
        val videos = request(
            path = "/x/web-interface/popular/series/one",
            parameters = mapOf("number" to number),
        ).requireSuccess()
            .optJSONObject("data")
            ?.optJSONArray("list")
            .toObjects()
            .mapNotNull(::parsePublicVideo)
        // long: 每周必看没有视频分页参数，首页的“下一页”表示继续读取更早一期，不能伪造 pn 请求。
        return BilibiliRecommendationPage(videos, page, hasMore = page < issues.size)
    }

    private suspend fun rankingRecommendations(page: Int): BilibiliRecommendationPage {
        val videos = request(
            path = "/x/web-interface/ranking/v2",
            parameters = mapOf("type" to "all"),
        ).requireSuccess()
            .optJSONObject("data")
            ?.optJSONArray("list")
            .toObjects()
            .mapNotNull(::parsePublicVideo)
        val fromIndex = (page - 1) * RECOMMENDATION_PAGE_SIZE
        if (fromIndex >= videos.size) return BilibiliRecommendationPage(emptyList(), page, hasMore = false)
        val toIndex = (fromIndex + RECOMMENDATION_PAGE_SIZE).coerceAtMost(videos.size)
        // long: 全站排行接口固定返回 Top 100，分批展示只发生在客户端，避免向 B 站发送并不存在的 pn/rid 参数。
        return BilibiliRecommendationPage(
            videos = videos.subList(fromIndex, toIndex),
            page = page,
            hasMore = toIndex < videos.size,
        )
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
        execute(request)
    }

    private suspend fun postForm(path: String, parameters: Map<String, String>): JSONObject {
        return postForm(buildUrl(path, emptyMap()), parameters)
    }

    private suspend fun postForm(url: HttpUrl, parameters: Map<String, String>): JSONObject {
        val body = FormBody.Builder().apply {
            parameters.forEach { (key, value) -> add(key, value) }
        }.build()
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        return withContext(Dispatchers.IO) { execute(request) }
    }

    private suspend fun postJson(
        path: String,
        queryParameters: Map<String, Any?>,
        payload: JSONObject,
    ): JSONObject {
        val request = Request.Builder()
            .url(buildUrl(path, queryParameters))
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return withContext(Dispatchers.IO) { execute(request) }
    }

    private fun execute(request: Request): JSONObject {
        return client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Bilibili HTTP ${response.code}")
            }
            runCatching { JSONObject(payload) }
                .getOrElse { throw IOException("Bilibili response is not JSON", it) }
        }
    }

    private fun requireCsrf(): String {
        return csrfProvider()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("登录凭据缺少 CSRF Token，请刷新登录状态后重试")
    }

    private suspend fun updateVideoFavorite(aid: Long, folderId: Long, adding: Boolean) {
        require(aid > 0L) { "视频 aid 无效" }
        require(folderId > 0L) { "收藏夹 id 无效" }
        postForm(
            path = "/x/v3/fav/resource/deal",
            parameters = buildMap {
                put("rid", aid.toString())
                put("type", "2")
                put(if (adding) "add_media_ids" else "del_media_ids", folderId.toString())
                put("platform", "web")
                put("ga", "1")
                put("gaia_source", "web_normal")
                put("csrf", requireCsrf())
            },
        ).requireSuccess()
    }

    private fun requireFavoriteFolderTitle(title: String): String {
        return title.trim().also { normalized ->
            require(normalized.isNotBlank()) { "收藏夹名称不能为空" }
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

    private fun parsePublicVideo(item: JSONObject): BilibiliVideo? {
        val bvid = item.optString("bvid").takeIf(String::isNotBlank) ?: return null
        return BilibiliVideo(
            bvid = bvid,
            aid = item.optLongOrNull("aid"),
            title = BilibiliText.plainTitle(item.optString("title")),
            author = item.optJSONObject("owner")?.optString("name").orEmpty()
                .ifBlank { item.optString("author") },
            coverUrl = BilibiliText.firstHttpsUrl(
                item.optString("pic"),
                item.optString("cover"),
            ),
            durationSeconds = item.optIntOrNull("duration"),
            playCount = item.optJSONObject("stat")?.optLongOrNull("view"),
            publishedAtEpochSeconds = item.optLongOrNull("pubdate") ?: item.optLongOrNull("ctime"),
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

    internal fun parseSearchCreator(item: JSONObject): BilibiliCreator? {
        val mid = item.optLong("mid", 0L).takeIf { it > 0L } ?: return null
        val name = BilibiliText.plainTitle(item.optString("uname")).takeIf(String::isNotBlank) ?: return null
        return BilibiliCreator(
            mid = mid,
            name = name,
            faceUrl = BilibiliText.httpsUrl(item.optString("upic")),
            signature = BilibiliText.plainTitle(item.optString("usign")),
            followerCount = item.optLongOrNull("fans"),
            videoCount = item.optIntOrNull("videos"),
            officialTitle = item.optJSONObject("official_verify")?.optString("desc").orEmpty(),
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
            signature = BilibiliText.plainTitle(item.optString("sign")),
            followerCount = item.optLongOrNull("fans"),
            officialTitle = item.optJSONObject("official_verify")?.optString("desc").orEmpty(),
        )
    }

    internal fun parseCreatorProfile(item: JSONObject): BilibiliCreator? {
        // long: 空间资料接口与搜索/关注接口字段名不同，在仓储层归一化后页面只依赖同一份 UP 主模型。
        val mid = item.optLong("mid", 0L).takeIf { it > 0L } ?: return null
        val name = BilibiliText.plainTitle(item.optString("name")).takeIf(String::isNotBlank) ?: return null
        val official = item.optJSONObject("official")
        return BilibiliCreator(
            mid = mid,
            name = name,
            faceUrl = BilibiliText.httpsUrl(item.optString("face")),
            signature = BilibiliText.plainTitle(item.optString("sign")),
            officialTitle = official?.optString("title").orEmpty().ifBlank { official?.optString("desc").orEmpty() },
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

    internal fun parseDynamicItem(item: JSONObject): BilibiliDynamicItem? {
        if (!item.optBoolean("visible", true)) return null
        val dynamicId = item.optString("id_str").takeIf(String::isNotBlank) ?: return null
        val modules = item.optJSONObject("modules") ?: return null
        val author = modules.optJSONObject("module_author") ?: return null
        val dynamic = modules.optJSONObject("module_dynamic") ?: return null
        val archive = dynamic.optJSONObject("major")?.optJSONObject("archive") ?: return null
        val bvid = archive.optString("bvid").takeIf(String::isNotBlank) ?: return null
        val authorName = BilibiliText.plainTitle(author.optString("name"))
        val publishedAt = author.optLongOrNull("pub_ts")
        val like = modules.optJSONObject("module_stat")?.optJSONObject("like")
        val video = BilibiliVideo(
            bvid = bvid,
            aid = archive.optLongOrNull("aid"),
            title = BilibiliText.plainTitle(archive.optString("title")),
            author = authorName,
            coverUrl = BilibiliText.httpsUrl(archive.optString("cover")),
            durationSeconds = parseDuration(archive.optString("duration_text")),
            playCount = null,
            publishedAtEpochSeconds = publishedAt,
        )
        return BilibiliDynamicItem(
            id = dynamicId,
            video = video,
            authorMid = author.optLong("mid", 0L),
            authorFaceUrl = BilibiliText.httpsUrl(author.optString("face")),
            description = BilibiliText.plainTitle(dynamic.optJSONObject("desc")?.optString("text").orEmpty()),
            publishedAtEpochSeconds = publishedAt,
            likeCount = like?.optLong("count", 0L)?.coerceAtLeast(0L) ?: 0L,
            isLiked = like?.optBoolean("status", false) == true,
            isLikeForbidden = like?.optBoolean("forbidden", false) == true,
        )
    }

    internal fun parseFavoriteFolder(
        item: JSONObject,
        group: BilibiliFavoriteFolderGroup = BilibiliFavoriteFolderGroup.CREATED,
    ): BilibiliFavoriteFolder? {
        if (item.optInt("state", 0) != 0) return null
        val id = item.optLong("id", 0L).takeIf { it > 0L } ?: return null
        val upper = item.optJSONObject("upper")
        return BilibiliFavoriteFolder(
            id = id,
            title = BilibiliText.plainTitle(item.optString("title")).ifBlank { "未命名收藏夹" },
            coverUrl = BilibiliText.httpsUrl(item.optString("cover")),
            mediaCount = item.optInt("media_count", 0),
            // long: created/list 的 type 表示内容属性而非收藏菜单类型，创建者自己的列表统一按普通收藏夹处理。
            type = if (group == BilibiliFavoriteFolderGroup.CREATED) {
                BilibiliFavoriteFolderType.VIDEO_FOLDER
            } else {
                BilibiliFavoriteFolderType.fromApiValue(item.optInt("type", 11))
            },
            group = group,
            ownerMid = upper?.optLong("mid", 0L)?.takeIf { it > 0L } ?: item.optLong("mid", 0L),
            ownerName = upper?.optString("name").orEmpty(),
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

    internal fun parseFavoriteCollectionVideo(item: JSONObject): BilibiliLibraryVideo? {
        val bvid = item.optString("bvid")
            .ifBlank { item.optString("bv_id") }
            .takeIf(String::isNotBlank) ?: return null
        val video = BilibiliVideo(
            bvid = bvid,
            aid = item.optLongOrNull("id"),
            title = BilibiliText.plainTitle(item.optString("title")),
            author = item.optJSONObject("upper")?.optString("name").orEmpty(),
            coverUrl = BilibiliText.httpsUrl(item.optString("cover")),
            durationSeconds = item.optIntOrNull("duration"),
            playCount = item.optJSONObject("cnt_info")?.optLongOrNull("play"),
            publishedAtEpochSeconds = item.optLongOrNull("pubtime"),
        )
        return BilibiliLibraryVideo(video)
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
            historyKey = history.optString("business")
                .takeIf(String::isNotBlank)
                ?.let { business -> "${business}_${history.optLong("oid")}" },
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
        const val CREATOR_SEARCH_PAGE_SIZE = 20
        const val CREATOR_VIDEO_PAGE_SIZE = 30
        const val RECOMMENDATION_PAGE_SIZE = 20
        const val FAVORITE_FOLDER_PAGE_SIZE = 50
        const val FAVORITE_PAGE_SIZE = 20
        const val HISTORY_PAGE_SIZE = 20
        const val MAX_FAVORITE_FOLDER_PAGES = 100
        const val MAX_FAVORITE_PAGES = 100
        const val DYNAMIC_FEATURES = "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,decorationCard,onlyfansAssetsV2,forwardListHidden,ugcDelete"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val WBI_CACHE_SECONDS = TimeUnit.HOURS.toSeconds(6)
        val HISTORY_KEY_PATTERN = Regex("[a-z-]+_[0-9]+")
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
