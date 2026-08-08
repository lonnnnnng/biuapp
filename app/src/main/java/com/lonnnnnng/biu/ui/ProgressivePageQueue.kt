package com.lonnnnnng.biu.ui

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal enum class QueuePlacement {
    PREPEND,
    APPEND,
}

internal data class QueueExpansion<T>(
    val placement: QueuePlacement,
    val item: T,
)

internal data class PlaybackQueueSnapshot<T>(
    val queueId: Long,
    val items: List<T>,
    val startIndex: Int,
    val startPositionMs: Long,
) {
    init {
        require(items.isNotEmpty()) { "播放队列不能为空" }
        require(startIndex in items.indices) { "播放起始索引越界" }
    }
}

internal class PlaybackQueueSnapshotStore<T>(
    private val itemId: (T) -> String,
) {
    private var snapshot: PlaybackQueueSnapshot<T>? = null

    @Synchronized
    fun replace(
        queueId: Long,
        items: List<T>,
        startIndex: Int,
        startPositionMs: Long,
    ): PlaybackQueueSnapshot<T> {
        return PlaybackQueueSnapshot(queueId, items, startIndex, startPositionMs).also { snapshot = it }
    }

    @Synchronized
    fun expand(queueId: Long, placement: QueuePlacement, item: T): PlaybackQueueSnapshot<T>? {
        val current = snapshot?.takeIf { it.queueId == queueId } ?: return null
        // long: 重连窗口可能重放已经进入快照的补齐结果，按媒体 ID 去重可避免队列出现重复分 P。
        if (current.items.any { itemId(it) == itemId(item) }) return current
        val updatedItems = when (placement) {
            QueuePlacement.PREPEND -> listOf(item) + current.items
            QueuePlacement.APPEND -> current.items + item
        }
        return current.copy(
            items = updatedItems,
            startIndex = current.startIndex + if (placement == QueuePlacement.PREPEND) 1 else 0,
        ).also { snapshot = it }
    }

    @Synchronized
    fun append(queueId: Long, items: List<T>): PlaybackQueueSnapshot<T>? {
        val current = snapshot?.takeIf { it.queueId == queueId } ?: return null
        val existingIds = current.items.mapTo(hashSetOf(), itemId)
        val additions = items.filter { item -> existingIds.add(itemId(item)) }
        if (additions.isEmpty()) return current
        return current.copy(items = current.items + additions).also { snapshot = it }
    }

    @Synchronized
    fun updateResumePosition(mediaId: String, positionMs: Long) {
        val current = snapshot ?: return
        val activeIndex = current.items.indexOfFirst { itemId(it) == mediaId }
        if (activeIndex < 0) return
        // long: 服务若在控制器断开期间重建，恢复到用户当前所在的 P 和最近进度，而不是回到最初选择页。
        snapshot = current.copy(startIndex = activeIndex, startPositionMs = positionMs.coerceAtLeast(0L))
    }

    @Synchronized
    fun remove(mediaId: String): PlaybackQueueSnapshot<T>? {
        val current = snapshot ?: return null
        val removeIndex = current.items.indexOfFirst { itemId(it) == mediaId }
        if (removeIndex < 0) return current
        if (current.items.size == 1) {
            snapshot = null
            return null
        }

        val activeId = itemId(current.items[current.startIndex])
        val updatedItems = current.items.toMutableList().apply { removeAt(removeIndex) }
        val retainedActiveIndex = updatedItems.indexOfFirst { itemId(it) == activeId }
        val nextStartIndex = retainedActiveIndex.takeIf { it >= 0 }
            ?: removeIndex.coerceAtMost(updatedItems.lastIndex)
        return current.copy(
            items = updatedItems,
            startIndex = nextStartIndex,
            startPositionMs = if (retainedActiveIndex >= 0) current.startPositionMs else 0L,
        ).also { snapshot = it }
    }

    @Synchronized
    fun moveNext(mediaId: String, currentMediaId: String): PlaybackQueueSnapshot<T>? {
        val current = snapshot ?: return null
        val targetIndex = current.items.indexOfFirst { itemId(it) == mediaId }
        val activeIndex = current.items.indexOfFirst { itemId(it) == currentMediaId }
        if (targetIndex < 0 || activeIndex < 0 || targetIndex == activeIndex) return current

        val updatedItems = current.items.toMutableList()
        val target = updatedItems.removeAt(targetIndex)
        val updatedActiveIndex = updatedItems.indexOfFirst { itemId(it) == currentMediaId }
        val insertIndex = (updatedActiveIndex + 1).coerceAtMost(updatedItems.size)
        updatedItems.add(insertIndex, target)
        return current.copy(items = updatedItems, startIndex = updatedActiveIndex).also { snapshot = it }
    }

    @Synchronized
    fun move(mediaId: String, targetIndex: Int): PlaybackQueueSnapshot<T>? {
        val current = snapshot ?: return null
        val sourceIndex = current.items.indexOfFirst { itemId(it) == mediaId }
        if (sourceIndex < 0) return current
        val boundedTarget = targetIndex.coerceIn(current.items.indices)
        if (sourceIndex == boundedTarget) return current

        val activeId = itemId(current.items[current.startIndex])
        val updatedItems = current.items.toMutableList()
        val movingItem = updatedItems.removeAt(sourceIndex)
        updatedItems.add(boundedTarget.coerceAtMost(updatedItems.size), movingItem)
        // long: 调整顺序不能把恢复锚点留在旧索引；始终按当前媒体 ID 重算，后台落盘后仍恢复同一首和同一进度。
        val updatedActiveIndex = updatedItems.indexOfFirst { itemId(it) == activeId }
        return current.copy(items = updatedItems, startIndex = updatedActiveIndex).also { snapshot = it }
    }

    @Synchronized
    fun reorder(mediaIds: List<String>): PlaybackQueueSnapshot<T>? {
        val current = snapshot ?: return null
        val requestedIds = mediaIds.distinct()
        val currentById = current.items.associateBy(itemId)
        if (requestedIds.size != current.items.size || requestedIds.toSet() != currentById.keys) return current
        val activeId = itemId(current.items[current.startIndex])
        val updatedItems = requestedIds.map { mediaId -> requireNotNull(currentById[mediaId]) }
        val updatedActiveIndex = updatedItems.indexOfFirst { itemId(it) == activeId }
        return current.copy(items = updatedItems, startIndex = updatedActiveIndex).also { snapshot = it }
    }

    @Synchronized
    fun removeAll(mediaIds: Set<String>): PlaybackQueueSnapshot<T>? {
        val current = snapshot ?: return null
        if (mediaIds.isEmpty()) return current
        val retainedItems = current.items.filterNot { item -> itemId(item) in mediaIds }
        if (retainedItems.isEmpty()) {
            snapshot = null
            return null
        }
        val activeId = itemId(current.items[current.startIndex])
        val retainedActiveIndex = retainedItems.indexOfFirst { itemId(it) == activeId }
        // long: 当前歌曲也被批量移除时，播放应衔接到原队列中最靠前的未删除后继；只有后方已无歌曲才回退到剩余末项。
        val successorId = current.items
            .drop(current.startIndex + 1)
            .firstOrNull { item -> itemId(item) !in mediaIds }
            ?.let(itemId)
        val nextStartIndex = retainedActiveIndex.takeIf { it >= 0 }
            ?: successorId?.let { id -> retainedItems.indexOfFirst { itemId(it) == id } }?.takeIf { it >= 0 }
            ?: retainedItems.lastIndex
        return current.copy(
            items = retainedItems,
            startIndex = nextStartIndex,
            startPositionMs = if (retainedActiveIndex >= 0) current.startPositionMs else 0L,
        ).also { snapshot = it }
    }

    @Synchronized
    fun clear() {
        snapshot = null
    }

    @Synchronized
    fun current(queueId: Long? = null): PlaybackQueueSnapshot<T>? {
        return snapshot?.takeIf { queueId == null || it.queueId == queueId }
    }
}

internal class ProgressivePageQueueLoader<T>(
    private val resolve: suspend (pageIndex: Int) -> T,
) {
    suspend fun load(
        pageCount: Int,
        startIndex: Int,
        includePrevious: Boolean = true,
        onSelected: suspend (T) -> Unit,
        onExpansion: suspend (QueueExpansion<T>) -> Unit,
    ) = coroutineScope {
        require(pageCount > 0) { "分 P 数量必须大于 0" }
        require(startIndex in 0 until pageCount) { "分 P 起始索引越界" }

        // long: 首次只等待用户选中的 P，避免 100P 视频在所有 DASH 地址解析完成前一直无法起播。
        onSelected(resolve(startIndex))

        if (includePrevious) {
            launch {
                // long: “全部播放”需要补回前置 P；“从此 P 开始”则跳过这条链路，避免上一曲回到用户明确排除的内容。
                for (pageIndex in startIndex - 1 downTo 0) {
                    onExpansion(QueueExpansion(QueuePlacement.PREPEND, resolve(pageIndex)))
                }
            }
        }
        launch {
            // long: 后置 P 按原顺序追加；与前置链路最多形成两路并发，避免 100P 同时请求触发接口限流。
            for (pageIndex in startIndex + 1 until pageCount) {
                onExpansion(QueueExpansion(QueuePlacement.APPEND, resolve(pageIndex)))
            }
        }
    }
}
