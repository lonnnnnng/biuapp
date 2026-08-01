package com.lonnnnnng.biu.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import com.lonnnnnng.biu.data.bilibili.BilibiliCreatorRelation
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreatorCenterScreen(
    state: CreatorCenterUiState,
    accountLoggedIn: Boolean,
    resolvingBvid: String?,
    onBack: () -> Unit,
    onTabSelected: (CreatorCenterTab) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onLoadMoreSearch: () -> Unit,
    onLoadFollowing: (Boolean) -> Unit,
    onOpenCreator: (BilibiliCreator) -> Unit,
    onCloseCreator: () -> Unit,
    onToggleRelation: () -> Unit,
    onLoadMoreVideos: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    onAddFavorite: (BilibiliVideo) -> Unit,
    onLogin: () -> Unit,
) {
    BackHandler {
        if (state.selectedCreator != null) onCloseCreator() else onBack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.selectedCreator?.name ?: "UP 主",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (state.selectedCreator != null) onCloseCreator() else onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (state.selectedCreator == null) {
                CreatorDirectory(
                    state = state,
                    accountLoggedIn = accountLoggedIn,
                    onTabSelected = onTabSelected,
                    onSearch = onSearch,
                    onClearSearch = onClearSearch,
                    onLoadMoreSearch = onLoadMoreSearch,
                    onLoadFollowing = onLoadFollowing,
                    onOpenCreator = onOpenCreator,
                    onLogin = onLogin,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                )
            } else {
                CreatorProfile(
                    state = state,
                    accountLoggedIn = accountLoggedIn,
                    resolvingBvid = resolvingBvid,
                    onToggleRelation = onToggleRelation,
                    onLoadMoreVideos = onLoadMoreVideos,
                    onPlay = onPlay,
                    onAddFavorite = onAddFavorite,
                    onLogin = onLogin,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                )
            }
        }
    }
}

@Composable
private fun CreatorDirectory(
    state: CreatorCenterUiState,
    accountLoggedIn: Boolean,
    onTabSelected: (CreatorCenterTab) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onLoadMoreSearch: () -> Unit,
    onLoadFollowing: (Boolean) -> Unit,
    onOpenCreator: (BilibiliCreator) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchKeyword by remember(state.searchKeyword) { mutableStateOf(state.searchKeyword) }
    var followingKeyword by remember { mutableStateOf("") }
    Column(modifier = modifier) {
        // long: 用户搜索走远端分页，关注关键词只筛选已加载页，两个入口分开表达，避免把局部结果误当成全局搜索。
        CreatorCenterTabs(
            selected = state.tab,
            onSelected = onTabSelected,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        val keyword = if (state.tab == CreatorCenterTab.SEARCH) searchKeyword else followingKeyword
        CompactSearchField(
            value = keyword,
            onValueChange = { value ->
                if (state.tab == CreatorCenterTab.SEARCH) {
                    searchKeyword = value
                    if (value.isBlank() && state.searchKeyword.isNotBlank()) onClearSearch()
                } else {
                    followingKeyword = value
                }
            },
            onSearch = {
                if (state.tab == CreatorCenterTab.SEARCH) onSearch(searchKeyword)
            },
            onClear = {
                if (state.tab == CreatorCenterTab.SEARCH) {
                    searchKeyword = ""
                    onClearSearch()
                } else {
                    followingKeyword = ""
                }
            },
            placeholder = if (state.tab == CreatorCenterTab.SEARCH) "按名称搜索 UP 主" else "筛选已加载的关注",
            loading = state.isListLoading && state.tab == CreatorCenterTab.SEARCH,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (state.isListLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        when (state.tab) {
            CreatorCenterTab.SEARCH -> SearchCreatorContent(
                state = state,
                onLoadMore = onLoadMoreSearch,
                onOpenCreator = onOpenCreator,
                modifier = Modifier.weight(1f),
            )
            CreatorCenterTab.FOLLOWING -> FollowingCreatorContent(
                state = state,
                accountLoggedIn = accountLoggedIn,
                keyword = followingKeyword,
                onLoadFollowing = onLoadFollowing,
                onOpenCreator = onOpenCreator,
                onLogin = onLogin,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SearchCreatorContent(
    state: CreatorCenterUiState,
    onLoadMore: () -> Unit,
    onOpenCreator: (BilibiliCreator) -> Unit,
    modifier: Modifier,
) {
    when {
        state.isListLoading -> Spacer(modifier)
        state.searchKeyword.isBlank() -> BiuEmptyState(
            icon = Icons.Rounded.PersonSearch,
            title = "搜索 UP 主",
            message = "输入名称后可查看空间、投稿和关注状态",
            modifier = modifier,
        )
        state.searchResults.isEmpty() -> BiuEmptyState(
            icon = Icons.Rounded.PersonSearch,
            title = "没有找到 UP 主",
            message = "换一个名称关键词再试试",
            modifier = modifier,
        )
        else -> CreatorList(
            creators = state.searchResults,
            loadingMore = state.isListLoadingMore,
            hasMore = state.searchNextPage != null,
            onLoadMore = onLoadMore,
            onOpenCreator = onOpenCreator,
            modifier = modifier,
        )
    }
}

@Composable
private fun FollowingCreatorContent(
    state: CreatorCenterUiState,
    accountLoggedIn: Boolean,
    keyword: String,
    onLoadFollowing: (Boolean) -> Unit,
    onOpenCreator: (BilibiliCreator) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier,
) {
    val visibleCreators = remember(state.followingCreators, keyword) {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) state.followingCreators else state.followingCreators.filter { creator ->
            creator.name.contains(normalized, ignoreCase = true)
        }
    }
    when {
        !accountLoggedIn -> BiuEmptyState(
            icon = Icons.Rounded.AccountCircle,
            title = "登录后查看关注",
            message = "登录 Bilibili 后可分页浏览和管理关注的 UP 主",
            actionLabel = "登录",
            onAction = onLogin,
            modifier = modifier,
        )
        state.isListLoading -> Spacer(modifier)
        state.followingCreators.isEmpty() -> BiuEmptyState(
            icon = Icons.Rounded.AccountCircle,
            title = "暂时没有关注内容",
            actionLabel = "重新加载",
            onAction = { onLoadFollowing(true) },
            modifier = modifier,
        )
        visibleCreators.isEmpty() -> BiuEmptyState(
            icon = Icons.Rounded.PersonSearch,
            title = "没有匹配的关注",
            message = "当前筛选只作用于已加载内容",
            modifier = modifier,
        )
        else -> CreatorList(
            creators = visibleCreators,
            loadingMore = state.isListLoadingMore,
            hasMore = state.followingNextPage != null && keyword.isBlank(),
            onLoadMore = { onLoadFollowing(false) },
            onOpenCreator = onOpenCreator,
            modifier = modifier,
        )
    }
}

@Composable
private fun CreatorCenterTabs(
    selected: CreatorCenterTab,
    onSelected: (CreatorCenterTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(modifier = Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            CreatorCenterTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }
                        .clickable { onSelected(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatorList(
    creators: List<BilibiliCreator>,
    loadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenCreator: (BilibiliCreator) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 2.dp)) {
        items(creators, key = BilibiliCreator::mid) { creator ->
            CreatorRow(creator = creator, onClick = { onOpenCreator(creator) })
            MediaDivider(start = 72.dp)
        }
        if (hasMore || loadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onLoadMore) { Text("加载更多") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorRow(creator: BilibiliCreator, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = creator.faceUrl,
            contentDescription = creator.name,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(creator.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Text(
                creatorMeta(creator),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (creator.signature.isNotBlank()) {
                Text(
                    creator.signature,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CreatorProfile(
    state: CreatorCenterUiState,
    accountLoggedIn: Boolean,
    resolvingBvid: String?,
    onToggleRelation: () -> Unit,
    onLoadMoreVideos: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    onAddFavorite: (BilibiliVideo) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val creator = state.selectedCreator ?: return
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = creator.faceUrl,
                contentDescription = creator.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(creator.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(
                    creatorMeta(creator),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (creator.officialTitle.isNotBlank()) {
                    Text(
                        creator.officialTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (accountLoggedIn) {
                Button(
                    onClick = onToggleRelation,
                    enabled = !state.isRelationMutating &&
                        state.relation != BilibiliCreatorRelation.BLOCKED,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (state.isRelationMutating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            when {
                                state.relation == BilibiliCreatorRelation.UNKNOWN -> "刷新状态"
                                state.relation.isFollowing -> "已关注"
                                else -> "关注"
                            },
                        )
                    }
                }
            } else {
                TextButton(onClick = onLogin) { Text("登录关注") }
            }
        }
        if (creator.signature.isNotBlank()) {
            Text(
                creator.signature,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "投稿",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        if (state.isProfileLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (!state.isProfileLoading && state.videos.isEmpty()) {
            BiuEmptyState(
                icon = Icons.Rounded.PersonSearch,
                title = "暂时没有投稿",
                modifier = Modifier.weight(1f),
            )
        } else {
            // long: 空间投稿复用推荐页的紧凑媒体行，让播放、收藏和解析中状态在两个入口保持一致。
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 2.dp)) {
                items(state.videos, key = BilibiliVideo::bvid) { video ->
                    VideoRow(
                        video = video,
                        resolving = resolvingBvid == video.bvid,
                        enabled = resolvingBvid == null,
                        onClick = { onPlay(video) },
                        onAddFavorite = { onAddFavorite(video) },
                    )
                    MediaDivider(start = 114.dp)
                }
                if (state.videosNextPage != null || state.isVideosLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.isVideosLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(onClick = onLoadMoreVideos) { Text("加载更多投稿") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun creatorMeta(creator: BilibiliCreator): String {
    val values = buildList {
        add("UID ${creator.mid}")
        creator.followerCount?.let { count -> add("${formatCompactCount(count)} 粉丝") }
        creator.videoCount?.let { count -> add("$count 投稿") }
    }
    return values.joinToString(" · ")
}

private fun formatCompactCount(count: Long): String = when {
    count >= 100_000_000L -> String.format(Locale.getDefault(), "%.1f亿", count / 100_000_000.0)
    count >= 10_000L -> String.format(Locale.getDefault(), "%.1f万", count / 10_000.0)
    else -> count.toString()
}
