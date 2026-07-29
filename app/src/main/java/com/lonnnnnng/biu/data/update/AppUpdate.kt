package com.lonnnnnng.biu.data.update

import java.io.IOException
import java.math.BigInteger
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AppUpdate(
    val version: String,
    val tagName: String,
    val releaseNotes: String,
    val downloadUrl: String,
)

class AppUpdateRepository(
    private val client: OkHttpClient,
    private val latestReleaseUrl: String = LATEST_RELEASE_URL,
) {
    suspend fun latestRelease(): AppUpdate = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(latestReleaseUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "BiuApp-Android")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub Release 请求失败（HTTP ${response.code}）")
            }
            parseRelease(response.body?.string().orEmpty())
        }
    }

    internal fun parseRelease(json: String): AppUpdate {
        val release = JSONObject(json)
        val tagName = release.optString("tag_name").trim()
        require(tagName.isNotBlank()) { "GitHub Release 缺少版本号" }
        val assets = release.optJSONArray("assets")
            ?: throw IllegalStateException("GitHub Release 缺少 APK 资产")
        val apkAssets = (0 until assets.length())
            .mapNotNull { index -> assets.optJSONObject(index) }
            .filter { asset ->
                val name = asset.optString("name")
                val contentType = asset.optString("content_type")
                // long: 只接受明确的 APK 文件，避免把同一 Release 中的校验文件当成安装包下载。
                name.endsWith(".apk", ignoreCase = true) ||
                    contentType == "application/vnd.android.package-archive"
            }
        val preferredAssetNames = setOf(
            "BiuApp-$tagName.apk",
            "BiuApp-${tagName.removePrefix("v").removePrefix("V")}.apk",
        )
        val apkAsset = apkAssets.firstOrNull { it.optString("name") in preferredAssetNames }
            ?: apkAssets.singleOrNull()
            ?: if (apkAssets.isEmpty()) {
                throw IllegalStateException("GitHub Release 中没有可安装的 APK 资产")
            } else {
                throw IllegalStateException("GitHub Release 包含多个 APK，无法确定通用安装包")
            }
        val downloadUrl = apkAsset.optString("browser_download_url").trim()
        require(downloadUrl.isNotBlank()) { "GitHub Release 的 APK 下载地址为空" }
        val downloadUri = runCatching { URI(downloadUrl) }.getOrNull()
        require(downloadUri?.scheme == "https" && downloadUri.host == "github.com") {
            "GitHub Release 的 APK 下载地址不受信任"
        }
        return AppUpdate(
            version = tagName.removePrefix("v").removePrefix("V"),
            tagName = tagName,
            releaseNotes = release.optString("body").trim().ifBlank { "本版本未提供更新说明。" },
            downloadUrl = downloadUrl,
        )
    }

    private companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/lonnnnnng/biuapp/releases/latest"
    }
}

internal object AppVersionPolicy {
    fun isNewer(latest: String, current: String): Boolean {
        val latestParts = normalizedParts(latest)
        val currentParts = normalizedParts(current)
        val count = maxOf(latestParts.size, currentParts.size)
        for (index in 0 until count) {
            val latestPart = latestParts.getOrElse(index) { BigInteger.ZERO }
            val currentPart = currentParts.getOrElse(index) { BigInteger.ZERO }
            if (latestPart != currentPart) return latestPart > currentPart
        }
        return false
    }

    private fun normalizedParts(version: String): List<BigInteger> {
        val normalized = version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
        // long: 版本比较只接受数字点分格式，并用任意精度整数避免异常大版本号溢出后被误判为旧版本。
        require(normalized.matches(Regex("\\d+(?:\\.\\d+)*"))) { "无效版本号：$version" }
        return normalized
            .split('.')
            .map(::BigInteger)
    }
}
