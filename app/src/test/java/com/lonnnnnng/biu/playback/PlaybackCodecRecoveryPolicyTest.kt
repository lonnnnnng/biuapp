package com.lonnnnnng.biu.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCodecRecoveryPolicyTest {
    @Test
    fun `MTK codec 服务死亡导致的未分类运行时错误需要重建播放器`() {
        val policy = PlaybackCodecRecoveryPolicy()

        assertTrue(
            policy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED,
                causeTypeNames = listOf("android.media.MediaCodec\$CodecException"),
                nowElapsedMs = 0L,
            ),
        )
    }

    @Test
    fun `明确的解码失败即使没有 CodecException 类型也需要重建播放器`() {
        val policy = PlaybackCodecRecoveryPolicy()

        assertTrue(
            policy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
                causeTypeNames = emptyList(),
                nowElapsedMs = 0L,
            ),
        )
    }

    @Test
    fun `网络错误不能误触发播放器重建`() {
        val policy = PlaybackCodecRecoveryPolicy()

        assertFalse(
            policy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                causeTypeNames = listOf("java.net.SocketTimeoutException"),
                nowElapsedMs = 0L,
            ),
        )
    }

    @Test
    fun `MTK 视频末尾 flush 的未分类错误需要重建而音频不需要`() {
        val videoPolicy = PlaybackCodecRecoveryPolicy()
        val audioPolicy = PlaybackCodecRecoveryPolicy()

        assertTrue(
            videoPolicy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED,
                causeTypeNames = listOf("java.lang.IllegalStateException"),
                nowElapsedMs = 0L,
                isMtkVideoPlayback = true,
            ),
        )
        assertFalse(
            audioPolicy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED,
                causeTypeNames = listOf("java.lang.IllegalStateException"),
                nowElapsedMs = 0L,
                isMtkVideoPlayback = false,
            ),
        )
    }

    @Test
    fun `同一资源在短时间内最多自动重建两次`() {
        val policy = PlaybackCodecRecoveryPolicy()

        assertTrue(
            policy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
                causeTypeNames = emptyList(),
                nowElapsedMs = 0L,
            ),
        )
        policy.finishRecovery()
        assertTrue(
            policy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
                causeTypeNames = emptyList(),
                nowElapsedMs = 1_000L,
            ),
        )
        policy.finishRecovery()
        assertFalse(
            policy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
                causeTypeNames = emptyList(),
                nowElapsedMs = 2_000L,
            ),
        )
    }

    @Test
    fun `稳定播放超过窗口后允许再次自动恢复`() {
        val policy = PlaybackCodecRecoveryPolicy()

        repeat(2) { attempt ->
            assertTrue(
                policy.tryBeginRecovery(
                    mediaId = "BV1:101",
                    errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
                    causeTypeNames = emptyList(),
                    nowElapsedMs = attempt * 1_000L,
                ),
            )
            policy.finishRecovery()
        }
        assertTrue(
            policy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
                causeTypeNames = emptyList(),
                nowElapsedMs = 31_000L,
            ),
        )
    }

    @Test
    fun `用户重新选择资源后允许新一轮 codec 恢复`() {
        val policy = PlaybackCodecRecoveryPolicy()

        assertTrue(
            policy.tryBeginRecovery(
                mediaId = "BV1:101",
                errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
                causeTypeNames = emptyList(),
                nowElapsedMs = 0L,
            ),
        )
        policy.finishRecovery()
        policy.resetForExternalMediaItemTransition()
        assertTrue(
            policy.tryBeginRecovery(
                mediaId = "BV1:202",
                errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
                causeTypeNames = emptyList(),
                nowElapsedMs = 1_000L,
            ),
        )
    }
}
