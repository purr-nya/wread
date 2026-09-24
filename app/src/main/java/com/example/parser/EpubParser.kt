package com.example.parser

import com.example.data.ChapterEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object EpubParser {

    data class EpubMetadata(
        val title: String,
        val author: String,
        val chapters: List<ChapterEntity>
    )

    fun scan(file: File, bookId: Long): EpubMetadata {
        val zip = ZipFile(file)
        try {
            // 1. Locate root opf file from META-INF/container.xml
            val opfPath = getOpfPath(zip) ?: "content.opf"
            val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

            // 2. Parse OPF file
            val opfEntry = zip.getEntry(opfPath) ?: throw IllegalArgumentException("OPF file not found in EPUB")
            val opfStream = zip.getInputStream(opfEntry)
            val opfContent = opfStream.bufferedReader().use { it.readText() }

            val title = extractTagContent(opfContent, "dc:title") ?: file.nameWithoutExtension
            val author = extractTagContent(opfContent, "dc:creator") ?: "未知"

            // 3. Parse manifest and spine
            val manifest = parseManifest(opfContent, opfDir)
            val spineIds = parseSpine(opfContent)

            // 4. Try parsing TOC (toc.ncx)
            val ncxHref = manifest["ncx"] ?: manifest.entries.firstOrNull { it.value.endsWith(".ncx") }?.value
            val ncxTitles = if (ncxHref != null) {
                parseNcx(zip, ncxHref)
            } else {
                emptyMap()
            }

            // 5. Build chapter entities
            val chapters = mutableListOf<ChapterEntity>()
            var chapterIndex = 0

            for (spineId in spineIds) {
                val href = manifest[spineId] ?: continue
                // Don't include cover or nav only if they don't have text
                val cleanHref = href.substringBefore("#")
                val titleFromNcx = ncxTitles[cleanHref] ?: ncxTitles[href]
                val defaultTitle = "第 ${chapterIndex + 1} 话"
                val chapterTitle = titleFromNcx ?: defaultTitle

                chapters.add(
                    ChapterEntity(
                        bookId = bookId,
                        chapterIndex = chapterIndex++,
                        title = chapterTitle,
                        entryName = href
                    )
                )
            }

            if (chapters.isEmpty()) {
                // Fallback: list all html/xhtml entries in zip
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".html", ignoreCase = true) || entry.name.endsWith(".xhtml", ignoreCase = true)) {
                        chapters.add(
                            ChapterEntity(
                                bookId = bookId,
                                chapterIndex = chapterIndex++,
                                title = "第 ${chapterIndex} 节",
                                entryName = entry.name
                            )
                        )
                    }
                }
            }

            return EpubMetadata(
                title = title.take(30),
                author = author.take(20),
                chapters = chapters
            )
        } finally {
            zip.close()
        }
    }

    fun readChapterContent(file: File, chapter: ChapterEntity): String {
        if (!file.exists() || chapter.entryName.isBlank()) return ""
        val zip = ZipFile(file)
        try {
            val entry = zip.getEntry(chapter.entryName)
                ?: findEntryFuzzy(zip, chapter.entryName)
                ?: return ""

            val html = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            return cleanHtmlToText(html)
        } catch (_: Exception) {
            return ""
        } finally {
            zip.close()
        }
    }

    private fun findEntryFuzzy(zip: ZipFile, target: String): ZipEntry? {
        val targetName = target.substringAfterLast("/")
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val e = entries.nextElement()
            if (e.name.endsWith(targetName, ignoreCase = true)) {
                return e
            }
        }
        return null
    }

    private fun getOpfPath(zip: ZipFile): String? {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
        val xml = zip.getInputStream(containerEntry).bufferedReader().use { it.readText() }
        val regex = Regex("""full-path\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(xml)?.groupValues?.get(1)
    }

    private fun extractTagContent(xml: String, tagName: String): String? {
        val regex = Regex("""<$tagName[^>]*>([^<]+)</$tagName>""", RegexOption.IGNORE_CASE)
        return regex.find(xml)?.groupValues?.get(1)?.trim()
    }

    private fun parseManifest(opfXml: String, baseDir: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val itemRegex = Regex("""<item\s+([^>]+)/>|<item\s+([^>]+)>""", RegexOption.IGNORE_CASE)
        val idRegex = Regex("""id\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val hrefRegex = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        for (match in itemRegex.findAll(opfXml)) {
            val attrs = match.groupValues[1].ifEmpty { match.groupValues[2] }
            val id = idRegex.find(attrs)?.groupValues?.get(1)
            val href = hrefRegex.find(attrs)?.groupValues?.get(1)
            if (id != null && href != null) {
                map[id] = baseDir + href
            }
        }
        return map
    }

    private fun parseSpine(opfXml: String): List<String> {
        val ids = mutableListOf<String>()
        val itemrefRegex = Regex("""<itemref\s+[^>]*idref\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        for (match in itemrefRegex.findAll(opfXml)) {
            ids.add(match.groupValues[1])
        }
        return ids
    }

    private fun parseNcx(zip: ZipFile, ncxPath: String): Map<String, String> {
        val titlesByHref = mutableMapOf<String, String>()
        try {
            val entry = zip.getEntry(ncxPath) ?: findEntryFuzzy(zip, ncxPath) ?: return emptyMap()
            val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }

            val navPointRegex = Regex("""<navPoint[\s\S]*?<text>([^<]+)</text>[\s\S]*?<content\s+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            for (match in navPointRegex.findAll(xml)) {
                val title = match.groupValues[1].trim()
                val src = match.groupValues[2].substringBefore("#")
                val ncxDir = if (ncxPath.contains("/")) ncxPath.substringBeforeLast("/") + "/" else ""
                val fullSrc = ncxDir + src
                titlesByHref[fullSrc] = title
                titlesByHref[src] = title
            }
        } catch (_: Exception) {
        }
        return titlesByHref
    }

    private fun cleanHtmlToText(html: String): String {
        // Strip script and style tags
        var text = html.replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "")

        // Replace headers and paragraph endings with newlines
        text = text.replace(Regex("""</(p|div|h[1-6]|li|blockquote)>""", RegexOption.IGNORE_CASE), "\n\n")
        text = text.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")

        // Strip remaining tags
        text = text.replace(Regex("""<[^>]+>"""), "")

        // Unescape entities
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&hellip;", "…")

        // Format clean watch paragraphs
        val lines = text.split("\n")
        val paragraphs = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                paragraphs.add("\u3000\u3000$trimmed")
            }
        }
        return paragraphs.joinToString("\n\n")
    }
}
