package com.lonnnnnng.biu.data.lyrics

object LrcParser {
    private val timeTag = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    fun parse(content: String): List<LyricsLine> {
        return content.lineSequence()
            .flatMap { rawLine ->
                val matches = timeTag.findAll(rawLine).toList()
                val text = timeTag.replace(rawLine, "").trim()
                if (matches.isEmpty() || text.isBlank()) {
                    emptySequence()
                } else {
                    // long: 一行 LRC 可以同时标记多个播放时间，每个标签都要展开为独立时间轴节点，否则副歌重复段只会显示第一次。
                    matches.asSequence().map { match ->
                        LyricsLine(
                            startTimeMs = match.toMilliseconds(),
                            text = text,
                        )
                    }
                }
            }
            .sortedBy(LyricsLine::startTimeMs)
            .toList()
    }

    fun currentLineIndex(
        lines: List<LyricsLine>,
        positionMs: Long,
        offsetMs: Long = 0L,
    ): Int {
        if (lines.isEmpty()) return -1
        val effectivePosition = (positionMs + offsetMs).coerceAtLeast(0L)
        return lines.indexOfLast { line -> line.startTimeMs <= effectivePosition }
    }

    private fun MatchResult.toMilliseconds(): Long {
        val minutes = groupValues[1].toLong()
        val seconds = groupValues[2].toLong()
        val fraction = groupValues[3]
        val fractionMs = when (fraction.length) {
            1 -> fraction.toLong() * 100L
            2 -> fraction.toLong() * 10L
            3 -> fraction.toLong()
            else -> 0L
        }
        return (minutes * 60L + seconds) * 1_000L + fractionMs
    }
}
