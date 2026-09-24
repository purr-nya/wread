package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookEntity
import com.example.data.BookmarkEntity
import com.example.data.ChapterEntity
import com.example.model.EdgeTapScheme
import com.example.model.ReadingMode
import com.example.model.ScreenShape
import com.example.model.WatchSettings
import com.example.model.WatchTheme
import com.example.ui.components.ChapterDrawer
import com.example.ui.components.CircularBezelProgress
import com.example.ui.components.WatchStatusBar
import com.example.util.WatchHaptics
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    book: BookEntity,
    chapters: List<ChapterEntity>,
    bookmarks: List<BookmarkEntity>,
    currentChapterText: String,
    currentChapterIndex: Int,
    settings: WatchSettings,
    onBack: () -> Unit,
    onChapterChange: (Int) -> Unit,
    onUpdateProgress: (chapterIndex: Int, progressPercent: Float, scrollOffset: Int) -> Unit,
    onAddBookmark: (chapterTitle: String, excerpt: String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onSettingsChanged: (WatchSettings) -> Unit,
    keyFlipTrigger: Int = 0 // increments on physical key press
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme = settings.watchTheme

    var showHud by remember { mutableStateOf(false) }
    var showChapterDrawer by remember { mutableStateOf(false) }
    var bookmarkToast by remember { mutableStateOf(false) }

    val currentChapter = chapters.getOrNull(currentChapterIndex)
    val chapterTitle = currentChapter?.title ?: "正文"

    // Pagination calculation
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentPageIndex by remember { mutableIntStateOf(0) }

    val scrollState = rememberLazyListState()

    // Recalculate pages when text, font size or line spacing changes
    LaunchedEffect(currentChapterText, settings.fontSizeSp, settings.screenShape) {
        pages = paginateContent(
            text = currentChapterText,
            fontSizeSp = settings.fontSizeSp,
            screenShape = settings.screenShape
        )
        if (currentPageIndex >= pages.size) {
            currentPageIndex = (pages.size - 1).coerceAtLeast(0)
        }
    }

    // Reset scroll to top when chapter changes
    LaunchedEffect(currentChapterIndex) {
        scrollState.scrollToItem(0, 0)
        currentPageIndex = 0
    }

    // Handle physical key flips
    LaunchedEffect(keyFlipTrigger) {
        if (keyFlipTrigger != 0) {
            if (settings.readingMode == ReadingMode.VERTICAL_SCROLL) {
                val scrollDelta = 420f
                if (keyFlipTrigger > 0) {
                    scrollState.animateScrollBy(scrollDelta)
                } else {
                    scrollState.animateScrollBy(-scrollDelta)
                }
                if (settings.vibrateOnFlip) WatchHaptics.tick(context)
            } else {
                if (keyFlipTrigger > 0) {
                    // Next page
                    flipNextPage(
                        pages = pages,
                        currentPageIndex = currentPageIndex,
                        currentChapterIndex = currentChapterIndex,
                        totalChapters = chapters.size,
                        settings = settings,
                        context = context,
                        onSetPage = { currentPageIndex = it },
                        onChapterChange = onChapterChange
                    )
                } else {
                    // Prev page
                    flipPrevPage(
                        pages = pages,
                        currentPageIndex = currentPageIndex,
                        currentChapterIndex = currentChapterIndex,
                        settings = settings,
                        context = context,
                        onSetPage = { currentPageIndex = it },
                        onChapterChange = onChapterChange
                    )
                }
            }
        }
    }

    // Save reading progress
    val scrollProgressPercent by remember(currentChapterIndex, chapters.size) {
        derivedStateOf {
            if (chapters.isEmpty()) 0f
            else {
                val totalItems = scrollState.layoutInfo.totalItemsCount
                val visibleIndex = scrollState.firstVisibleItemIndex
                val innerFraction = if (totalItems > 1) {
                    (visibleIndex.toFloat() / (totalItems - 1).toFloat()).coerceIn(0f, 1f)
                } else 0f
                val chapterWeight = 100f / chapters.size
                val pct = (currentChapterIndex * chapterWeight) + (innerFraction * chapterWeight)
                pct.coerceIn(0f, 100f)
            }
        }
    }

    val pagedProgressPercent by remember(currentChapterIndex, chapters.size, currentPageIndex, pages.size) {
        derivedStateOf {
            if (chapters.isEmpty()) 0f
            else {
                val chapterWeight = 100f / chapters.size
                val innerFraction = if (pages.isNotEmpty()) currentPageIndex.toFloat() / pages.size else 0f
                val pct = (currentChapterIndex * chapterWeight) + (innerFraction * chapterWeight)
                pct.coerceIn(0f, 100f)
            }
        }
    }

    val overallProgressPercent = if (settings.readingMode == ReadingMode.VERTICAL_SCROLL) {
        scrollProgressPercent
    } else {
        pagedProgressPercent
    }

    LaunchedEffect(overallProgressPercent, currentChapterIndex, currentPageIndex) {
        onUpdateProgress(currentChapterIndex, overallProgressPercent, currentPageIndex)
    }

    // Insets based on watch screen shape
    val horizontalPadding = when (settings.screenShape) {
        ScreenShape.ROUND -> 22.dp
        ScreenShape.ROUNDED_SQUARE -> 14.dp
        ScreenShape.SQUARE -> 8.dp
    }
    val verticalPadding = when (settings.screenShape) {
        ScreenShape.ROUND -> 20.dp
        ScreenShape.ROUNDED_SQUARE -> 12.dp
        ScreenShape.SQUARE -> 6.dp
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reader_screen"),
        color = theme.backgroundColor
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            // Circular Bezel Progress Ring if Round Screen
            if (settings.screenShape == ScreenShape.ROUND) {
                CircularBezelProgress(
                    progress = overallProgressPercent / 100f,
                    accentColor = theme.accentColor,
                    trackColor = theme.textColor.copy(alpha = 0.12f),
                    strokeWidthDp = 3f
                )
            }

            // Main Reading Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            ) {
                // Top Status Bar (Time, Battery, Truncated Title)
                if (settings.showStatusBar) {
                    WatchStatusBar(
                        textColor = theme.secondaryTextColor,
                        centerTitle = chapterTitle
                    )
                }

                // Reading Area: Paged Mode or Vertical Scroll Mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (settings.readingMode == ReadingMode.PAGED) {
                        // Paged mode: Single page of text, clean and zero-jitter
                        val pageContent = pages.getOrElse(currentPageIndex) {
                            if (currentChapterText.isBlank()) "加载中..." else currentChapterText
                        }

                        Text(
                            text = pageContent,
                            color = theme.textColor,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("reader_page_text")
                        )

                        // Touch Zone Overlays for Paged mode
                        if (settings.edgeTapScheme == EdgeTapScheme.TOP_BOTTOM) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Top zone (32%): Prev Page
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.32f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            flipPrevPage(
                                                pages = pages,
                                                currentPageIndex = currentPageIndex,
                                                currentChapterIndex = currentChapterIndex,
                                                settings = settings,
                                                context = context,
                                                onSetPage = { currentPageIndex = it },
                                                onChapterChange = onChapterChange
                                            )
                                        }
                                )
                                // Center zone (36%): Toggle HUD
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.36f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            showHud = !showHud
                                        }
                                )
                                // Bottom zone (32%): Next Page
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.32f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            flipNextPage(
                                                pages = pages,
                                                currentPageIndex = currentPageIndex,
                                                currentChapterIndex = currentChapterIndex,
                                                totalChapters = chapters.size,
                                                settings = settings,
                                                context = context,
                                                onSetPage = { currentPageIndex = it },
                                                onChapterChange = onChapterChange
                                            )
                                        }
                                )
                            }
                        } else {
                            // Left / Right tap zones
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Left 30%: Prev
                                Box(
                                    modifier = Modifier
                                        .weight(0.30f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            flipPrevPage(
                                                pages = pages,
                                                currentPageIndex = currentPageIndex,
                                                currentChapterIndex = currentChapterIndex,
                                                settings = settings,
                                                context = context,
                                                onSetPage = { currentPageIndex = it },
                                                onChapterChange = onChapterChange
                                            )
                                        }
                                )
                                // Center 40%: HUD
                                Box(
                                    modifier = Modifier
                                        .weight(0.40f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            showHud = !showHud
                                        }
                                )
                                // Right 30%: Next
                                Box(
                                    modifier = Modifier
                                        .weight(0.30f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            flipNextPage(
                                                pages = pages,
                                                currentPageIndex = currentPageIndex,
                                                currentChapterIndex = currentChapterIndex,
                                                totalChapters = chapters.size,
                                                settings = settings,
                                                context = context,
                                                onSetPage = { currentPageIndex = it },
                                                onChapterChange = onChapterChange
                                            )
                                        }
                                )
                            }
                        }
                    } else {
                        // Webtoon-style continuous vertical scrolling
                        val paragraphs = remember(currentChapterText) {
                            currentChapterText.split("\n\n").filter { it.isNotBlank() }
                        }

                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            val h = size.height
                                            val y = offset.y
                                            if (y < h * 0.22f) {
                                                // Tap top 22%: smooth scroll up
                                                scope.launch {
                                                    scrollState.animateScrollBy(-420f)
                                                }
                                                if (settings.vibrateOnFlip) WatchHaptics.tick(context)
                                            } else if (y > h * 0.78f) {
                                                // Tap bottom 22%: smooth scroll down
                                                scope.launch {
                                                    scrollState.animateScrollBy(420f)
                                                }
                                                if (settings.vibrateOnFlip) WatchHaptics.tick(context)
                                            } else {
                                                // Center area tap: toggle HUD
                                                showHud = !showHud
                                            }
                                        }
                                    )
                                }
                                .testTag("reader_vertical_scroll")
                        ) {
                            // Chapter header banner
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(theme.accentColor.copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "第 ${currentChapterIndex + 1} 话",
                                            color = theme.accentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = chapterTitle,
                                        color = theme.textColor,
                                        fontSize = (settings.fontSizeSp + 2).sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(44.dp)
                                            .height(2.dp)
                                            .background(theme.accentColor.copy(alpha = 0.35f))
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }

                            // Continuous paragraphs
                            itemsIndexed(paragraphs) { idx, para ->
                                Text(
                                    text = para,
                                    color = theme.textColor,
                                    fontSize = settings.fontSizeSp.sp,
                                    lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }

                            // Webtoon chapter end card
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(1.dp)
                                            .background(theme.secondaryTextColor.copy(alpha = 0.3f))
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "—— 本话阅读完毕 ——",
                                        color = theme.secondaryTextColor,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (currentChapterIndex + 1 < chapters.size) {
                                        val nextChapter = chapters[currentChapterIndex + 1]
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    onChapterChange(currentChapterIndex + 1)
                                                    if (settings.vibrateOnFlip) WatchHaptics.doubleTick(context)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "进入下一话 ➔",
                                                        color = theme.accentColor,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = nextChapter.title,
                                                        color = theme.textColor,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = theme.accentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "🎉 已读完本书全部章节",
                                            color = theme.accentColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FilledTonalButton(
                                            onClick = onBack,
                                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = theme.cardBackground),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("返回书架", color = theme.textColor, fontSize = 11.sp)
                                        }
                                    }

                                    if (currentChapterIndex > 0) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "返回上一话",
                                            color = theme.secondaryTextColor,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    onChapterChange(currentChapterIndex - 1)
                                                    if (settings.vibrateOnFlip) WatchHaptics.tick(context)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Page / Progress info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pageText = if (settings.readingMode == ReadingMode.PAGED) {
                        "页 ${currentPageIndex + 1}/${pages.size.coerceAtLeast(1)}"
                    } else {
                        "第 ${currentChapterIndex + 1}/${chapters.size.coerceAtLeast(1)} 话"
                    }
                    Text(
                        text = pageText,
                        color = theme.secondaryTextColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "${overallProgressPercent.toInt()}%",
                        color = theme.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // HUD Overlay (Watch-friendly quick action sheet)
            AnimatedVisibility(
                visible = showHud,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("reader_hud_overlay"),
                    color = theme.backgroundColor.copy(alpha = 0.94f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalPadding, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Bar: Back, Chapter title, Bookmark
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(theme.cardBackground)
                                    .testTag("hud_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回书架",
                                    tint = theme.textColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = chapterTitle,
                                color = theme.textColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                textAlign = TextAlign.Center
                            )

                            IconButton(
                                onClick = {
                                    val snippet = pages.getOrNull(currentPageIndex)?.take(40) ?: chapterTitle
                                    onAddBookmark(chapterTitle, snippet)
                                    bookmarkToast = true
                                    if (settings.vibrateOnFlip) WatchHaptics.tick(context)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(theme.cardBackground)
                                    .testTag("hud_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "添加书签",
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Middle Floating Quick Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.cardBackground)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Row 1: Font Size +/- and Theme cycle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            if (settings.fontSizeSp > 12) {
                                                onSettingsChanged(settings.copy(fontSizeSp = settings.fontSizeSp - 1))
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(width = 38.dp, height = 32.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text("A-", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Text(
                                        text = "${settings.fontSizeSp}",
                                        color = theme.accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )

                                    FilledTonalButton(
                                        onClick = {
                                            if (settings.fontSizeSp < 28) {
                                                onSettingsChanged(settings.copy(fontSizeSp = settings.fontSizeSp + 1))
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(width = 38.dp, height = 32.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text("A+", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Quick theme cycle button
                                FilledTonalButton(
                                    onClick = {
                                        val themes = WatchTheme.entries
                                        val nextIndex = (themes.indexOf(settings.watchTheme) + 1) % themes.size
                                        onSettingsChanged(settings.copy(watchTheme = themes[nextIndex]))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(settings.watchTheme.displayName.take(2), fontSize = 10.sp, color = theme.textColor)
                                }
                            }

                            // Row 2: Mode & Screen Shape Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Paged vs Scroll
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(theme.backgroundColor)
                                        .clickable {
                                            val nextMode = if (settings.readingMode == ReadingMode.PAGED) ReadingMode.VERTICAL_SCROLL else ReadingMode.PAGED
                                            onSettingsChanged(settings.copy(readingMode = nextMode))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (settings.readingMode == ReadingMode.PAGED) "翻页模式" else "滚动模式",
                                        color = theme.accentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Round vs Square
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(theme.backgroundColor)
                                        .clickable {
                                            val nextShape = when (settings.screenShape) {
                                                ScreenShape.ROUND -> ScreenShape.ROUNDED_SQUARE
                                                ScreenShape.ROUNDED_SQUARE -> ScreenShape.SQUARE
                                                ScreenShape.SQUARE -> ScreenShape.ROUND
                                            }
                                            onSettingsChanged(settings.copy(screenShape = nextShape))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = settings.screenShape.displayName,
                                        color = theme.textColor,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Bottom Bar: Chapter jump & TOC drawer button
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.cardBackground)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (chapters.size > 1) {
                                var sliderValue by remember(currentChapterIndex) { mutableFloatStateOf(currentChapterIndex.toFloat()) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${sliderValue.toInt() + 1}/${chapters.size}",
                                        color = theme.secondaryTextColor,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Slider(
                                        value = sliderValue,
                                        onValueChange = { sliderValue = it },
                                        onValueChangeFinished = { onChapterChange(sliderValue.toInt()) },
                                        valueRange = 0f..(chapters.size - 1).toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = theme.accentColor,
                                            activeTrackColor = theme.accentColor,
                                            inactiveTrackColor = theme.textColor.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentChapterIndex > 0) {
                                            onChapterChange(currentChapterIndex - 1)
                                            if (settings.vibrateOnFlip) WatchHaptics.doubleTick(context)
                                        }
                                    },
                                    enabled = currentChapterIndex > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SkipPrevious,
                                        contentDescription = "上一章",
                                        tint = if (currentChapterIndex > 0) theme.textColor else theme.secondaryTextColor.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        showChapterDrawer = true
                                        showHud = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = theme.backgroundColor),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("open_toc_button")
                                ) {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("目录与书签", color = theme.textColor, fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = {
                                        if (currentChapterIndex + 1 < chapters.size) {
                                            onChapterChange(currentChapterIndex + 1)
                                            if (settings.vibrateOnFlip) WatchHaptics.doubleTick(context)
                                        }
                                    },
                                    enabled = currentChapterIndex + 1 < chapters.size,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = "下一章",
                                        tint = if (currentChapterIndex + 1 < chapters.size) theme.textColor else theme.secondaryTextColor.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Close HUD hint / click
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHud = false }
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "点击此处返回阅读",
                                color = theme.secondaryTextColor,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Bookmark Saved Flash Toast
            if (bookmarkToast) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1200)
                    bookmarkToast = false
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("书签已保存", color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Chapter Drawer Sheet
            if (showChapterDrawer) {
                ChapterDrawer(
                    chapters = chapters,
                    bookmarks = bookmarks,
                    currentChapterIndex = currentChapterIndex,
                    watchTheme = theme,
                    onSelectChapter = { idx ->
                        onChapterChange(idx)
                        currentPageIndex = 0
                    },
                    onSelectBookmark = { bm ->
                        onChapterChange(bm.chapterIndex)
                        currentPageIndex = 0
                    },
                    onDeleteBookmark = onDeleteBookmark,
                    onClose = { showChapterDrawer = false }
                )
            }
        }
    }
}

// Helpers for paging
private fun flipNextPage(
    pages: List<String>,
    currentPageIndex: Int,
    currentChapterIndex: Int,
    totalChapters: Int,
    settings: WatchSettings,
    context: android.content.Context,
    onSetPage: (Int) -> Unit,
    onChapterChange: (Int) -> Unit
) {
    if (currentPageIndex + 1 < pages.size) {
        onSetPage(currentPageIndex + 1)
        if (settings.vibrateOnFlip) WatchHaptics.tick(context)
    } else if (currentChapterIndex + 1 < totalChapters) {
        // Automatically go to next chapter
        onChapterChange(currentChapterIndex + 1)
        onSetPage(0)
        if (settings.vibrateOnFlip) WatchHaptics.doubleTick(context)
    }
}

private fun flipPrevPage(
    pages: List<String>,
    currentPageIndex: Int,
    currentChapterIndex: Int,
    settings: WatchSettings,
    context: android.content.Context,
    onSetPage: (Int) -> Unit,
    onChapterChange: (Int) -> Unit
) {
    if (currentPageIndex > 0) {
        onSetPage(currentPageIndex - 1)
        if (settings.vibrateOnFlip) WatchHaptics.tick(context)
    } else if (currentChapterIndex > 0) {
        // Go to previous chapter
        onChapterChange(currentChapterIndex - 1)
        onSetPage(0)
        if (settings.vibrateOnFlip) WatchHaptics.doubleTick(context)
    }
}

// Fast pagination algorithm customized for watch viewports
private fun paginateContent(
    text: String,
    fontSizeSp: Int,
    screenShape: ScreenShape
): List<String> {
    if (text.isBlank()) return listOf("")

    // Approximate characters per page based on font size and watch shape:
    // On a 1.4-1.8 inch watch screen:
    // at 14sp: ~180-220 chars per page
    // at 16sp: ~140-160 chars per page
    // at 20sp: ~90-110 chars per page
    // Round screens have slightly less usable area
    val baseCapacity = when {
        fontSizeSp <= 13 -> 240
        fontSizeSp <= 15 -> 180
        fontSizeSp <= 17 -> 140
        fontSizeSp <= 20 -> 100
        fontSizeSp <= 24 -> 75
        else -> 55
    }

    val pageCapacity = if (screenShape == ScreenShape.ROUND) (baseCapacity * 0.82f).toInt() else baseCapacity

    val pages = mutableListOf<String>()
    val paragraphs = text.split("\n\n")

    val currentPage = StringBuilder()
    var currentChars = 0

    for (para in paragraphs) {
        val trimmed = para.trim()
        if (trimmed.isEmpty()) continue

        // If paragraph alone is longer than page capacity, split it
        if (trimmed.length > pageCapacity) {
            var start = 0
            while (start < trimmed.length) {
                val remaining = trimmed.length - start
                val sliceLen = minOf(pageCapacity, remaining)
                var end = start + sliceLen

                // Try to break at punctuation if possible
                if (end < trimmed.length) {
                    val searchWindow = trimmed.substring(start, end)
                    val lastPunct = searchWindow.lastIndexOfAny(charArrayOf('。', '！', '？', '；', '，', '.', '!', '?', ',', '\n'))
                    if (lastPunct > sliceLen * 0.6) {
                        end = start + lastPunct + 1
                    }
                }

                val slice = trimmed.substring(start, end)
                if (currentPage.isNotEmpty()) {
                    pages.add(currentPage.toString())
                    currentPage.clear()
                    currentChars = 0
                }
                pages.add(slice)
                start = end
            }
        } else {
            // Check if paragraph fits into current page
            val paraWeight = trimmed.length + 15 // paragraph break counts towards vertical space
            if (currentChars + paraWeight > pageCapacity && currentPage.isNotEmpty()) {
                pages.add(currentPage.toString())
                currentPage.clear()
                currentChars = 0
            }

            if (currentPage.isNotEmpty()) {
                currentPage.append("\n\n")
            }
            currentPage.append(trimmed)
            currentChars += paraWeight
        }
    }

    if (currentPage.isNotEmpty()) {
        pages.add(currentPage.toString())
    }

    return if (pages.isEmpty()) listOf(text) else pages
}
