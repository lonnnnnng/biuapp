package com.lonnnnnng.biu.data.bilibili

data class CreatorFeedTabState(
    val creator: BilibiliCreator,
    val videos: List<BilibiliVideo> = emptyList(),
    val nextPage: Int? = 1,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
) {
    val hasMore: Boolean
        get() = nextPage != null
}

object CreatorFeedPolicy {
    // long: 保存范围后每位 UP 都是独立 Tab；配置增删只移除失效缓存，未变化的 UP 保留已加载内容和游标。
    fun reconcileTabs(
        existing: List<CreatorFeedTabState>,
        selectedCreators: List<BilibiliCreator>,
    ): List<CreatorFeedTabState> {
        val existingByMid = existing.associateBy { tab -> tab.creator.mid }
        return selectedCreators.distinctBy(BilibiliCreator::mid).map { creator ->
            existingByMid[creator.mid]
                ?.copy(creator = creator, isLoading = false, isLoadingMore = false)
                ?: CreatorFeedTabState(creator = creator)
        }
    }

    // long: 每个 UP 的续页只推进自己的游标，并在追加时按 BV 号去重，避免一个 Tab 的接口结果污染其他 Tab。
    fun applyPage(
        current: CreatorFeedTabState,
        page: BilibiliCreatorVideoPage,
        append: Boolean,
    ): CreatorFeedTabState {
        val videos = if (append) current.videos + page.videos else page.videos
        return current.copy(
            videos = videos.distinctBy(BilibiliVideo::bvid),
            nextPage = if (page.hasMore) page.page + 1 else null,
            hasLoaded = true,
            isLoading = false,
            isLoadingMore = false,
        )
    }
}
