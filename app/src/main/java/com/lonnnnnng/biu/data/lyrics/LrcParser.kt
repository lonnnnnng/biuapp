package com.lonnnnnng.biu.data.lyrics

object LrcParser {
    private val timeTag = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    fun parse(content: String): List<LyricsLine> {
        val timedTexts = content.lineSequence()
            .flatMapIndexed { lineIndex, rawLine ->
                val matches = timeTag.findAll(rawLine).toList()
                val text = timeTag.replace(rawLine, "").trim()
                if (matches.isEmpty() || text.isBlank()) {
                    emptySequence()
                } else {
                    // long: 一行 LRC 可以同时标记多个播放时间，每个标签都要展开为独立时间轴节点，否则副歌重复段只会显示第一次。
                    matches.asSequence().mapIndexed { matchIndex, match ->
                        TimedText(
                            startTimeMs = match.toMilliseconds(),
                            text = text,
                            sourceOrder = lineIndex to matchIndex,
                        )
                    }
                }
            }
            .toList()

        return timedTexts
            .groupBy(TimedText::startTimeMs)
            .toSortedMap()
            .mapNotNull { (startTimeMs, entries) ->
                // long: LRCLIB 常用相同时间戳的连续两行承载原文和翻译；合并成一个节点后，高亮和自动滚动不会在同一时刻跳两次。
                val texts = entries
                    .sortedWith(compareBy<TimedText> { it.sourceOrder.first }.thenBy { it.sourceOrder.second })
                    .map(TimedText::text)
                    .distinct()
                val primary = texts.firstOrNull() ?: return@mapNotNull null
                LyricsLine(
                    startTimeMs = startTimeMs,
                    text = primary,
                    translation = texts.drop(1).joinToString("\n").takeIf(String::isNotBlank),
                )
            }
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

    private data class TimedText(
        val startTimeMs: Long,
        val text: String,
        val sourceOrder: Pair<Int, Int>,
    )
}
