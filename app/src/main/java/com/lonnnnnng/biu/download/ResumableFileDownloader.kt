package com.lonnnnnng.biu.download

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class ResumableFileDownloader(
    private val httpClient: OkHttpClient,
) {
    @Volatile
    private var activeCall: Call? = null

    fun cancelActiveCall() {
        activeCall?.cancel()
    }

    suspend fun download(
        urls: List<String>,
        targetFile: File,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): Long {
        require(urls.isNotEmpty()) { "没有可用的下载地址" }
        targetFile.parentFile?.let { directory ->
            check(directory.exists() || directory.mkdirs()) { "无法创建下载临时目录" }
        }
        var lastError: Throwable? = null
        for (url in urls.distinct()) {
            currentCoroutineContext().ensureActive()
            try {
                return downloadFromUrl(url, targetFile, onProgress)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                currentCoroutineContext().ensureActive()
                lastError = error
            }
        }
        throw lastError ?: IOException("没有可用的下载地址")
    }

    private suspend fun downloadFromUrl(
        url: String,
        targetFile: File,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Long {
        var requestedOffset = targetFile.takeIf(File::isFile)?.length() ?: 0L
        var allowRangeReset = true
        while (true) {
            val request = Request.Builder()
                .url(url)
                .apply {
                    if (requestedOffset > 0L) header("Range", "bytes=$requestedOffset-")
                }
                .build()
            val call = httpClient.newCall(request)
            activeCall = call
            try {
                call.execute().use { response ->
                    if (
                        response.code == HTTP_RANGE_NOT_SATISFIABLE &&
                        requestedOffset > 0L &&
                        allowRangeReset
                    ) {
                        // long: CDN 更换文件版本后旧断点可能越界；只清空一次再从头请求，避免异常服务端造成无限循环。
                        FileOutputStream(targetFile, false).use { }
                        onProgress(0L, 0L)
                        requestedOffset = 0L
                        allowRangeReset = false
                        continue
                    }
                    if (!response.isSuccessful || response.body == null) {
                        throw IOException("CDN 返回 HTTP ${response.code}")
                    }
                    return writeResponse(response, targetFile, requestedOffset, onProgress)
                }
            } finally {
                if (activeCall === call) activeCall = null
            }
        }
    }

    private suspend fun writeResponse(
        response: Response,
        targetFile: File,
        requestedOffset: Long,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Long {
        val append = requestedOffset > 0L && response.code == HTTP_PARTIAL_CONTENT
        var downloadedBytes = if (append) requestedOffset else 0L
        val responseLength = response.body?.contentLength()?.coerceAtLeast(0L) ?: 0L
        val totalBytes = contentRangeTotal(response.header("Content-Range"))
            ?: responseLength.takeIf { it > 0L }?.plus(downloadedBytes)
            ?: 0L
        if (!append && requestedOffset > 0L) {
            // long: CDN 忽略 Range 返回完整文件时，先清除 UI/Room 的旧断点，再用覆盖模式写入，避免进度与磁盘内容短暂不一致。
            onProgress(0L, totalBytes)
        }
        FileOutputStream(targetFile, append).use { output ->
            response.body?.byteStream()?.use { input ->
                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    downloadedBytes += count
                    onProgress(downloadedBytes, totalBytes)
                }
                output.flush()
            } ?: throw IOException("CDN 响应为空")
        }
        if (totalBytes > 0L && downloadedBytes != totalBytes) {
            throw IOException("下载字节数校验失败")
        }
        return downloadedBytes
    }
}

private fun contentRangeTotal(contentRange: String?): Long? {
    return contentRange
        ?.substringAfterLast('/', missingDelimiterValue = "")
        ?.takeUnless { it.isBlank() || it == "*" }
        ?.toLongOrNull()
}

private const val HTTP_PARTIAL_CONTENT = 206
private const val HTTP_RANGE_NOT_SATISFIABLE = 416
private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
