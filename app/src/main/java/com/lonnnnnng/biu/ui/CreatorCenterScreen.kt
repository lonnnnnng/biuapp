package com.lonnnnnng.biu.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.lonnnnnng.biu.data.local.CreatorGroupEntity
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreatorCenterScreen(
    state: CreatorCenterUiState,
    selectedCreators: List<BilibiliCreator>,
    groups: List<CreatorGroupEntity>,
    groupMembers: Map<Long, Set<Long>>,
    accountLoggedIn: Boolean,
    resolvingBvid: String?,
    onBack: () -> Unit,
    onTabSelected: (CreatorCenterTab) -> Unit,
    onGroupSelected: (Long?) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onLoadMoreSearch: () -> Unit,
    onLoadFollowing: (Boolean) -> Unit,
    onOpenCreator: (BilibiliCreator) -> Unit,
    onCloseCreator: () -> Unit,
    onOpenHomeScope: () -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (CreatorGroupEntity, String) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onToggleCreatorGroup: (Long, Long) -> Unit,
    onToggleRelation: () -> Unit,
    onLoadMoreVideos: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    onAddFavorite: (BilibiliVideo) -> Unit,
    onLogin: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showGroupManager by remember { mutableStateOf(false) }
    if (showGroupManager) {
        CreatorGroupManagerDialog(
            groups = groups,
            memberCounts = groupMembers.mapValues { (_, members) -> members.size },
            onDismiss = { showGroupManager = false },
            onCreate = onCreateGroup,
            onRename = onRenameGroup,
            onDelete = onDeleteGroup,
        )
    }
    BackHandler {
        if (state.selectedCreator != null) onCloseCreator() else onBack()
    }
    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = sheetState,
        modifier = Modifier.widthIn(max = 840.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        // long: UP 主搜索、关注列表和空间详情共用同一个全高弹层，层级切换只改变标题与返回动作，避免跳出首页视觉体系。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            BiuSheetHeader(
                title = state.selectedCreator?.name ?: "管理音乐来源",
                onClose = onBack,
                navigationIcon = if (state.selectedCreator == null) null else Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "返回 UP 主列表",
                onNavigation = onCloseCreator,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (state.selectedCreator == null) {
                    CreatorDirectory(
                        state = state,
                        selectedCreators = selectedCreators,
                        groups = groups,
                        groupMembers = groupMembers,
                        accountLoggedIn = accountLoggedIn,
                        onTabSelected = onTabSelected,
                        onGroupSelected = onGroupSelected,
                        onSearch = onSearch,
                        onClearSearch = onClearSearch,
                        onLoadMoreSearch = onLoadMoreSearch,
                        onLoadFollowing = onLoadFollowing,
                        onOpenHomeScope = onOpenHomeScope,
                        onManageGroups = { showGroupManager = true },
                        onOpenCreator = onOpenCreator,
                        onLogin = onLogin,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp),
                    )
                } else {
                    CreatorProfile(
                        state = state,
                        groups = groups,
                        groupMembers = groupMembers,
                        accountLoggedIn = accountLoggedIn,
                        resolvingBvid = resolvingBvid,
                        onToggleRelation = onToggleRelation,
                        onToggleCreatorGroup = onToggleCreatorGroup,
                        onManageGroups = { showGroupManager = true },
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
}

@Composable
private fun CreatorDirectory(
    state: CreatorCenterUiState,
    selectedCreators: List<BilibiliCreator>,
    groups: List<CreatorGroupEntity>,
    groupMembers: Map<Long, Set<Long>>,
    accountLoggedIn: Boolean,
    onTabSelected: (CreatorCenterTab) -> Unit,
    onGroupSelected: (Long?) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onLoadMoreSearch: () -> Unit,
    onLoadFollowing: (Boolean) -> Unit,
    onOpenHomeScope: () -> Unit,
    onManageGroups: () -> Unit,
    onOpenCreator: (BilibiliCreator) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchKeyword by remember(state.searchKeyword) { mutableStateOf(state.searchKeyword) }
    var followingKeyword by remember { mutableStateOf("") }
    Column(modifier = modifier) {
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
            placeholder = when (state.tab) {
                CreatorCenterTab.SEARCH -> "按名称搜索 UP 主"
                CreatorCenterTab.FOLLOWING -> "筛选已加载的关注"
                CreatorCenterTab.HOME_SELECTED -> "筛选首页已选 UP 主"
            },
            loading = state.isListLoading && state.tab == CreatorCenterTab.SEARCH,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        // long: 搜索框保持首要输入位置，用户搜索与我的关注作为同层内容 Tab 放在其下方，并与推荐、账号页共用下划线选中样式。
        CreatorCenterTabs(
            selected = state.tab,
            onSelected = onTabSelected,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "首页来源",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenHomeScope) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("管理范围")
            }
            TextButton(onClick = onManageGroups) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (groups.isEmpty()) "新建分组" else "管理分组")
            }
        }
        if (groups.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = state.selectedGroupId == null,
                    onClick = { onGroupSelected(null) },
                    label = { Text("全部") },
                )
                groups.forEach { group ->
                    FilterChip(
                        selected = state.selectedGroupId == group.groupId,
                        onClick = { onGroupSelected(group.groupId) },
                        label = { Text(group.name, maxLines = 1) },
                    )
                }
            }
        }
        if (state.isListLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        val selectedGroupMids = state.selectedGroupId?.let(groupMembers::get)
        val filteredSearchResults = selectedGroupMids?.let { mids ->
            state.searchResults.filter { it.mid in mids }
        } ?: state.searchResults
        val filteredFollowingCreators = selectedGroupMids?.let { mids ->
            state.followingCreators.filter { it.mid in mids }
        } ?: state.followingCreators
        val filteredSelectedCreators = selectedGroupMids?.let { mids ->
            selectedCreators.filter { it.mid in mids }
        } ?: selectedCreators
        when (state.tab) {
            CreatorCenterTab.SEARCH -> SearchCreatorContent(
                state = state.copy(searchResults = filteredSearchResults),
                onLoadMore = onLoadMoreSearch,
                onOpenCreator = onOpenCreator,
                modifier = Modifier.weight(1f),
            )
            CreatorCenterTab.FOLLOWING -> FollowingCreatorContent(
                state = state.copy(followingCreators = filteredFollowingCreators),
                accountLoggedIn = accountLoggedIn,
                keyword = followingKeyword,
                onLoadFollowing = onLoadFollowing,
                onOpenCreator = onOpenCreator,
                onLogin = onLogin,
                modifier = Modifier.weight(1f),
            )
            CreatorCenterTab.HOME_SELECTED -> SelectedCreatorContent(
                creators = filteredSelectedCreators,
                keyword = followingKeyword,
                onOpenCreator = onOpenCreator,
                onOpenHomeScope = onOpenHomeScope,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SelectedCreatorContent(
    creators: List<BilibiliCreator>,
    keyword: String,
    onOpenCreator: (BilibiliCreator) -> Unit,
    onOpenHomeScope: () -> Unit,
    modifier: Modifier,
) {
    val visibleCreators = remember(creators, keyword) {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) creators else creators.filter { creator ->
            creator.name.contains(normalized, ignoreCase = true)
        }
    }
    when {
        creators.isEmpty() -> BiuEmptyState(
            icon = Icons.Rounded.Tune,
            title = "首页使用默认热门",
            message = "选择 UP 主后，首页会按发布时间展示他们的投稿。",
            actionLabel = "选择首页来源",
            onAction = onOpenHomeScope,
            modifier = modifier,
        )
        visibleCreators.isEmpty() -> BiuEmptyState(
            icon = Icons.Rounded.PersonSearch,
            title = "没有匹配的首页来源",
            modifier = modifier,
        )
        else -> CreatorList(
            creators = visibleCreators,
            loadingMore = false,
            hasMore = false,
            onLoadMore = {},
            onOpenCreator = onOpenCreator,
            modifier = modifier,
        )
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
private fun CreatorGroupManagerDialog(
    groups: List<CreatorGroupEntity>,
    memberCounts: Map<Long, Int>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (CreatorGroupEntity, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editingGroup by remember { mutableStateOf<CreatorGroupEntity?>(null) }
    var groupName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<CreatorGroupEntity?>(null) }

    pendingDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除分组？") },
            text = { Text("将删除“${group.name}”及其本地分组关系，不会取消关注或移除首页来源。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(group.groupId)
                    },
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理 UP 主分组") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (editingGroup == null) "新分组名称" else "修改分组名称") },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    editingGroup?.let {
                        TextButton(
                            onClick = {
                                editingGroup = null
                                groupName = ""
                            },
                        ) { Text("取消编辑") }
                    }
                    Button(
                        onClick = {
                            val normalized = groupName.trim()
                            val group = editingGroup
                            if (group == null) onCreate(normalized) else onRename(group, normalized)
                            editingGroup = null
                            groupName = ""
                        },
                        enabled = groupName.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(if (editingGroup == null) "新建" else "保存")
                    }
                }
                if (groups.isEmpty()) {
                    Text(
                        "创建分组后，可在 UP 主详情中加入翻唱、现场、纯音乐等分类。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    groups.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${memberCounts[group.groupId] ?: 0} 位 UP 主",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingGroup = group
                                    groupName = group.name
                                },
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "重命名${group.name}")
                            }
                            IconButton(onClick = { pendingDelete = group }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除${group.name}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun CreatorCenterTabs(
    selected: CreatorCenterTab,
    onSelected: (CreatorCenterTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        CreatorCenterTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    }
                    .clickable { onSelected(tab) },
            ) {
                Text(
                    tab.label,
                    modifier = Modifier.align(Alignment.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
    groups: List<CreatorGroupEntity>,
    groupMembers: Map<Long, Set<Long>>,
    accountLoggedIn: Boolean,
    resolvingBvid: String?,
    onToggleRelation: () -> Unit,
    onToggleCreatorGroup: (Long, Long) -> Unit,
    onManageGroups: () -> Unit,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            groups.forEach { group ->
                val selected = creator.mid in groupMembers[group.groupId].orEmpty()
                FilterChip(
                    selected = selected,
                    onClick = { onToggleCreatorGroup(group.groupId, creator.mid) },
                    label = { Text(group.name, maxLines = 1) },
                )
            }
            TextButton(onClick = onManageGroups) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (groups.isEmpty()) "新建分组" else "管理分组")
            }
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
