package com.lonnnnnng.biu.download

internal object DownloadFileNamePolicy {
    private val invalidFileNameCharacters = Regex("""[\\/:*?\"<>|\u0000-\u001F]""")
    private val repeatedWhitespace = Regex("\\s+")

    fun displayName(
        title: String,
        artist: String,
        titleFallback: String,
        artistFallback: String,
        extension: String,
    ): String {
        // long: 音频和视频最终都发布到公共媒体目录，统一清理 Android/桌面文件系统保留字符，避免同一曲目产生两套命名规则。
        val safeTitle = safePart(title, titleFallback)
        val safeArtist = safePart(artist, artistFallback)
        return "$safeTitle - $safeArtist.$extension"
    }

    private fun safePart(value: String, fallback: String): String {
        return value
            .replace(invalidFileNameCharacters, "_")
            .replace(repeatedWhitespace, " ")
            .trim(' ', '.')
            .take(MAX_FILE_NAME_PART_LENGTH)
            .trimEnd(' ', '.')
            .ifBlank { fallback }
    }
}

private const val MAX_FILE_NAME_PART_LENGTH = 96
