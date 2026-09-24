package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "未知",
    val format: String, // "TXT" or "EPUB"
    val filePath: String,
    val fileSize: Long = 0,
    val totalChapters: Int = 0,
    val currentChapterIndex: Int = 0,
    val currentProgressPercent: Float = 0f,
    val currentScrollOffset: Int = 0,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val isSample: Boolean = false
)
