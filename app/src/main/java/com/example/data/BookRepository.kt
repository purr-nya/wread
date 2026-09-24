package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.parser.EpubParser
import com.example.parser.TxtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BookRepository(private val database: AppDatabase) {
    private val bookDao = database.bookDao()

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBook(id: Long): Flow<BookEntity?> = bookDao.getBookById(id)

    suspend fun getBookOnce(id: Long): BookEntity? = bookDao.getBookByIdOnce(id)

    fun getChapters(bookId: Long): Flow<List<ChapterEntity>> = bookDao.getChapters(bookId)

    suspend fun getChaptersOnce(bookId: Long): List<ChapterEntity> = bookDao.getChaptersOnce(bookId)

    fun getBookmarks(bookId: Long): Flow<List<BookmarkEntity>> = bookDao.getBookmarks(bookId)

    suspend fun addBookmark(bookmark: BookmarkEntity): Long = bookDao.insertBookmark(bookmark)

    suspend fun deleteBookmark(id: Long) = bookDao.deleteBookmark(id)

    suspend fun updateReadingProgress(
        bookId: Long,
        chapterIndex: Int,
        progressPercent: Float,
        scrollOffset: Int
    ) {
        withContext(Dispatchers.IO) {
            bookDao.updateProgress(bookId, chapterIndex, progressPercent, scrollOffset)
        }
    }

    suspend fun deleteBook(book: BookEntity) {
        withContext(Dispatchers.IO) {
            bookDao.deleteBook(book)
            if (!book.isSample) {
                runCatching { File(book.filePath).delete() }
            }
        }
    }

    suspend fun ensureInitialSampleBooks(context: Context) = withContext(Dispatchers.IO) {
        val existingBooks = bookDao.getAllBooks().firstOrNull() ?: emptyList()
        if (existingBooks.isNotEmpty()) return@withContext

        val sampleFiles = SampleBooks.ensureSampleBooks(context)
        for (file in sampleFiles) {
            importFromFile(context, file, isSample = true)
        }
    }

    suspend fun importFromFile(
        context: Context,
        file: File,
        isSample: Boolean = false
    ): BookEntity = withContext(Dispatchers.IO) {
        val format = if (file.name.endsWith(".epub", ignoreCase = true)) "EPUB" else "TXT"

        val initialBook = BookEntity(
            title = file.nameWithoutExtension,
            format = format,
            filePath = file.absolutePath,
            fileSize = file.length(),
            isSample = isSample
        )
        val bookId = bookDao.insertBook(initialBook)

        val chapters: List<ChapterEntity>
        val updatedTitle: String
        val updatedAuthor: String

        if (format == "EPUB") {
            val result = EpubParser.scan(file, bookId)
            chapters = result.chapters
            updatedTitle = result.title.ifBlank { file.nameWithoutExtension }
            updatedAuthor = result.author
        } else {
            val result = TxtParser.scan(file, bookId)
            chapters = result.chapters
            updatedTitle = result.title.ifBlank { file.nameWithoutExtension }
            updatedAuthor = "未知"
        }

        bookDao.insertChapters(chapters)

        val finalizedBook = initialBook.copy(
            id = bookId,
            title = updatedTitle,
            author = updatedAuthor,
            totalChapters = chapters.size
        )
        bookDao.updateBook(finalizedBook)
        finalizedBook
    }

    suspend fun importFromUri(context: Context, uri: Uri): Result<BookEntity> = withContext(Dispatchers.IO) {
        runCatching {
            var fileName = "imported_book_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }

            val booksDir = File(context.filesDir, "imported_books")
            if (!booksDir.exists()) booksDir.mkdirs()

            val destFile = File(booksDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("无法打开文件")

            importFromFile(context, destFile, isSample = false)
        }
    }

    suspend fun importCustomText(
        context: Context,
        title: String,
        content: String
    ): Result<BookEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = title.trim().ifEmpty { "随笔_${System.currentTimeMillis() % 10000}" }
            val booksDir = File(context.filesDir, "imported_books")
            if (!booksDir.exists()) booksDir.mkdirs()

            val txtFile = File(booksDir, "$cleanTitle.txt")
            txtFile.writeText(content, Charsets.UTF_8)

            importFromFile(context, txtFile, isSample = false)
        }
    }

    suspend fun loadChapterContent(book: BookEntity, chapter: ChapterEntity): String = withContext(Dispatchers.IO) {
        val file = File(book.filePath)
        if (!file.exists()) return@withContext "文件不存在或已被移动"

        if (book.format == "EPUB") {
            EpubParser.readChapterContent(file, chapter)
        } else {
            TxtParser.readChapterContent(file, chapter)
        }
    }
}
