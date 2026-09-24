package com.example.parser

import com.example.data.ChapterEntity
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object TxtParser {
    // Standard chapter title regex for Chinese & English web novels
    private val CHAPTER_PATTERN = Pattern.compile(
        "^\\s*(第[0-9一二三四五六七八九十百千万零]+[章回节卷集篇话部]|Chapter\\s*\\d+|CHAPTER\\s*\\d+|引子|序言|楔子|尾声|后记|番外).*$",
        Pattern.MULTILINE
    )

    data class ScanResult(
        val title: String,
        val encoding: String,
        val chapters: List<ChapterEntity>
    )

    fun scan(file: File, bookId: Long): ScanResult {
        val encoding = detectCharset(file)
        val charset = Charset.forName(encoding)
        val fileLength = file.length()

        val parsedChapters = mutableListOf<ChapterDraft>()
        var chapterIndex = 0

        val raf = RandomAccessFile(file, "r")
        try {
            var currentOffset = 0L
            val buffer = ByteArray(64 * 1024)
            var leftover = ""
            var leftoverOffset = 0L

            while (currentOffset < fileLength) {
                val bytesToRead = minOf(buffer.size.toLong(), fileLength - currentOffset).toInt()
                raf.seek(currentOffset)
                val readBytes = raf.read(buffer, 0, bytesToRead)
                if (readBytes <= 0) break

                val chunkString = String(buffer, 0, readBytes, charset)
                val combined = leftover + chunkString
                val baseByteOffset = leftoverOffset

                val lines = combined.split("\r\n", "\n", "\r")
                // Keep the last incomplete line for next iteration if not EOF
                val isEof = (currentOffset + readBytes >= fileLength)
                val linesToProcess = if (isEof) lines else lines.dropLast(1)
                leftover = if (isEof) "" else lines.last()

                var runningByteInChunk = 0L
                for (line in linesToProcess) {
                    val lineBytes = (line + "\n").toByteArray(charset).size.toLong()
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && trimmed.length <= 45 && CHAPTER_PATTERN.matcher(trimmed).matches()) {
                        val chapterByteOffset = baseByteOffset + runningByteInChunk
                        parsedChapters.add(
                            ChapterDraft(
                                index = chapterIndex++,
                                title = trimmed,
                                byteOffset = chapterByteOffset
                            )
                        )
                    }
                    runningByteInChunk += lineBytes
                }

                currentOffset += readBytes
                leftoverOffset = currentOffset - leftover.toByteArray(charset).size
            }
        } finally {
            raf.close()
        }

        // If no chapters detected, divide by safe segments (~12 KB per chapter for watch memory)
        val finalChapters = mutableListOf<ChapterEntity>()
        if (parsedChapters.isEmpty()) {
            val chunkSize = 12 * 1024L
            var offset = 0L
            var index = 0
            while (offset < fileLength) {
                val length = minOf(chunkSize, fileLength - offset).toInt()
                finalChapters.add(
                    ChapterEntity(
                        bookId = bookId,
                        chapterIndex = index,
                        title = "第 ${index + 1} 节",
                        byteOffset = offset,
                        byteLength = length
                    )
                )
                offset += length
                index++
            }
        } else {
            // Compute byte lengths
            for (i in parsedChapters.indices) {
                val current = parsedChapters[i]
                val nextOffset = if (i + 1 < parsedChapters.size) {
                    parsedChapters[i + 1].byteOffset
                } else {
                    fileLength
                }
                val length = (nextOffset - current.byteOffset).coerceAtLeast(0L).toInt()
                finalChapters.add(
                    ChapterEntity(
                        bookId = bookId,
                        chapterIndex = current.index,
                        title = current.title,
                        byteOffset = current.byteOffset,
                        byteLength = length
                    )
                )
            }
        }

        // Determine title from filename
        val bookTitle = file.nameWithoutExtension.take(30)
        return ScanResult(
            title = bookTitle,
            encoding = encoding,
            chapters = finalChapters
        )
    }

    fun readChapterContent(file: File, chapter: ChapterEntity, encoding: String = "UTF-8"): String {
        if (!file.exists() || chapter.byteLength <= 0) return ""
        val charset = try {
            Charset.forName(encoding)
        } catch (_: Exception) {
            StandardCharsets.UTF_8
        }

        val raf = RandomAccessFile(file, "r")
        return try {
            raf.seek(chapter.byteOffset)
            val bytes = ByteArray(chapter.byteLength)
            val read = raf.read(bytes)
            if (read > 0) {
                cleanText(String(bytes, 0, read, charset))
            } else ""
        } catch (_: Exception) {
            ""
        } finally {
            raf.close()
        }
    }

    private fun cleanText(raw: String): String {
        val lines = raw.split("\r\n", "\n", "\r")
        val cleanParagraphs = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                // Prepend Chinese standard 2 full-width spaces for paragraphs
                cleanParagraphs.add("\u3000\u3000$trimmed")
            }
        }
        return cleanParagraphs.joinToString("\n\n")
    }

    private fun detectCharset(file: File): String {
        // Sample first 4KB to detect UTF-8 or GBK
        val sampleSize = minOf(4096L, file.length()).toInt()
        if (sampleSize <= 0) return "UTF-8"

        val buffer = ByteArray(sampleSize)
        val raf = RandomAccessFile(file, "r")
        try {
            raf.readFully(buffer)
        } catch (_: Exception) {
            return "UTF-8"
        } finally {
            raf.close()
        }

        if (isUtf8(buffer)) {
            return "UTF-8"
        }
        // Fallback for Chinese text
        return "GB18030"
    }

    private fun isUtf8(bytes: ByteArray): Boolean {
        var i = 0
        var hasHighBytes = false
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b <= 0x7F) {
                i++
                continue
            }
            hasHighBytes = true
            val bytesNeeded = when {
                (b and 0xE0) == 0xC0 -> 1
                (b and 0xF0) == 0xE0 -> 2
                (b and 0xF8) == 0xF0 -> 3
                else -> return false
            }
            if (i + bytesNeeded >= bytes.size) break
            for (j in 1..bytesNeeded) {
                val follow = bytes[i + j].toInt() and 0xFF
                if ((follow and 0xC0) != 0x80) return false
            }
            i += bytesNeeded + 1
        }
        return hasHighBytes
    }

    private data class ChapterDraft(
        val index: Int,
        val title: String,
        val byteOffset: Long
    )
}
