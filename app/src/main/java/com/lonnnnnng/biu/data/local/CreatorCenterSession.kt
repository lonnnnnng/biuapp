package com.lonnnnnng.biu.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.creatorCenterSessionDataStore by preferencesDataStore(name = "creator_center_session")

enum class CreatorCenterListSlot {
    SEARCH,
    FOLLOWING,
    HOME_SELECTED,
    WORKS,
    COLLECTIONS,
    COLLECTION_VIDEOS,
}

data class PersistedListPosition(
    val index: Int = 0,
    val offset: Int = 0,
) {
    fun normalized(): PersistedListPosition = copy(
        index = index.coerceAtLeast(0),
        offset = offset.coerceAtLeast(0),
    )
}

data class CreatorSourceDraftItem(
    val mid: Long,
    val name: String,
    val faceUrl: String,
)

data class CreatorCenterSession(
    val tab: String = "",
    val selectedGroupId: Long? = null,
    val searchInput: String = "",
    val searchKeyword: String = "",
    val filterKeyword: String = "",
    val selectedCreatorMid: Long? = null,
    val selectedCreatorName: String = "",
    val selectedCreatorFaceUrl: String = "",
    val profileTab: String = "",
    val listPositions: Map<CreatorCenterListSlot, PersistedListPosition> = emptyMap(),
    val hasSourceDraft: Boolean = false,
    val sourceDraft: List<CreatorSourceDraftItem> = emptyList(),
) {
    fun position(slot: CreatorCenterListSlot): PersistedListPosition =
        listPositions[slot]?.normalized() ?: PersistedListPosition()
}

object CreatorCenterSessionPolicy {
    internal fun encode(session: CreatorCenterSession): String {
        val positions = JSONObject().apply {
            session.listPositions.forEach { (slot, rawPosition) ->
                val position = rawPosition.normalized()
                put(
                    slot.name,
                    JSONObject()
                        .put("index", position.index)
                        .put("offset", position.offset),
                )
            }
        }
        val draft = JSONArray().apply {
            session.sourceDraft
                .filter { item -> item.mid > 0L }
                .distinctBy(CreatorSourceDraftItem::mid)
                .forEach { item ->
                    put(
                        JSONObject()
                            .put("mid", item.mid)
                            .put("name", item.name)
                            .put("faceUrl", item.faceUrl),
                    )
                }
        }
        return JSONObject()
            .put("tab", session.tab)
            .put("selectedGroupId", session.selectedGroupId ?: JSONObject.NULL)
            .put("searchInput", session.searchInput)
            .put("searchKeyword", session.searchKeyword)
            .put("filterKeyword", session.filterKeyword)
            .put("selectedCreatorMid", session.selectedCreatorMid ?: JSONObject.NULL)
            .put("selectedCreatorName", session.selectedCreatorName)
            .put("selectedCreatorFaceUrl", session.selectedCreatorFaceUrl)
            .put("profileTab", session.profileTab)
            .put("listPositions", positions)
            .put("hasSourceDraft", session.hasSourceDraft)
            .put("sourceDraft", draft)
            .toString()
    }

    internal fun decode(raw: String?): CreatorCenterSession {
        if (raw.isNullOrBlank()) return CreatorCenterSession()
        return runCatching {
            val json = JSONObject(raw)
            val positionsJson = json.optJSONObject("listPositions")
            val positions = buildMap {
                CreatorCenterListSlot.entries.forEach { slot ->
                    val positionJson = positionsJson?.optJSONObject(slot.name) ?: return@forEach
                    put(
                        slot,
                        PersistedListPosition(
                            index = positionJson.optInt("index").coerceAtLeast(0),
                            offset = positionJson.optInt("offset").coerceAtLeast(0),
                        ),
                    )
                }
            }
            val draftJson = json.optJSONArray("sourceDraft")
            val draft = buildList {
                repeat(draftJson?.length() ?: 0) { index ->
                    val item = draftJson?.optJSONObject(index) ?: return@repeat
                    val mid = item.optLong("mid")
                    if (mid > 0L) {
                        add(
                            CreatorSourceDraftItem(
                                mid = mid,
                                name = item.optString("name").take(MAX_NAME_LENGTH),
                                faceUrl = item.optString("faceUrl").take(MAX_URL_LENGTH),
                            ),
                        )
                    }
                }
            }.distinctBy(CreatorSourceDraftItem::mid)
            CreatorCenterSession(
                tab = json.optString("tab"),
                selectedGroupId = json.optNullablePositiveLong("selectedGroupId"),
                searchInput = json.optString("searchInput").take(MAX_QUERY_LENGTH),
                searchKeyword = json.optString("searchKeyword").take(MAX_QUERY_LENGTH),
                filterKeyword = json.optString("filterKeyword").take(MAX_QUERY_LENGTH),
                selectedCreatorMid = json.optNullablePositiveLong("selectedCreatorMid"),
                selectedCreatorName = json.optString("selectedCreatorName").take(MAX_NAME_LENGTH),
                selectedCreatorFaceUrl = json.optString("selectedCreatorFaceUrl").take(MAX_URL_LENGTH),
                profileTab = json.optString("profileTab"),
                listPositions = positions,
                hasSourceDraft = json.optBoolean("hasSourceDraft"),
                sourceDraft = draft,
            )
        }.getOrDefault(CreatorCenterSession())
    }

    private fun JSONObject.optNullablePositiveLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key).takeIf { value -> value > 0L }
    }

    private const val MAX_QUERY_LENGTH = 160
    private const val MAX_NAME_LENGTH = 160
    private const val MAX_URL_LENGTH = 2_048
}

class CreatorCenterSessionRepository(context: Context) {
    private val dataStore = context.applicationContext.creatorCenterSessionDataStore

    val session: Flow<CreatorCenterSession> = dataStore.data.map { values ->
        CreatorCenterSessionPolicy.decode(values[KEY_SESSION])
    }

    suspend fun save(session: CreatorCenterSession) {
        dataStore.edit { values ->
            values[KEY_SESSION] = CreatorCenterSessionPolicy.encode(session)
        }
    }

    private companion object {
        val KEY_SESSION = stringPreferencesKey("state")
    }
}
