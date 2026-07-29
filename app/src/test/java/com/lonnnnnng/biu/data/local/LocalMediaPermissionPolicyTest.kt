package com.lonnnnnng.biu.data.local

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMediaPermissionPolicyTest {
    @Test
    fun `Android 13 及以上读取音频使用细分权限`() {
        assertEquals(
            Manifest.permission.READ_MEDIA_AUDIO,
            LocalMediaPermissionPolicy.permissionFor(sdkInt = 33),
        )
    }

    @Test
    fun `Android 12 及以下读取音频使用外部存储权限`() {
        assertEquals(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            LocalMediaPermissionPolicy.permissionFor(sdkInt = 32),
        )
    }
}
