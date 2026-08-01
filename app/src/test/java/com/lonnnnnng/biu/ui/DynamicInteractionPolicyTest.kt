package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.BilibiliApiException
import com.lonnnnnng.biu.data.bilibili.BilibiliDynamicItem
import com.lonnnnnng.biu.data.bilibili.BilibiliTripleResult
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicInteractionPolicyTest {
    @Test
    fun `点赞先乐观更新且失败后恢复原始状态`() {
        val original = dynamicItem(isLiked = false, likeCount = 9)

        val mutation = DynamicInteractionPolicy.beginLikeMutation(original)

        assertTrue(mutation.optimistic.isLiked)
        assertEquals(10L, mutation.optimistic.likeCount)
        assertEquals(original, mutation.rollback())
    }

    @Test
    fun `取消点赞不会把计数减成负数`() {
        val mutation = DynamicInteractionPolicy.beginLikeMutation(dynamicItem(isLiked = true, likeCount = 0))

        assertFalse(mutation.optimistic.isLiked)
        assertEquals(0L, mutation.optimistic.likeCount)
    }

    @Test
    fun `三连成功同步点赞状态且只补一次计数`() {
        val result = BilibiliTripleResult(liked = true, coined = true, favorited = true, coinCount = 2)

        val updated = DynamicInteractionPolicy.applyTriple(dynamicItem(isLiked = false, likeCount = 9), result)
        val alreadyLiked = DynamicInteractionPolicy.applyTriple(dynamicItem(isLiked = true, likeCount = 9), result)

        assertTrue(updated.isLiked)
        assertEquals(10L, updated.likeCount)
        assertEquals(9L, alreadyLiked.likeCount)
    }

    @Test
    fun `账号限流和风控错误返回可执行提示`() {
        assertEquals(
            "请求过于频繁，请稍后重试",
            DynamicInteractionPolicy.errorMessage(BilibiliApiException(-799, "频繁"), "点赞失败"),
        )
        assertEquals(
            "Bilibili 风控拦截了操作，请稍后重试或重新登录",
            DynamicInteractionPolicy.errorMessage(BilibiliApiException(-352, "风控"), "点赞失败"),
        )
        assertEquals(
            "登录状态已失效，请刷新账号或重新登录",
            DynamicInteractionPolicy.errorMessage(BilibiliApiException(-101, "未登录"), "点赞失败"),
        )
        assertEquals(
            "登录凭据校验失败，请刷新账号后重试",
            DynamicInteractionPolicy.errorMessage(BilibiliApiException(-111, "csrf"), "点赞失败"),
        )
    }

    private fun dynamicItem(isLiked: Boolean, likeCount: Long): BilibiliDynamicItem {
        return BilibiliDynamicItem(
            id = "dynamic-1",
            video = BilibiliVideo(
                bvid = "BV1DYNAMIC",
                aid = 42,
                title = "动态视频",
                author = "音乐UP",
                coverUrl = "",
                durationSeconds = 60,
                playCount = null,
            ),
            authorMid = 1001,
            authorFaceUrl = "",
            description = "",
            publishedAtEpochSeconds = 1700000100,
            likeCount = likeCount,
            isLiked = isLiked,
            isLikeForbidden = false,
        )
    }
}
