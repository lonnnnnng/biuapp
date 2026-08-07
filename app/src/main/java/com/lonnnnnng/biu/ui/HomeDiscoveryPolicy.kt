package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.CreatorFeedTabState
import com.lonnnnnng.biu.data.local.CreatorGroupEntity
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity

enum class HomeDiscoveryMode(val label: String) {
    LATEST("最新发布"),
    UNPLAYED("未播放"),
    RECENT("最近播放"),
}

sealed interface HomeDiscoveryScope {
    data object All : HomeDiscoveryScope

    data class Group(val groupId: Long) : HomeDiscoveryScope

    data class Creator(val mid: Long) : HomeDiscoveryScope
}

data class HomeDiscoveryScopeOption(
    val scope: HomeDiscoveryScope,
    val label: String,
)

data class HomeDiscoverySnapshot(
    val videos: List<BilibiliVideo>,
    val hasMore: Boolean,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val paginationKey: Int,
)

internal object HomeDiscoveryPolicy {
    fun scopeOptions(
        creators: List<BilibiliCreator>,
        groups: List<CreatorGroupEntity>,
        memberships: Map<Long, Set<Long>>,
    ): List<HomeDiscoveryScopeOption> {
        val selectedMids = creators.mapTo(linkedSetOf(), BilibiliCreator::mid)
        return buildList {
            add(HomeDiscoveryScopeOption(HomeDiscoveryScope.All, "全部"))
            groups.forEach { group ->
                if (memberships[group.groupId].orEmpty().any(selectedMids::contains)) {
                    add(HomeDiscoveryScopeOption(HomeDiscoveryScope.Group(group.groupId), group.name))
                }
            }
            creators.distinctBy(BilibiliCreator::mid).forEach { creator ->
                add(
                    HomeDiscoveryScopeOption(
                        scope = HomeDiscoveryScope.Creator(creator.mid),
                        label = creator.name.ifBlank { "UID ${creator.mid}" },
                    ),
                )
            }
        }
    }

    fun normalizeScope(
        scope: HomeDiscoveryScope,
        creators: List<BilibiliCreator>,
        groups: List<CreatorGroupEntity>,
        memberships: Map<Long, Set<Long>>,
    ): HomeDiscoveryScope {
        val available = scopeOptions(creators, groups, memberships).mapTo(hashSetOf(), HomeDiscoveryScopeOption::scope)
        return scope.takeIf(available::contains) ?: HomeDiscoveryScope.All
    }

    fun includedMids(
        scope: HomeDiscoveryScope,
        creators: List<BilibiliCreator>,
        memberships: Map<Long, Set<Long>>,
    ): Set<Long> {
        val selectedMids = creators.mapTo(linkedSetOf(), BilibiliCreator::mid)
        return when (scope) {
            HomeDiscoveryScope.All -> selectedMids
            is HomeDiscoveryScope.Group -> memberships[scope.groupId].orEmpty().filterTo(linkedSetOf(), selectedMids::contains)
            is HomeDiscoveryScope.Creator -> setOf(scope.mid).filterTo(linkedSetOf(), selectedMids::contains)
        }
    }

    fun snapshot(
        tabs: List<CreatorFeedTabState>,
        scope: HomeDiscoveryScope,
        mode: HomeDiscoveryMode,
        creators: List<BilibiliCreator>,
        memberships: Map<Long, Set<Long>>,
        history: List<PlaybackHistoryEntity>,
    ): HomeDiscoverySnapshot {
        val mids = includedMids(scope, creators, memberships)
        val includedTabs = tabs.filter { tab -> tab.creator.mid in mids }
        val playedAtByBvid = history.groupingBy(PlaybackHistoryEntity::bvid)
            .fold(0L) { latest, item -> maxOf(latest, item.playedAtEpochMs) }
        val videos = includedTabs
            .flatMap(CreatorFeedTabState::videos)
            .distinctBy(BilibiliVideo::bvid)
            .let { loaded ->
                when (mode) {
                    HomeDiscoveryMode.LATEST -> loaded.sortedWith(latestVideoComparator)
                    HomeDiscoveryMode.UNPLAYED -> loaded
                        .filterNot { video -> video.bvid in playedAtByBvid }
                        .sortedWith(latestVideoComparator)
                    HomeDiscoveryMode.RECENT -> loaded
                        .filter { video -> video.bvid in playedAtByBvid }
                        .sortedWith(
                            compareByDescending<BilibiliVideo> { video -> playedAtByBvid[video.bvid] ?: 0L }
                                .then(latestVideoComparator),
                        )
                }
            }
        return HomeDiscoverySnapshot(
            videos = videos,
            hasMore = includedTabs.any(CreatorFeedTabState::hasMore),
            isLoading = includedTabs.any(CreatorFeedTabState::isLoading),
            isLoadingMore = includedTabs.any(CreatorFeedTabState::isLoadingMore),
            // long: 过滤模式可能拉到整页不可见内容；来源游标变化仍必须重新允许触底续页。
            paginationKey = includedTabs.fold(1) { key, tab ->
                31 * key + tab.videos.size + 31 * (tab.nextPage ?: -1)
            },
        )
    }

    fun midsNeedingInitialLoad(
        tabs: List<CreatorFeedTabState>,
        scope: HomeDiscoveryScope,
        creators: List<BilibiliCreator>,
        memberships: Map<Long, Set<Long>>,
    ): List<Long> {
        val mids = includedMids(scope, creators, memberships)
        return tabs.filter { tab -> tab.creator.mid in mids && !tab.hasLoaded }
            .map { tab -> tab.creator.mid }
    }

    fun nextAppendMid(
        tabs: List<CreatorFeedTabState>,
        scope: HomeDiscoveryScope,
        creators: List<BilibiliCreator>,
        memberships: Map<Long, Set<Long>>,
    ): Long? {
        val mids = includedMids(scope, creators, memberships)
        return tabs
            .asSequence()
            .filter { tab -> tab.creator.mid in mids && tab.hasMore && !tab.isLoading && !tab.isLoadingMore }
            // long: 聚合续页每次只拉一个 UP；优先推进当前最接近列表尾部的发布时间边界，避免一次触底并发几十个请求。
            .maxByOrNull { tab -> tab.videos.lastOrNull()?.publishedAtEpochSeconds ?: Long.MAX_VALUE }
            ?.creator
            ?.mid
    }

    private val latestVideoComparator = compareByDescending<BilibiliVideo> {
        it.publishedAtEpochSeconds ?: Long.MIN_VALUE
    }.thenBy(BilibiliVideo::bvid)
}
