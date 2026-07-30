package com.lonnnnnng.biu.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class VideoDownloadPublisher(context: Context) {
    private val contentResolver = context.applicationContext.contentResolver

    suspend fun publish(request: VideoDownloadRequest, sourceFile: File): Uri = withContext(Dispatchers.IO) {
        require(sourceFile.isFile && sourceFile.length() > 0L) { "合并视频为空，无法发布" }
        val spec = VideoDownloadPublishPolicy.spec(request, Build.VERSION.SDK_INT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishScoped(spec, sourceFile)
        } else {
            publishLegacy(spec, sourceFile)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun publishScoped(spec: VideoDownloadPublishSpec, sourceFile: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, spec.displayName)
            put(MediaStore.Video.Media.MIME_TYPE, spec.mimeType)
            put(MediaStore.Video.Media.TITLE, spec.title)
            put(MediaStore.Video.Media.RELATIVE_PATH, spec.relativePath)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val target = contentResolver.insert(collection, values)
            ?: error("MediaStore 无法创建视频文件")
        try {
            contentResolver.openOutputStream(target, "w")?.use { output ->
                copyWithCancellation(sourceFile, output)
            } ?: error("MediaStore 无法写入视频文件")
            val updatedRows = contentResolver.update(
                target,
                ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                null,
                null,
            )
            check(updatedRows > 0) { "MediaStore 无法完成视频发布" }
            return target
        } catch (error: Throwable) {
            // long: 发布中断时删除 pending 项，避免系统相册保留不可见或永远未完成的半文件。
            runCatching { contentResolver.delete(target, null, null) }
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun publishLegacy(spec: VideoDownloadPublishSpec, sourceFile: File): Uri {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Biu",
        ).apply { check(exists() || mkdirs()) { "无法创建 Movies/Biu 目录" } }
        val targetFile = uniqueLegacyFile(directory, spec.displayName)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, targetFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, spec.mimeType)
            put(MediaStore.Video.Media.TITLE, spec.title)
            put(MediaStore.Video.Media.DATA, targetFile.absolutePath)
        }
        val target = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore 无法创建视频文件")
        try {
            contentResolver.openOutputStream(target, "w")?.use { output ->
                copyWithCancellation(sourceFile, output)
            } ?: error("MediaStore 无法写入视频文件")
            return target
        } catch (error: Throwable) {
            runCatching { contentResolver.delete(target, null, null) }
            throw error
        }
    }

    private suspend fun copyWithCancellation(sourceFile: File, output: OutputStream) {
        FileInputStream(sourceFile).use { input ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
            }
            output.flush()
        }
    }

    private fun uniqueLegacyFile(directory: File, displayName: String): File {
        val direct = File(directory, displayName)
        if (!direct.exists()) return direct
        val baseName = displayName.substringBeforeLast('.', missingDelimiterValue = displayName)
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "mp4")
        var suffix = 2
        while (true) {
            val candidate = File(directory, "$baseName ($suffix).$extension")
            if (!candidate.exists()) return candidate
            suffix += 1
        }
    }
}

private const val COPY_BUFFER_SIZE = 64 * 1024
