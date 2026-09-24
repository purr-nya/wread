package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.screens.BookShelfScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.WristReaderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val books by viewModel.books.collectAsState()
            val activeBook by viewModel.activeBook.collectAsState()
            val chapters by viewModel.chapters.collectAsState()
            val bookmarks by viewModel.bookmarks.collectAsState()
            val currentChapterContent by viewModel.currentChapterContent.collectAsState()
            val currentChapterIndex by viewModel.currentChapterIndex.collectAsState()
            val keyFlipTrigger by viewModel.keyFlipTrigger.collectAsState()

            // Keep screen on when reading if enabled
            LaunchedEffect(activeBook, settings.keepScreenOn) {
                if (activeBook != null && settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            WristReaderTheme(watchTheme = settings.watchTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(settings.watchTheme.backgroundColor)
                ) {
                    if (activeBook != null) {
                        BackHandler {
                            viewModel.closeBook()
                        }
                        ReaderScreen(
                            book = activeBook!!,
                            chapters = chapters,
                            bookmarks = bookmarks,
                            currentChapterText = currentChapterContent,
                            currentChapterIndex = currentChapterIndex,
                            settings = settings,
                            onBack = { viewModel.closeBook() },
                            onChapterChange = { viewModel.selectChapter(it) },
                            onUpdateProgress = { idx, pct, offset ->
                                viewModel.updateProgress(idx, pct, offset)
                            },
                            onAddBookmark = { title, excerpt ->
                                viewModel.addBookmark(title, excerpt)
                            },
                            onDeleteBookmark = { viewModel.deleteBookmark(it) },
                            onSettingsChanged = { viewModel.updateSettings(it) },
                            keyFlipTrigger = keyFlipTrigger
                        )
                    } else {
                        BookShelfScreen(
                            books = books,
                            settings = settings,
                            onOpenBook = { viewModel.openBook(it) },
                            onImportUri = { viewModel.importFromUri(it) },
                            onImportText = { title, content -> viewModel.importText(title, content) },
                            onDeleteBook = { viewModel.deleteBook(it) },
                            onResetSamples = { viewModel.resetSampleBooks() },
                            onSettingsChanged = { viewModel.updateSettings(it) }
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.onKeyDown(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
