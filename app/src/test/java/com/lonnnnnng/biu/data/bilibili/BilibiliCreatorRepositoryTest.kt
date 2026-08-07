package com.lonnnnnng.biu.data.bilibili

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BilibiliCreatorRepositoryTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().also(MockWebServer::start)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `读取登录账号关注列表并携带分页参数`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "total": 2,
                    "list": [
                      {"mid": 1001, "uname": "UP一", "face": "//i0.hdslb.com/1.jpg"},
                      {"mid": 1002, "uname": "UP二", "face": "//i0.hdslb.com/2.jpg"}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val creators = repository.followingCreators(mid = 99L)

        assertEquals(listOf(1001L, 1002L), creators.map(BilibiliCreator::mid))
        val request = server.takeRequest()
        assertEquals("/x/relation/followings", request.requestUrl?.encodedPath)
        assertEquals("99", request.requestUrl?.queryParameter("vmid"))
        assertEquals("1", request.requestUrl?.queryParameter("pn"))
        assertEquals("50", request.requestUrl?.queryParameter("ps"))
    }

    @Test
    fun `关注列表超过二十页时继续读取到接口总数`() = runBlocking {
        repeat(21) { index ->
            server.enqueue(
                jsonResponse(
                    """
                    {
                      "code": 0,
                      "data": {
                        "total": 21,
                        "list": [
                          {"mid": ${index + 1}, "uname": "UP${index + 1}", "face": ""}
                        ]
                      }
                    }
                    """.trimIndent(),
                ),
            )
        }
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val creators = repository.followingCreators(mid = 99L)

        assertEquals(21, creators.size)
        repeat(20) { server.takeRequest() }
        assertEquals("21", server.takeRequest().requestUrl?.queryParameter("pn"))
    }

    @Test
    fun `按发布时间读取UP投稿并使用WBI签名`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "wbi_img": {
                      "img_url": "https://i0.hdslb.com/bfs/wbi/abcdefghijklmnopqrstuvwxyz123456.png",
                      "sub_url": "https://i0.hdslb.com/bfs/wbi/123456abcdefghijklmnopqrstuvwxyz.png"
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "list": {
                      "vlist": [
                        {
                          "aid": 42,
                          "bvid": "BV1CREATOR",
                          "title": "最新投稿",
                          "pic": "//i0.hdslb.com/video.jpg",
                          "length": "03:15",
                          "play": 1234,
                          "created": 1700003000
                        }
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            nowEpochSeconds = { 1_700_000_000L },
            apiBase = server.url("/"),
        )
        val creator = BilibiliCreator(1001L, "关注的UP", "")

        val videos = repository.creatorVideos(creator)

        assertEquals(listOf("BV1CREATOR"), videos.map(BilibiliVideo::bvid))
        server.takeRequest() // WBI key
        val request = server.takeRequest()
        assertEquals("/x/space/wbi/arc/search", request.requestUrl?.encodedPath)
        assertEquals("1001", request.requestUrl?.queryParameter("mid"))
        assertEquals("pubdate", request.requestUrl?.queryParameter("order"))
        assertEquals("30", request.requestUrl?.queryParameter("ps"))
        assertEquals("1700000000", request.requestUrl?.queryParameter("wts"))
        check(!request.requestUrl?.queryParameter("w_rid").isNullOrBlank())
    }

    @Test
    fun `按名称分页搜索UP主并解析资料`() = runBlocking {
        server.enqueue(wbiKeyResponse())
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "numPages": 3,
                    "numResults": 42,
                    "result": [
                      {
                        "mid": 1001,
                        "uname": "<em class=\"keyword\">音乐</em>UP",
                        "upic": "//i0.hdslb.com/up.jpg",
                        "usign": "每天更新",
                        "fans": 12345,
                        "videos": 88,
                        "official_verify": {"desc": "音乐UP主"}
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            nowEpochSeconds = { 1_700_000_000L },
            apiBase = server.url("/"),
        )

        val result = repository.searchCreators(" 音乐 ", page = 2)

        assertEquals(2, result.page)
        assertEquals(42, result.total)
        assertTrue(result.hasMore)
        assertEquals("音乐UP", result.creators.single().name)
        assertEquals(12_345L, result.creators.single().followerCount)
        assertEquals(88, result.creators.single().videoCount)
        server.takeRequest()
        val request = server.takeRequest()
        assertEquals("/x/web-interface/wbi/search/type", request.requestUrl?.encodedPath)
        assertEquals("bili_user", request.requestUrl?.queryParameter("search_type"))
        assertEquals("音乐", request.requestUrl?.queryParameter("keyword"))
        assertEquals("2", request.requestUrl?.queryParameter("page"))
    }

    @Test
    fun `读取UP主空间资料和投稿总数`() = runBlocking {
        server.enqueue(wbiKeyResponse())
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "mid": 1001,
                    "name": "空间UP",
                    "face": "//i0.hdslb.com/space.jpg",
                    "sign": "空间签名",
                    "official": {"title": "音乐创作者"}
                  }
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "page": {"count": 31},
                    "list": {
                      "vlist": [
                        {"aid": 42, "bvid": "BV1SPACE", "title": "投稿", "pic": "", "length": "01:00", "created": 1700003000}
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            nowEpochSeconds = { 1_700_000_000L },
            apiBase = server.url("/"),
        )

        val profile = repository.creatorProfile(1001L)
        val videos = repository.creatorVideoPage(profile, page = 1)

        assertEquals("空间UP", profile.name)
        assertEquals("空间签名", profile.signature)
        assertEquals("音乐创作者", profile.officialTitle)
        assertEquals(31, videos.total)
        assertTrue(videos.hasMore)
        assertEquals("空间UP", videos.videos.single().author)
        server.takeRequest()
        assertEquals("/x/space/wbi/acc/info", server.takeRequest().requestUrl?.encodedPath)
        assertEquals("/x/space/wbi/arc/search", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun `读取UP主合集和系列并合并分页`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "items_lists": {
                      "page": {"page_num": 1, "page_size": 20, "total": 21},
                      "seasons_list": [
                        {"meta": {"season_id": 11, "name": "现场合集", "cover": "//i0.hdslb.com/s.jpg", "mid": 1001, "total": 8, "ptime": 1700001000}}
                      ],
                      "series_list": [
                        {"meta": {"series_id": 22, "name": "翻唱系列", "cover": "//i0.hdslb.com/r.jpg", "mid": 1001, "total": 12, "ctime": 1700002000}}
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))
        val creator = BilibiliCreator(1001L, "音乐UP", "")

        val page = repository.creatorCollectionPage(creator)

        assertEquals(listOf("翻唱系列", "现场合集"), page.collections.map { it.title })
        assertEquals(BilibiliCreatorCollectionType.SERIES, page.collections.first().type)
        assertTrue(page.hasMore)
        val request = server.takeRequest()
        assertEquals("/x/polymer/web-space/seasons_series_list", request.requestUrl?.encodedPath)
        assertEquals("1001", request.requestUrl?.queryParameter("mid"))
        assertEquals("20", request.requestUrl?.queryParameter("page_size"))
    }

    @Test
    fun `读取系列视频并保留稳定BV号和发布时间`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": 0,
                  "data": {
                    "page": {"num": 1, "size": 30, "total": 31},
                    "archives": [
                      {"aid": 42, "bvid": "BV1SERIES", "title": "系列歌曲", "pic": "//i0.hdslb.com/v.jpg", "duration": 245, "pubdate": 1700003000, "stat": {"view": 99}}
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))
        val collection = BilibiliCreatorCollection(
            id = 22L,
            type = BilibiliCreatorCollectionType.SERIES,
            title = "翻唱系列",
            coverUrl = "",
            mediaCount = 31,
            ownerMid = 1001L,
            ownerName = "音乐UP",
        )

        val page = repository.creatorCollectionVideoPage(collection)

        assertEquals("BV1SERIES", page.videos.single().bvid)
        assertEquals("音乐UP", page.videos.single().author)
        assertEquals(1_700_003_000L, page.videos.single().publishedAtEpochSeconds)
        assertTrue(page.hasMore)
        val request = server.takeRequest()
        assertEquals("/x/series/archives", request.requestUrl?.encodedPath)
        assertEquals("22", request.requestUrl?.queryParameter("series_id"))
        assertEquals("desc", request.requestUrl?.queryParameter("sort"))
    }

    @Test
    fun `读取关注关系并映射互相关注`() = runBlocking {
        server.enqueue(jsonResponse("""{"code":0,"data":{"mid":1001,"attribute":6}}"""))
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val relation = repository.creatorRelation(1001L)

        assertEquals(BilibiliCreatorRelation.MUTUAL, relation)
        assertTrue(relation.isFollowing)
        val request = server.takeRequest()
        assertEquals("/x/relation", request.requestUrl?.encodedPath)
        assertEquals("1001", request.requestUrl?.queryParameter("fid"))
    }

    @Test
    fun `提交取消关注时携带CSRF和来源`() = runBlocking {
        server.enqueue(jsonResponse("""{"code":0,"message":"0"}"""))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        repository.modifyCreatorRelation(mid = 1001L, following = false)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/x/relation/modify", request.requestUrl?.encodedPath)
        assertEquals("fid=1001&act=2&re_src=11&csrf=csrf-token", request.body.readUtf8())
    }

    @Test
    fun `未知关注属性保持未知状态`() = runBlocking {
        server.enqueue(jsonResponse("""{"code":0,"data":{"attribute":99}}"""))
        val repository = BilibiliRepository(OkHttpClient(), apiBase = server.url("/"))

        val relation = repository.creatorRelation(1001L)

        assertEquals(BilibiliCreatorRelation.UNKNOWN, relation)
        assertFalse(relation.isFollowing)
    }

    @Test(expected = BilibiliApiException::class)
    fun `关注接口业务失败时抛出统一异常`() = runBlocking {
        server.enqueue(jsonResponse("""{"code":-101,"message":"账号未登录"}"""))
        val repository = BilibiliRepository(
            client = OkHttpClient(),
            apiBase = server.url("/"),
            csrfProvider = { "csrf-token" },
        )

        repository.modifyCreatorRelation(mid = 1001L, following = true)
    }

    private fun wbiKeyResponse(): MockResponse = jsonResponse(
        """
        {
          "code": 0,
          "data": {
            "wbi_img": {
              "img_url": "https://i0.hdslb.com/bfs/wbi/abcdefghijklmnopqrstuvwxyz123456.png",
              "sub_url": "https://i0.hdslb.com/bfs/wbi/123456abcdefghijklmnopqrstuvwxyz.png"
            }
          }
        }
        """.trimIndent(),
    )

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
