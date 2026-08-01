package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.BilibiliApiException
import com.lonnnnnng.biu.data.bilibili.BilibiliDynamicItem
import com.lonnnnnng.biu.data.bilibili.BilibiliTripleResult

internal data class DynamicLikeMutation(
    val original: BilibiliDynamicItem,
    val optimistic: BilibiliDynamicItem,
) {
    fun rollback(): BilibiliDynamicItem = original
}

internal object DynamicInteractionPolicy {
    fun beginLikeMutation(item: BilibiliDynamicItem): DynamicLikeMutation {
        val targetLiked = !item.isLiked
        // long: 点赞先反馈到卡片，失败时仍保留完整原始快照；计数在零处收敛，避免取消异常状态后显示负数。
        val optimistic = item.copy(
            isLiked = targetLiked,
            likeCount = if (targetLiked) item.likeCount + 1L else (item.likeCount - 1L).coerceAtLeast(0L),
        )
        return DynamicLikeMutation(original = item, optimistic = optimistic)
    }

    fun applyTriple(item: BilibiliDynamicItem, result: BilibiliTripleResult): BilibiliDynamicItem {
        if (!result.liked) return item
        // long: 三连响应是最终状态；原卡片已经点赞时不能重复增加计数，否则重复操作会造成客户端数字漂移。
        return item.copy(
            isLiked = true,
            likeCount = item.likeCount + if (item.isLiked) 0L else 1L,
        )
    }

    fun errorMessage(error: Throwable, fallback: String): String {
        val code = (error as? BilibiliApiException)?.code
        return when (code) {
            -101 -> "登录状态已失效，请刷新账号或重新登录"
            -111 -> "登录凭据校验失败，请刷新账号后重试"
            -352, -412, -403 -> "Bilibili 风控拦截了操作，请稍后重试或重新登录"
            -799, -429 -> "请求过于频繁，请稍后重试"
            65006 -> "该视频已点赞，刷新动态后可同步最新状态"
            else -> error.message?.takeIf(String::isNotBlank)?.let { "$fallback：$it" } ?: fallback
        }
    }
}
