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

class AudioDownloadPublisher(context: Context) {
    private val contentResolver = context.applicationContext.contentResolver

    suspend fun publish(request: AudioDownloadRequest, sourceFile: File): Uri = withContext(Dispatchers.IO) {
        require(sourceFile.isFile && sourceFile.length() > 0L) { "下载文件为空，无法发布" }
        val spec = AudioDownloadPublishPolicy.spec(request, Build.VERSION.SDK_INT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishScoped(spec, sourceFile)
        } else {
            publishLegacy(spec, sourceFile)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun publishScoped(spec: AudioDownloadPublishSpec, sourceFile: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, spec.displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, spec.mimeType)
            put(MediaStore.Audio.Media.TITLE, spec.title)
            put(MediaStore.Audio.Media.ARTIST, spec.artist)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.RELATIVE_PATH, spec.relativePath)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val target = contentResolver.insert(collection, values)
            ?: error("MediaStore 无法创建音频文件")
        try {
            contentResolver.openOutputStream(target, "w")?.use { output ->
                copyWithCancellation(sourceFile, output)
            } ?: error("MediaStore 无法写入音频文件")
            val updatedRows = contentResolver.update(
                target,
                ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                    // long: MediaProvider 扫描无标签的 DASH 容器时会回退到文件名和未知作者，发布完成时重新写回在线曲目的展示元数据。
                    put(MediaStore.Audio.Media.TITLE, spec.title)
                    put(MediaStore.Audio.Media.ARTIST, spec.artist)
                },
                null,
                null,
            )
            check(updatedRows > 0) { "MediaStore 无法完成音频发布" }
            return target
        } catch (error: Throwable) {
            // long: 发布被暂停、取消或写入失败时删除 pending 条目，本地音乐页不能残留永远不可见的半文件。
            runCatching { contentResolver.delete(target, null, null) }
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun publishLegacy(spec: AudioDownloadPublishSpec, sourceFile: File): Uri {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Biu",
        ).apply { check(exists() || mkdirs()) { "无法创建 Music/Biu 目录" } }
        val targetFile = uniqueLegacyFile(directory, spec.displayName)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, targetFile.name)
            put(MediaStore.Audio.Media.MIME_TYPE, spec.mimeType)
            put(MediaStore.Audio.Media.TITLE, spec.title)
            put(MediaStore.Audio.Media.ARTIST, spec.artist)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.DATA, targetFile.absolutePath)
        }
        val target = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore 无法创建音频文件")
        try {
            contentResolver.openOutputStream(target, "w")?.use { output ->
                copyWithCancellation(sourceFile, output)
            } ?: error("MediaStore 无法写入音频文件")
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
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "m4a")
        var suffix = 2
        while (true) {
            val candidate = File(directory, "$baseName ($suffix).$extension")
            if (!candidate.exists()) return candidate
            suffix += 1
        }
    }
}

private const val COPY_BUFFER_SIZE = 64 * 1024
