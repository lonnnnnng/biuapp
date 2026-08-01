package com.lonnnnnng.biu.data.bilibili

import com.lonnnnnng.biu.core.model.AudioQualityPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BilibiliModelsTest {
    @Test
    fun `搜索标题去掉高亮标签并解码常见实体`() {
        assertEquals(
            "周杰伦 & 五月天",
            BilibiliText.plainTitle("<em class=\"keyword\">周杰伦</em> &amp; 五月天"),
        )
    }

    @Test
    fun `封面地址统一为 HTTPS`() {
        assertEquals("https://i0.hdslb.com/a.jpg", BilibiliText.httpsUrl("//i0.hdslb.com/a.jpg"))
        assertEquals("https://i0.hdslb.com/a.jpg", BilibiliText.httpsUrl("http://i0.hdslb.com/a.jpg"))
    }

    @Test
    fun `封面字段变体取第一个非空地址`() {
        assertEquals(
            "https://i0.hdslb.com/fallback.jpg",
            BilibiliText.firstHttpsUrl("", null, "//i0.hdslb.com/fallback.jpg"),
        )
    }

    @Test
    fun `WBI 图片地址提取实时 key`() {
        val keys = WbiKeyParser.fromImageUrls(
            "https://i0.hdslb.com/bfs/wbi/abc123.png",
            "https://i0.hdslb.com/bfs/wbi/def456.png",
        )

        assertEquals(WbiKeys("abc123", "def456"), keys)
    }

    @Test
    fun `DASH 音频优先无损其次杜比最后普通最高码率`() {
        val standard = listOf(stream("normal-low", 64_000), stream("normal-high", 192_000))
        val dolby = listOf(stream("dolby", 256_000))
        val flac = stream("flac", 900_000)

        assertEquals("flac", DashAudioSelector.select(AudioQualityPreference.HIGHEST, flac, dolby, standard)?.url)
        assertEquals("dolby", DashAudioSelector.select(AudioQualityPreference.HIGHEST, null, dolby, standard)?.url)
        assertEquals("normal-high", DashAudioSelector.select(AudioQualityPreference.HIGHEST, null, emptyList(), standard)?.url)
    }

    @Test
    fun `省流量音质选择普通流最低码率`() {
        val standard = listOf(stream("normal-low", 64_000), stream("normal-high", 192_000))

        assertEquals(
            "normal-low",
            DashAudioSelector.select(AudioQualityPreference.DATA_SAVER, stream("flac", 900_000), emptyList(), standard)?.url,
        )
    }

    @Test
    fun `播放地址提取过期时间`() {
        assertEquals(1_702_204_169L, StreamUrlExpiry.epochSeconds("https://example.com/audio?deadline=1702204169&foo=1"))
        assertNull(StreamUrlExpiry.epochSeconds("https://example.com/audio"))
    }

    @Test
    fun `主播放地址失败后切换备用 CDN`() {
        val stream = DashAudioStream(
            url = "https://primary.example/audio",
            bandwidth = 192_000,
            codecs = "mp4a.40.2",
            qualityLabel = "192 kbps",
            expiresAtEpochSeconds = null,
            backupUrls = listOf("https://backup.example/audio"),
        )

        assertEquals("https://backup.example/audio", stream.replacementUrl(stream.url))
        assertEquals(stream.url, stream.replacementUrl("https://old.example/audio"))
    }

    @Test
    fun `视频下载优先最高画质 AVC 并在缺失时回退其他编码`() {
        val candidates = listOf(
            videoStream("avc-720", qualityId = 64, codecs = "avc1.64001f", bandwidth = 1_200_000),
            videoStream("avc-1080", qualityId = 80, codecs = "avc1.640028", bandwidth = 2_400_000),
            videoStream("hevc-4k", qualityId = 120, codecs = "hev1.1.6.L153.B0", bandwidth = 8_000_000),
        )

        assertEquals("avc-1080", DashVideoSelector.select(candidates)?.url)
        assertEquals(
            "hevc-4k",
            DashVideoSelector.select(candidates.filterNot { it.codecs.startsWith("avc1") })?.url,
        )
    }

    @Test
    fun `视频画质候选按清晰度去重并在指定清晰度优先 AVC`() {
        val candidates = listOf(
            videoStream("hevc-4k", qualityId = 120, codecs = "hev1.1.6.L153.B0", bandwidth = 8_000_000),
            videoStream("hevc-1080", qualityId = 80, codecs = "hev1.1.6.L120.B0", bandwidth = 1_900_000),
            videoStream("avc-1080", qualityId = 80, codecs = "avc1.640028", bandwidth = 2_400_000),
            videoStream("avc-720", qualityId = 64, codecs = "avc1.64001f", bandwidth = 1_200_000),
        )

        assertEquals(
            listOf("hevc-4k", "avc-1080", "avc-720"),
            DashVideoSelector.selectableStreams(candidates).map(DashVideoStream::url),
        )
        assertEquals("avc-1080", DashVideoSelector.select(candidates, qualityId = 80)?.url)
        assertNull(DashVideoSelector.select(candidates, qualityId = 32))
    }

    @Test
    fun `MTK 播放偏好在同清晰度优先 HEVC 并保留画质顺序`() {
        val candidates = listOf(
            videoStream("hevc-1080", qualityId = 80, codecs = "hev1.1.6.L120.B0", bandwidth = 1_900_000),
            videoStream("avc-1080", qualityId = 80, codecs = "avc1.640028", bandwidth = 2_400_000),
            videoStream("hevc-720", qualityId = 64, codecs = "hev1.1.6.L120.B0", bandwidth = 1_100_000),
            videoStream("avc-720", qualityId = 64, codecs = "avc1.64001f", bandwidth = 1_200_000),
        )

        assertEquals(
            "hevc-1080",
            DashVideoSelector.select(
                candidates,
                qualityId = 80,
                codecPreference = DashVideoCodecPreference.HEVC,
            )?.url,
        )
        assertEquals(
            listOf("hevc-1080", "hevc-720"),
            DashVideoSelector.selectableStreams(
                candidates,
                codecPreference = DashVideoCodecPreference.HEVC,
            ).map(DashVideoStream::url),
        )
    }

    @Test
    fun `MTK 默认播放限制到720P但用户手选画质不受影响`() {
        val candidates = listOf(
            videoStream("avc-1080", qualityId = 80, codecs = "avc1.640033", bandwidth = 2_700_000),
            videoStream("avc-720", qualityId = 64, codecs = "avc1.64001f", bandwidth = 1_200_000),
        )

        assertEquals(
            "avc-720",
            DashVideoSelector.selectForPlayback(candidates, defaultMaxQualityId = 64)?.url,
        )
        assertEquals(
            "avc-1080",
            DashVideoSelector.selectForPlayback(candidates, qualityId = 80, defaultMaxQualityId = 64)?.url,
        )
    }

    @Test
    fun `默认上限没有匹配轨时仍回退到可播放最高画质`() {
        val candidates = listOf(
            videoStream("avc-1080", qualityId = 80, codecs = "avc1.640033", bandwidth = 2_700_000),
        )

        assertEquals(
            "avc-1080",
            DashVideoSelector.selectForPlayback(candidates, defaultMaxQualityId = 64)?.url,
        )
    }

    @Test
    fun `视频主播放地址失败后切换备用 CDN`() {
        val stream = videoStream(
            url = "https://primary.example/video",
            qualityId = 80,
            codecs = "avc1.640028",
            bandwidth = 2_400_000,
            backupUrls = listOf("https://backup.example/video"),
        )

        assertEquals("https://backup.example/video", stream.replacementUrl(stream.url))
        assertEquals(stream.url, stream.replacementUrl("https://old.example/video"))
    }

    private fun stream(url: String, bandwidth: Long): DashAudioStream {
        return DashAudioStream(url, bandwidth, "mp4a.40.2", "test", null)
    }

    private fun videoStream(
        url: String,
        qualityId: Int,
        codecs: String,
        bandwidth: Long,
        backupUrls: List<String> = emptyList(),
    ): DashVideoStream {
        return DashVideoStream(
            url = url,
            qualityId = qualityId,
            bandwidth = bandwidth,
            codecs = codecs,
            width = 1920,
            height = 1080,
            frameRate = 30.0,
            qualityLabel = "test",
            expiresAtEpochSeconds = null,
            backupUrls = backupUrls,
        )
    }
}
