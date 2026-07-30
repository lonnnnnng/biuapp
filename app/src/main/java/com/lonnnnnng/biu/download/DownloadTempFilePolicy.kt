package com.lonnnnnng.biu.download

import java.io.File

data class VideoDownloadTempFiles(
    val video: File,
    val audio: File,
    val output: File,
)

object DownloadTempFilePolicy {
    fun audio(filesDir: File, taskId: String): File {
        return File(File(filesDir, "audio-downloads"), "${safeTaskId(taskId)}.m4a.part")
    }

    fun video(filesDir: File, taskId: String): VideoDownloadTempFiles {
        val safeId = safeTaskId(taskId)
        val directory = File(filesDir, "video-downloads")
        return VideoDownloadTempFiles(
            video = File(directory, "$safeId.video.m4s"),
            audio = File(directory, "$safeId.audio.m4s"),
            output = File(directory, "$safeId.output.mp4"),
        )
    }

    private fun safeTaskId(taskId: String): String {
        return taskId.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
