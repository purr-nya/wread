package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookmarkEntity
import com.example.data.ChapterEntity
import com.example.model.WatchTheme

@Composable
fun ChapterDrawer(
    chapters: List<ChapterEntity>,
    bookmarks: List<BookmarkEntity>,
    currentChapterIndex: Int,
    watchTheme: WatchTheme,
    onSelectChapter: (Int) -> Unit,
    onSelectBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to current chapter when opening chapters tab
    LaunchedEffect(currentChapterIndex, selectedTab) {
        if (selectedTab == 0 && currentChapterIndex in chapters.indices) {
            val target = (currentChapterIndex - 2).coerceAtLeast(0)
            listState.scrollToItem(target)
        }
    }

    val filteredChapters = remember(chapters, searchQuery) {
        if (searchQuery.isBlank()) {
            chapters
        } else {
            chapters.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chapter_drawer"),
        color = watchTheme.backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTab == 0) "章节目录 (${chapters.size})" else "我的书签 (${bookmarks.size})",
                    color = watchTheme.textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭目录",
                        tint = watchTheme.secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tab bar for Chapters vs Bookmarks
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = watchTheme.cardBackground,
                contentColor = watchTheme.accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .height(36.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("目录", fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("书签", fontSize = 12.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (selectedTab == 0) {
                // Search filter (if many chapters)
                if (chapters.size > 10) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索章节...", fontSize = 11.sp, color = watchTheme.secondaryTextColor) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = watchTheme.secondaryTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = watchTheme.textColor,
                            unfocusedTextColor = watchTheme.textColor,
                            focusedBorderColor = watchTheme.accentColor,
                            unfocusedBorderColor = watchTheme.cardBackground,
                            focusedContainerColor = watchTheme.cardBackground,
                            unfocusedContainerColor = watchTheme.cardBackground
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(bottom = 6.dp)
                    )
                }

                // Chapters list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredChapters) { _, chapter ->
                        val isCurrent = (chapter.chapterIndex == currentChapterIndex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) watchTheme.accentColor.copy(alpha = 0.18f)
                                    else watchTheme.cardBackground
                                )
                                .clickable {
                                    onSelectChapter(chapter.chapterIndex)
                                    onClose()
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(watchTheme.accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = chapter.title,
                                    color = if (isCurrent) watchTheme.accentColor else watchTheme.textColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isCurrent) {
                                Text(
                                    text = "当前",
                                    color = watchTheme.accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                // Bookmarks list
                if (bookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无书签\n在阅读界面点击顶部书签图标即可添加",
                            color = watchTheme.secondaryTextColor,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(bookmarks.size) { idx ->
                            val bookmark = bookmarks[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(watchTheme.cardBackground)
                                    .clickable {
                                        onSelectBookmark(bookmark)
                                        onClose()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bookmark.chapterTitle,
                                        color = watchTheme.accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = bookmark.excerpt,
                                        color = watchTheme.textColor.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteBookmark(bookmark.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除书签",
                                        tint = watchTheme.secondaryTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
