package com.example.ui

import android.app.Application
import android.net.Uri
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BookEntity
import com.example.data.BookmarkEntity
import com.example.data.BookRepository
import com.example.data.ChapterEntity
import com.example.model.WatchSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BookRepository(AppDatabase.getInstance(application))

    val books: StateFlow<List<BookEntity>> = repository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeBookId = MutableStateFlow<Long?>(null)
    val activeBookId: StateFlow<Long?> = _activeBookId.asStateFlow()

    private val _activeBook = MutableStateFlow<BookEntity?>(null)
    val activeBook: StateFlow<BookEntity?> = _activeBook.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val chapters: StateFlow<List<ChapterEntity>> = _chapters.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _currentChapterContent = MutableStateFlow("")
    val currentChapterContent: StateFlow<String> = _currentChapterContent.asStateFlow()

    private val _settings = MutableStateFlow(WatchSettings.load(application))
    val settings: StateFlow<WatchSettings> = _settings.asStateFlow()

    // Key flip trigger: positive = next, negative = prev
    private val _keyFlipTrigger = MutableStateFlow(0)
    val keyFlipTrigger: StateFlow<Int> = _keyFlipTrigger.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialSampleBooks(application)
        }
    }

    fun openBook(bookId: Long) {
        viewModelScope.launch {
            val book = repository.getBookOnce(bookId) ?: return@launch
            _activeBookId.value = bookId
            _activeBook.value = book

            val bookChapters = repository.getChaptersOnce(bookId)
            _chapters.value = bookChapters

            // Observe bookmarks
            viewModelScope.launch {
                repository.getBookmarks(bookId).collect {
                    _bookmarks.value = it
                }
            }

            // Restore last read chapter
            val targetChapterIndex = book.currentChapterIndex.coerceIn(0, (bookChapters.size - 1).coerceAtLeast(0))
            _currentChapterIndex.value = targetChapterIndex

            loadChapter(book, bookChapters, targetChapterIndex)
        }
    }

    fun closeBook() {
        _activeBookId.value = null
        _activeBook.value = null
        _chapters.value = emptyList()
        _bookmarks.value = emptyList()
        _currentChapterContent.value = ""
    }

    fun selectChapter(chapterIndex: Int) {
        val book = _activeBook.value ?: return
        val bookChapters = _chapters.value
        if (chapterIndex !in bookChapters.indices) return

        _currentChapterIndex.value = chapterIndex
        loadChapter(book, bookChapters, chapterIndex)
    }

    private fun loadChapter(book: BookEntity, chaptersList: List<ChapterEntity>, index: Int) {
        viewModelScope.launch {
            val chapter = chaptersList.getOrNull(index)
            if (chapter == null) {
                _currentChapterContent.value = ""
                return@launch
            }
            _currentChapterContent.value = "加载中..."
            val text = repository.loadChapterContent(book, chapter)
            _currentChapterContent.value = text.ifBlank { "本章暂无内容" }
        }
    }

    fun updateProgress(chapterIndex: Int, progressPercent: Float, scrollOffset: Int) {
        val bookId = _activeBookId.value ?: return
        viewModelScope.launch {
            repository.updateReadingProgress(bookId, chapterIndex, progressPercent, scrollOffset)
        }
    }

    fun addBookmark(chapterTitle: String, excerpt: String) {
        val bookId = _activeBookId.value ?: return
        viewModelScope.launch {
            repository.addBookmark(
                BookmarkEntity(
                    bookId = bookId,
                    chapterIndex = _currentChapterIndex.value,
                    chapterTitle = chapterTitle,
                    excerpt = excerpt
                )
            )
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            repository.importFromUri(getApplication(), uri)
        }
    }

    fun importText(title: String, content: String) {
        viewModelScope.launch {
            repository.importCustomText(getApplication(), title, content)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            if (_activeBookId.value == book.id) {
                closeBook()
            }
            repository.deleteBook(book)
        }
    }

    fun resetSampleBooks() {
        viewModelScope.launch {
            repository.ensureInitialSampleBooks(getApplication())
        }
    }

    fun updateSettings(newSettings: WatchSettings) {
        _settings.value = newSettings
        WatchSettings.save(getApplication(), newSettings)
    }

    fun onKeyDown(keyCode: Int): Boolean {
        if (_activeBookId.value == null) return false
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_NAVIGATE_NEXT -> {
                _keyFlipTrigger.value = _keyFlipTrigger.value + 1
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> {
                _keyFlipTrigger.value = _keyFlipTrigger.value - 1
                true
            }
            else -> false
        }
    }
}
