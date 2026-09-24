package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ScreenShape
import com.example.model.WatchSettings
import com.example.model.WatchTheme
import com.example.parser.TxtParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WristReader", appName)
    }

    @Test
    fun testTxtParserChapterScan() {
        val file = tempFolder.newFile("test_novel.txt")
        file.writeText(
            """
            引子
            在宇宙深处，有一颗沉默的蓝色星体。

            第一章 星海启航
            战舰在跃迁通道中穿梭。
            船员们凝视着舷窗外的光带。

            第二章 机械回响
            废弃基地的发条齿轮仍在咬合。
            """.trimIndent(),
            Charsets.UTF_8
        )

        val result = TxtParser.scan(file, bookId = 1L)
        assertTrue(result.chapters.isNotEmpty())
        assertEquals(3, result.chapters.size)
        assertTrue(result.chapters[0].title.contains("引子"))
        assertTrue(result.chapters[1].title.contains("第一章"))
        assertTrue(result.chapters[2].title.contains("第二章"))

        // Test reading chapter content
        val ch1Content = TxtParser.readChapterContent(file, result.chapters[1])
        assertTrue(ch1Content.contains("战舰在跃迁通道中穿梭"))
    }

    @Test
    fun testWatchSettingsPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initial = WatchSettings(
            fontSizeSp = 20,
            screenShape = ScreenShape.ROUND,
            watchTheme = WatchTheme.CYBER_MINT
        )
        WatchSettings.save(context, initial)

        val loaded = WatchSettings.load(context)
        assertEquals(20, loaded.fontSizeSp)
        assertEquals(ScreenShape.ROUND, loaded.screenShape)
        assertEquals(WatchTheme.CYBER_MINT, loaded.watchTheme)
    }
}
