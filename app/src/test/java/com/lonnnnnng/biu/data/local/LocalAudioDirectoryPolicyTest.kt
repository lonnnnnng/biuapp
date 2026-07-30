package com.lonnnnnng.biu.data.local

import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LocalAudioDirectoryPolicyTest {
    @Test
    fun `主存储 SAF 目录映射为 MediaStore 主卷和相对路径`() {
        val directory = LocalAudioDirectoryPolicy.fromTreeDocument(
            treeUri = "content://com.android.externalstorage.documents/tree/primary%3AMusic%2FBiu",
            authority = "com.android.externalstorage.documents",
            treeDocumentId = "primary:Music/Biu",
            displayName = "Biu",
        )

        requireNotNull(directory)
        assertEquals(MediaStore.VOLUME_EXTERNAL_PRIMARY, directory.volumeName)
        assertEquals("Music/Biu/", directory.relativePath)
        assertEquals("Biu", directory.displayName)
    }

    @Test
    fun `可移动存储保留卷标并为根目录生成可读名称`() {
        val directory = LocalAudioDirectoryPolicy.fromTreeDocument(
            treeUri = "content://com.android.externalstorage.documents/tree/1234-5678%3A",
            authority = "com.android.externalstorage.documents",
            treeDocumentId = "1234-5678:",
            displayName = "",
        )

        requireNotNull(directory)
        assertEquals("1234-5678", directory.volumeName)
        assertEquals("", directory.relativePath)
        assertEquals("1234-5678", directory.displayName)
    }

    @Test
    fun `非设备存储文档提供方不能作为 MediaStore 目录筛选`() {
        assertNull(
            LocalAudioDirectoryPolicy.fromTreeDocument(
                treeUri = "content://com.example.cloud/tree/music",
                authority = "com.example.cloud",
                treeDocumentId = "music",
                displayName = "云端音乐",
            ),
        )
    }

    @Test
    fun `目录名称中的空格保持不变以匹配 MediaStore 相对路径`() {
        val directory = LocalAudioDirectoryPolicy.fromTreeDocument(
            treeUri = "content://tree",
            authority = "com.android.externalstorage.documents",
            treeDocumentId = "primary:Music/  Live Sets  ",
            displayName = "  Live Sets  ",
        )

        requireNotNull(directory)
        assertEquals("Music/  Live Sets  /", directory.relativePath)
    }

    @Test
    fun `目录筛选包含当前目录和所有子目录并转义通配符`() {
        val directory = LocalAudioDirectory(
            treeUri = "content://tree",
            displayName = "100%_Music",
            volumeName = MediaStore.VOLUME_EXTERNAL_PRIMARY,
            relativePath = "Music/100%_Music/",
        )

        val filter = LocalAudioMediaStorePolicy.filterFor(directory, sdkInt = 36)

        assertEquals(MediaStore.VOLUME_EXTERNAL_PRIMARY, filter.volumeName)
        assertEquals(
            """(${MediaStore.Audio.Media.RELATIVE_PATH} = ? OR ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? ESCAPE '\')""",
            filter.selection,
        )
        assertEquals(
            listOf("Music/100%_Music/", "Music/100\\%\\_Music/%"),
            filter.selectionArgs,
        )
    }

    @Test
    fun `Android 9 不会静默退化为显示全部音乐`() {
        val directory = LocalAudioDirectory(
            treeUri = "content://tree",
            displayName = "Music",
            volumeName = MediaStore.VOLUME_EXTERNAL_PRIMARY,
            relativePath = "Music/",
        )

        assertThrows(UnsupportedOperationException::class.java) {
            LocalAudioMediaStorePolicy.filterFor(directory, sdkInt = 28)
        }
    }
}
