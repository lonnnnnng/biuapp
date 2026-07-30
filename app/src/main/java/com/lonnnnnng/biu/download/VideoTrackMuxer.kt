package com.lonnnnnng.biu.download

import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.media3.common.C
import androidx.media3.common.util.MediaFormatUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.BufferInfo
import androidx.media3.muxer.Mp4Muxer
import androidx.media3.muxer.SeekableMuxerOutput
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@UnstableApi
class VideoTrackMuxer {
    suspend fun mux(videoFile: File, audioFile: File, outputFile: File): Long = withContext(Dispatchers.IO) {
        require(videoFile.isFile && videoFile.length() > 0L) { "视频轨文件为空" }
        require(audioFile.isFile && audioFile.length() > 0L) { "音频轨文件为空" }
        outputFile.parentFile?.let { directory ->
            check(directory.exists() || directory.mkdirs()) { "无法创建视频合并临时目录" }
        }
        check(!outputFile.exists() || outputFile.delete()) { "无法覆盖旧的视频合并文件" }

        val videoInput = openTrack(videoFile, C.TRACK_TYPE_VIDEO)
        val audioInput = openTrack(audioFile, C.TRACK_TYPE_AUDIO)
        val muxer = Mp4Muxer.Builder(
            SeekableMuxerOutput.of(FileOutputStream(outputFile, false)),
        ).build()
        var samplesCompleted = false
        var muxerClosed = false
        try {
            videoInput.outputTrackId = muxer.addTrack(MediaFormatUtil.createFormatFromMediaFormat(videoInput.format))
            audioInput.outputTrackId = muxer.addTrack(MediaFormatUtil.createFormatFromMediaFormat(audioInput.format))
            // long: DASH 的音视频时间轴需要按时间戳交错写入；整轨先后写会生成可播放性差、无法流式读取的 MP4。
            while (!videoInput.ended || !audioInput.ended) {
                currentCoroutineContext().ensureActive()
                val next = when {
                    videoInput.ended -> audioInput
                    audioInput.ended -> videoInput
                    videoInput.sampleTimeUs <= audioInput.sampleTimeUs -> videoInput
                    else -> audioInput
                }
                writeNextSample(next, muxer)
            }
            samplesCompleted = true
        } finally {
            videoInput.extractor.release()
            audioInput.extractor.release()
            try {
                muxer.close()
                muxerClosed = true
            } catch (closeError: Throwable) {
                if (samplesCompleted) throw closeError
            } finally {
                if (!samplesCompleted || !muxerClosed) outputFile.delete()
            }
        }
        check(outputFile.isFile && outputFile.length() > 0L) { "视频合并结果为空" }
        outputFile.length()
    }

    private fun openTrack(file: File, trackType: Int): ExtractorInput {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                val format = extractor.getTrackFormat(index)
                when (trackType) {
                    C.TRACK_TYPE_VIDEO -> MediaFormatUtil.isVideoFormat(format)
                    C.TRACK_TYPE_AUDIO -> MediaFormatUtil.isAudioFormat(format)
                    else -> false
                }
            } ?: throw IOException(if (trackType == C.TRACK_TYPE_VIDEO) "文件中没有视频轨" else "文件中没有音频轨")
            extractor.selectTrack(trackIndex)
            return ExtractorInput(
                extractor = extractor,
                format = extractor.getTrackFormat(trackIndex),
                buffer = ByteBuffer.allocateDirect(sampleBufferSize(extractor.getTrackFormat(trackIndex))),
            )
        } catch (error: Throwable) {
            extractor.release()
            throw error
        }
    }

    private fun writeNextSample(input: ExtractorInput, muxer: Mp4Muxer) {
        input.buffer.clear()
        val size = input.extractor.readSampleData(input.buffer, 0)
        if (size < 0) {
            input.ended = true
            return
        }
        val presentationTimeUs = input.extractor.sampleTime
        val flags = if (input.extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            C.BUFFER_FLAG_KEY_FRAME
        } else {
            0
        }
        input.buffer.position(0)
        input.buffer.limit(size)
        muxer.writeSampleData(
            input.outputTrackId,
            input.buffer,
            BufferInfo(presentationTimeUs, size, flags),
        )
        if (!input.extractor.advance()) input.ended = true
    }

    private fun sampleBufferSize(format: MediaFormat): Int {
        val declaredSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            runCatching { format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }.getOrDefault(0)
        } else {
            0
        }
        return declaredSize.coerceAtLeast(DEFAULT_SAMPLE_BUFFER_SIZE)
    }
}

private data class ExtractorInput(
    val extractor: MediaExtractor,
    val format: MediaFormat,
    val buffer: ByteBuffer,
    var outputTrackId: Int = -1,
    var ended: Boolean = false,
) {
    val sampleTimeUs: Long
        get() = extractor.sampleTime.takeIf { it >= 0L } ?: Long.MAX_VALUE
}

private const val DEFAULT_SAMPLE_BUFFER_SIZE = 4 * 1024 * 1024
