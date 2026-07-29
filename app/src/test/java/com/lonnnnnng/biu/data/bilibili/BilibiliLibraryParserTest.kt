package com.lonnnnnng.biu.data.bilibili

import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BilibiliLibraryParserTest {
    private val repository = BilibiliRepository(OkHttpClient())

    @Test
    fun `favorite video maps playable archive fields`() {
        val item = JSONObject(
            """
            {
              "id": 123,
              "type": 2,
              "attr": 0,
              "title": "收藏歌曲",
              "cover": "//i0.hdslb.com/cover.jpg",
              "duration": 245,
              "fav_time": 1700000000,
              "bvid": "BV1TEST",
              "upper": {"name": "UP主"},
              "cnt_info": {"play": 4567}
            }
            """.trimIndent(),
        )

        val result = repository.parseFavoriteVideo(item)!!

        assertEquals("BV1TEST", result.video.bvid)
        assertEquals("https://i0.hdslb.com/cover.jpg", result.video.coverUrl)
        assertEquals(245, result.video.durationSeconds)
        assertEquals(4567L, result.video.playCount)
        assertEquals(1700000000L, result.savedAtEpochSeconds)
    }

    @Test
    fun `favorite parser rejects non video and unavailable entries`() {
        assertNull(repository.parseFavoriteVideo(JSONObject("{\"type\":12,\"attr\":0,\"bvid\":\"BV1\"}")))
        assertNull(repository.parseFavoriteVideo(JSONObject("{\"type\":2,\"attr\":1,\"bvid\":\"BV1\"}")))
    }

    @Test
    fun `online history ignores records without bvid`() {
        val item = JSONObject("{\"title\":\"直播记录\",\"history\":{\"business\":\"live\"}}")

        assertNull(repository.parseOnlineHistoryVideo(item))
    }

    @Test
    fun `watch later keeps progress and add time`() {
        val item = JSONObject(
            """
            {
              "aid": 42,
              "bvid": "BV1LATER",
              "title": "稍后播放",
              "pic": "https://example.com/a.jpg",
              "duration": 300,
              "progress": 61,
              "add_at": 1700001000,
              "owner": {"name": "作者"},
              "stat": {"view": 99}
            }
            """.trimIndent(),
        )

        val result = repository.parseWatchLaterVideo(item)!!

        assertEquals(61, result.progressSeconds)
        assertEquals(1700001000L, result.savedAtEpochSeconds)
        assertEquals("作者", result.video.author)
    }
}
