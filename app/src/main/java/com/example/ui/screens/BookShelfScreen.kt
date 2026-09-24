package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookEntity
import com.example.model.ScreenShape
import com.example.model.WatchSettings
import com.example.ui.components.CircularBezelProgress
import com.example.ui.components.SettingsSheet
import com.example.ui.components.TextImportDialog
import com.example.ui.components.WatchStatusBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookShelfScreen(
    books: List<BookEntity>,
    settings: WatchSettings,
    onOpenBook: (Long) -> Unit,
    onImportUri: (Uri) -> Unit,
    onImportText: (title: String, content: String) -> Unit,
    onDeleteBook: (BookEntity) -> Unit,
    onResetSamples: () -> Unit,
    onSettingsChanged: (WatchSettings) -> Unit
) {
    val theme = settings.watchTheme
    var showSettings by remember { mutableStateOf(false) }
    var showTextImport by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    // File picker for .txt and .epub
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportUri(it) }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Circular bezel accent ring if round watch
            if (settings.screenShape == ScreenShape.ROUND) {
                CircularBezelProgress(
                    progress = 1f,
                    accentColor = theme.accentColor.copy(alpha = 0.35f),
                    trackColor = Color.Transparent,
                    strokeWidthDp = 2f
                )
            }

            val horizontalMargin = when (settings.screenShape) {
                ScreenShape.ROUND -> 22.dp
                ScreenShape.ROUNDED_SQUARE -> 14.dp
                ScreenShape.SQUARE -> 10.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalMargin)
            ) {
                // Watch status bar
                WatchStatusBar(
                    textColor = theme.textColor,
                    centerTitle = "腕上阅读"
                )

                // Quick Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "text/plain",
                                    "application/epub+zip",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = theme.cardBackground),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("import_file_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("导入", color = theme.textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    FilledTonalButton(
                        onClick = { showTextImport = true },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = theme.cardBackground),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("paste_text_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("粘贴", color = theme.textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.cardBackground)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = theme.secondaryTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Books list
                if (books.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = theme.secondaryTextColor,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "书架空空如也",
                                color = theme.textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "点击上方导入或恢复示例小说",
                                color = theme.secondaryTextColor,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = onResetSamples,
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = theme.cardBackground),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("加载预置书籍", color = theme.textColor, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(books, key = { it.id }) { book ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { onOpenBook(book.id) },
                                        onLongClick = { bookToDelete = book }
                                    )
                                    .testTag("book_card_${book.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = book.title,
                                            color = theme.textColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Format tag capsule
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (book.format == "EPUB") theme.accentColor.copy(alpha = 0.2f)
                                                    else Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = book.format,
                                                color = if (book.format == "EPUB") theme.accentColor else Color(0xFF60A5FA),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Progress bar
                                    val progressFraction = (book.currentProgressPercent / 100f).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        color = theme.accentColor,
                                        trackColor = theme.textColor.copy(alpha = 0.15f),
                                        strokeCap = StrokeCap.Round,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val chapterText = if (book.totalChapters > 0) {
                                            "第 ${book.currentChapterIndex + 1}/${book.totalChapters} 话"
                                        } else "未分章"

                                        Text(
                                            text = chapterText,
                                            color = theme.secondaryTextColor,
                                            fontSize = 10.sp
                                        )

                                        Text(
                                            text = "${book.currentProgressPercent.toInt()}%",
                                            color = theme.accentColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom padding for round screen ergonomics
                        item {
                            Spacer(modifier = Modifier.height(if (settings.screenShape == ScreenShape.ROUND) 24.dp else 12.dp))
                        }
                    }
                }
            }

            // Overlay Sheets
            if (showSettings) {
                SettingsSheet(
                    settings = settings,
                    onSettingsChanged = onSettingsChanged,
                    onClose = { showSettings = false }
                )
            }

            if (showTextImport) {
                TextImportDialog(
                    watchTheme = theme,
                    onConfirm = { title, content ->
                        onImportText(title, content)
                        showTextImport = false
                    },
                    onDismiss = { showTextImport = false }
                )
            }

            // Delete Book Confirmation
            bookToDelete?.let { book ->
                AlertDialog(
                    onDismissRequest = { bookToDelete = null },
                    title = { Text("管理书籍", fontSize = 13.sp, color = theme.textColor) },
                    text = {
                        Text(
                            "确定要从书架删除《${book.title}》吗？",
                            fontSize = 11.sp,
                            color = theme.secondaryTextColor
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeleteBook(book)
                                bookToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("删除", color = Color.White, fontSize = 11.sp)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { bookToDelete = null },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("取消", color = theme.textColor, fontSize = 11.sp)
                        }
                    },
                    containerColor = theme.cardBackground
                )
            }
        }
    }
}
