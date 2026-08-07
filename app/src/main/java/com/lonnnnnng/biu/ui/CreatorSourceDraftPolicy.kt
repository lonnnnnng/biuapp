package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.BilibiliCreator

object CreatorSourceDraftPolicy {
    fun toggle(current: List<BilibiliCreator>, creator: BilibiliCreator): List<BilibiliCreator> {
        return if (current.any { item -> item.mid == creator.mid }) {
            current.filterNot { item -> item.mid == creator.mid }
        } else {
            current + creator
        }
    }

    fun isDirty(draft: List<BilibiliCreator>, saved: List<BilibiliCreator>): Boolean =
        draft.map(BilibiliCreator::mid) != saved.map(BilibiliCreator::mid)

    fun mergeMetadata(
        draft: List<BilibiliCreator>,
        candidates: List<BilibiliCreator>,
    ): List<BilibiliCreator> {
        val latestByMid = candidates.associateBy(BilibiliCreator::mid)
        return draft.map { creator -> latestByMid[creator.mid] ?: creator }
    }
}
