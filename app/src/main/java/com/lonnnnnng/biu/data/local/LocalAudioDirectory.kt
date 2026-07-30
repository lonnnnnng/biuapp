package com.lonnnnnng.biu.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.localAudioDirectoryDataStore by preferencesDataStore(name = "local_audio_directory")

data class LocalAudioDirectory(
    val treeUri: String,
    val displayName: String,
    val volumeName: String,
    val relativePath: String,
)

internal data class LocalAudioMediaStoreFilter(
    val volumeName: String,
    val selection: String?,
    val selectionArgs: List<String>,
)

internal object LocalAudioDirectoryPolicy {
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    fun fromTreeDocument(
        treeUri: String,
        authority: String?,
        treeDocumentId: String,
        displayName: String,
    ): LocalAudioDirectory? {
        if (authority != EXTERNAL_STORAGE_AUTHORITY) return null
        val separatorIndex = treeDocumentId.indexOf(':')
        if (separatorIndex <= 0) return null

        val storageVolumeId = treeDocumentId.substring(0, separatorIndex).trim()
        if (storageVolumeId.isBlank()) return null
        val relativePath = treeDocumentId.substring(separatorIndex + 1)
            .trim('/')
            .let { normalized -> if (normalized.isBlank()) "" else "$normalized/" }
        val volumeName = if (storageVolumeId.equals("primary", ignoreCase = true)) {
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        } else {
            storageVolumeId
        }
        val fallbackName = relativePath.trimEnd('/').substringAfterLast('/').ifBlank { storageVolumeId }

        return LocalAudioDirectory(
            treeUri = treeUri,
            displayName = displayName.trim().ifBlank { fallbackName },
            volumeName = volumeName,
            relativePath = relativePath,
        )
    }
}

internal object LocalAudioMediaStorePolicy {
    fun filterFor(
        directory: LocalAudioDirectory,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): LocalAudioMediaStoreFilter {
        if (sdkInt < Build.VERSION_CODES.Q) {
            // long: Android 9 及以下没有 RELATIVE_PATH，静默回退会把目录筛选误显示成全库结果。
            throw UnsupportedOperationException("目录筛选需要 Android 10 或更高版本")
        }
        if (directory.relativePath.isBlank()) {
            return LocalAudioMediaStoreFilter(
                volumeName = directory.volumeName,
                selection = null,
                selectionArgs = emptyList(),
            )
        }

        val escapedPath = directory.relativePath
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return LocalAudioMediaStoreFilter(
            volumeName = directory.volumeName,
            selection = """(${MediaStore.Audio.Media.RELATIVE_PATH} = ? OR ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? ESCAPE '\')""",
            selectionArgs = listOf(directory.relativePath, "$escapedPath%"),
        )
    }
}

class LocalAudioDirectoryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val dataStore = appContext.localAudioDirectoryDataStore

    suspend fun currentDirectory(): LocalAudioDirectory? = withContext(Dispatchers.IO) {
        val preferences = dataStore.data.first()
        val treeUri = preferences[KEY_TREE_URI]?.takeIf(String::isNotBlank) ?: return@withContext null
        val hasPersistedReadAccess = runCatching {
            contentResolver.persistedUriPermissions.any { permission ->
                permission.isReadPermission && permission.uri.toString() == treeUri
            }
        }.getOrDefault(false)
        if (!hasPersistedReadAccess) {
            // long: 系统设置中撤销目录授权后不能继续展示过期筛选，冷启动时直接回到“全部音乐”。
            clearStoredDirectory()
            return@withContext null
        }

        val displayName = preferences[KEY_DISPLAY_NAME]?.takeIf(String::isNotBlank)
            ?: return@withContext clearInvalidDirectory(treeUri)
        val volumeName = preferences[KEY_VOLUME_NAME]?.takeIf(String::isNotBlank)
            ?: return@withContext clearInvalidDirectory(treeUri)
        val relativePath = preferences[KEY_RELATIVE_PATH]
            ?: return@withContext clearInvalidDirectory(treeUri)
        LocalAudioDirectory(
            treeUri = treeUri,
            displayName = displayName,
            volumeName = volumeName,
            relativePath = relativePath,
        )
    }

    suspend fun selectDirectory(treeUri: Uri): LocalAudioDirectory = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw UnsupportedOperationException("目录筛选需要 Android 10 或更高版本")
        }
        val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }
            .getOrElse { throw IllegalArgumentException("无法识别所选目录", it) }
        val directory = LocalAudioDirectoryPolicy.fromTreeDocument(
            treeUri = treeUri.toString(),
            authority = treeUri.authority,
            treeDocumentId = treeDocumentId,
            displayName = queryDisplayName(treeUri, treeDocumentId),
        ) ?: throw IllegalArgumentException("请选择设备存储中的音乐目录")

        val previousTreeUri = dataStore.data.first()[KEY_TREE_URI]
        // long: 目录筛选需要跨进程重启保留，只有显式接收系统提供的持久授权才能在下次启动继续使用。
        contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            dataStore.edit { preferences ->
                preferences[KEY_TREE_URI] = directory.treeUri
                preferences[KEY_DISPLAY_NAME] = directory.displayName
                preferences[KEY_VOLUME_NAME] = directory.volumeName
                preferences[KEY_RELATIVE_PATH] = directory.relativePath
            }
        } catch (error: Exception) {
            if (previousTreeUri != directory.treeUri) releasePersistedReadPermission(directory.treeUri)
            throw error
        }
        if (!previousTreeUri.isNullOrBlank() && previousTreeUri != directory.treeUri) {
            // long: 改选目录后只保留当前范围的读取权，防止多次选择逐步累积不再需要的 SAF 授权。
            releasePersistedReadPermission(previousTreeUri)
        }
        directory
    }

    suspend fun clearDirectory() = withContext(Dispatchers.IO) {
        dataStore.data.first()[KEY_TREE_URI]?.takeIf(String::isNotBlank)?.let { storedUri ->
            // long: 清除筛选同时归还 SAF 授权，避免应用长期保留用户已经不再使用的目录访问权。
            releasePersistedReadPermission(storedUri)
        }
        clearStoredDirectory()
    }

    private fun queryDisplayName(treeUri: Uri, treeDocumentId: String): String {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
        return runCatching {
            contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            }.orEmpty()
        }.getOrDefault("")
    }

    private suspend fun clearInvalidDirectory(treeUri: String): LocalAudioDirectory? {
        // long: 配置损坏时也归还仍有效的目录授权，避免 DataStore 已失效但系统授权继续残留。
        releasePersistedReadPermission(treeUri)
        clearStoredDirectory()
        return null
    }

    private suspend fun clearStoredDirectory() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private fun releasePersistedReadPermission(treeUri: String) {
        runCatching {
            contentResolver.releasePersistableUriPermission(
                Uri.parse(treeUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private companion object {
        val KEY_TREE_URI = stringPreferencesKey("tree_uri")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_VOLUME_NAME = stringPreferencesKey("volume_name")
        val KEY_RELATIVE_PATH = stringPreferencesKey("relative_path")
    }
}
