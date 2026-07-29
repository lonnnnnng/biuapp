package com.lonnnnnng.biu.data.bilibili

enum class HomeFeedMode {
    FALLBACK,
    MY_FOLLOWS,
}

object CreatorFeedPolicy {
    // long: 未选择关注 UP 时继续保留原音乐内容，只有保存了明确范围后才切换首页来源。
    fun modeFor(selectedCreators: List<BilibiliCreator>): HomeFeedMode {
        return if (selectedCreators.isEmpty()) HomeFeedMode.FALLBACK else HomeFeedMode.MY_FOLLOWS
    }

    // long: 每位 UP 的接口结果各自有序，但首页需要全局时间线；先统一排序再去重可保留同 BV 号的最新记录。
    fun merge(creatorFeeds: List<List<BilibiliVideo>>): List<BilibiliVideo> {
        return creatorFeeds
            .flatten()
            .sortedByDescending { video -> video.publishedAtEpochSeconds ?: Long.MIN_VALUE }
            .distinctBy(BilibiliVideo::bvid)
    }
}
