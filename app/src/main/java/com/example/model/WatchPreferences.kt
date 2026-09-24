package com.example.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

enum class ScreenShape(val displayName: String) {
    ROUND("圆形表盘"),
    ROUNDED_SQUARE("大圆角方屏"),
    SQUARE("标准方屏")
}

enum class ReadingMode(val displayName: String) {
    VERTICAL_SCROLL("长条滚动(Webtoon)"),
    PAGED("点按翻页")
}

enum class EdgeTapScheme(val displayName: String) {
    TOP_BOTTOM("上下边缘翻页"),
    LEFT_RIGHT("左右边缘翻页")
}

enum class WatchTheme(
    val displayName: String,
    val backgroundColor: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    val cardBackground: Color
) {
    OLED_BLACK(
        displayName = "OLED纯黑",
        backgroundColor = Color(0xFF000000),
        textColor = Color(0xFFE2E8F0),
        secondaryTextColor = Color(0xFF94A3B8),
        accentColor = Color(0xFF38BDF8),
        cardBackground = Color(0xFF14171C)
    ),
    CHARCOAL(
        displayName = "深空深灰",
        backgroundColor = Color(0xFF121418),
        textColor = Color(0xFFD1D5DB),
        secondaryTextColor = Color(0xFF9CA3AF),
        accentColor = Color(0xFF60A5FA),
        cardBackground = Color(0xFF1E232B)
    ),
    AMBER_NIGHT(
        displayName = "夜光琥珀",
        backgroundColor = Color(0xFF18130E),
        textColor = Color(0xFFFDE68A),
        secondaryTextColor = Color(0xFFD97706),
        accentColor = Color(0xFFF59E0B),
        cardBackground = Color(0xFF261D15)
    ),
    CYBER_MINT(
        displayName = "极客荧绿",
        backgroundColor = Color(0xFF06140D),
        textColor = Color(0xFF86EFAC),
        secondaryTextColor = Color(0xFF34D399),
        accentColor = Color(0xFF10B981),
        cardBackground = Color(0xFF0C2419)
    ),
    PARCHMENT(
        displayName = "日光羊皮纸",
        backgroundColor = Color(0xFFF5EFE6),
        textColor = Color(0xFF292524),
        secondaryTextColor = Color(0xFF57534E),
        accentColor = Color(0xFFB45309),
        cardBackground = Color(0xFFEAE2D5)
    )
}

data class WatchSettings(
    val fontSizeSp: Int = 16,
    val lineSpacingMultiplier: Float = 1.35f,
    val screenShape: ScreenShape = ScreenShape.ROUNDED_SQUARE,
    val watchTheme: WatchTheme = WatchTheme.OLED_BLACK,
    val readingMode: ReadingMode = ReadingMode.VERTICAL_SCROLL,
    val edgeTapScheme: EdgeTapScheme = EdgeTapScheme.TOP_BOTTOM,
    val keepScreenOn: Boolean = true,
    val vibrateOnFlip: Boolean = true,
    val showStatusBar: Boolean = true
) {
    companion object {
        private const val PREFS_NAME = "wrist_reader_prefs"

        fun load(context: Context): WatchSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val shapeName = prefs.getString("screenShape", ScreenShape.ROUNDED_SQUARE.name) ?: ScreenShape.ROUNDED_SQUARE.name
            val themeName = prefs.getString("watchTheme", WatchTheme.OLED_BLACK.name) ?: WatchTheme.OLED_BLACK.name
            val modeName = prefs.getString("readingMode", ReadingMode.VERTICAL_SCROLL.name) ?: ReadingMode.VERTICAL_SCROLL.name
            val tapSchemeName = prefs.getString("edgeTapScheme", EdgeTapScheme.TOP_BOTTOM.name) ?: EdgeTapScheme.TOP_BOTTOM.name

            return WatchSettings(
                fontSizeSp = prefs.getInt("fontSizeSp", 16),
                lineSpacingMultiplier = prefs.getFloat("lineSpacingMultiplier", 1.35f),
                screenShape = runCatching { ScreenShape.valueOf(shapeName) }.getOrDefault(ScreenShape.ROUNDED_SQUARE),
                watchTheme = runCatching { WatchTheme.valueOf(themeName) }.getOrDefault(WatchTheme.OLED_BLACK),
                readingMode = runCatching { ReadingMode.valueOf(modeName) }.getOrDefault(ReadingMode.VERTICAL_SCROLL),
                edgeTapScheme = runCatching { EdgeTapScheme.valueOf(tapSchemeName) }.getOrDefault(EdgeTapScheme.TOP_BOTTOM),
                keepScreenOn = prefs.getBoolean("keepScreenOn", true),
                vibrateOnFlip = prefs.getBoolean("vibrateOnFlip", true),
                showStatusBar = prefs.getBoolean("showStatusBar", true)
            )
        }

        fun save(context: Context, settings: WatchSettings) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("fontSizeSp", settings.fontSizeSp)
                .putFloat("lineSpacingMultiplier", settings.lineSpacingMultiplier)
                .putString("screenShape", settings.screenShape.name)
                .putString("watchTheme", settings.watchTheme.name)
                .putString("readingMode", settings.readingMode.name)
                .putString("edgeTapScheme", settings.edgeTapScheme.name)
                .putBoolean("keepScreenOn", settings.keepScreenOn)
                .putBoolean("vibrateOnFlip", settings.vibrateOnFlip)
                .putBoolean("showStatusBar", settings.showStatusBar)
                .apply()
        }
    }
}
