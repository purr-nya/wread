package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EdgeTapScheme
import com.example.model.ReadingMode
import com.example.model.ScreenShape
import com.example.model.WatchSettings
import com.example.model.WatchTheme

@Composable
fun SettingsSheet(
    settings: WatchSettings,
    onSettingsChanged: (WatchSettings) -> Unit,
    onClose: () -> Unit
) {
    val theme = settings.watchTheme
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_sheet"),
        color = theme.backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "手表阅读偏好",
                    color = theme.textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭设置",
                        tint = theme.secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Section 1: Screen Shape
                Text(
                    text = "屏幕适配形状",
                    color = theme.secondaryTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScreenShape.entries.forEach { shape ->
                        val selected = settings.screenShape == shape
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) theme.accentColor else theme.cardBackground
                                )
                                .clickable { onSettingsChanged(settings.copy(screenShape = shape)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shape.displayName,
                                color = if (selected) Color.Black else theme.textColor,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Section 2: Reading Mode
                Text(
                    text = "翻页交互方式",
                    color = theme.secondaryTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReadingMode.entries.forEach { mode ->
                        val selected = settings.readingMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) theme.accentColor else theme.cardBackground
                                )
                                .clickable { onSettingsChanged(settings.copy(readingMode = mode)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                color = if (selected) Color.Black else theme.textColor,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Section 3: Themes
                Text(
                    text = "低功耗主题",
                    color = theme.secondaryTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WatchTheme.entries.forEach { t ->
                        val selected = settings.watchTheme == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(t.backgroundColor)
                                .clickable { onSettingsChanged(settings.copy(watchTheme = t)) }
                                .then(
                                    if (selected) Modifier.background(Color.Transparent) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(t.accentColor)
                            )
                        }
                    }
                }

                // Section 4: Font Size
                Text(
                    text = "字体字号 (${settings.fontSizeSp} sp)",
                    color = theme.secondaryTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (settings.fontSizeSp > 12) {
                                onSettingsChanged(settings.copy(fontSizeSp = settings.fontSizeSp - 1))
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = theme.cardBackground),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("A-", color = theme.textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.cardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${settings.fontSizeSp}sp",
                            color = theme.accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            if (settings.fontSizeSp < 28) {
                                onSettingsChanged(settings.copy(fontSizeSp = settings.fontSizeSp + 1))
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = theme.cardBackground),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("A+", color = theme.textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Section 5: Edge Tap Scheme
                Text(
                    text = "边缘触控区域",
                    color = theme.secondaryTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EdgeTapScheme.entries.forEach { scheme ->
                        val selected = settings.edgeTapScheme == scheme
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) theme.accentColor else theme.cardBackground
                                )
                                .clickable { onSettingsChanged(settings.copy(edgeTapScheme = scheme)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = scheme.displayName,
                                color = if (selected) Color.Black else theme.textColor,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Section 6: Toggles (Vibration & Keep Screen On)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.cardBackground)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("按键/翻页震动", color = theme.textColor, fontSize = 12.sp)
                        Text("侧键或边缘点击触感反馈", color = theme.secondaryTextColor, fontSize = 9.sp)
                    }
                    Switch(
                        checked = settings.vibrateOnFlip,
                        onCheckedChange = { onSettingsChanged(settings.copy(vibrateOnFlip = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.accentColor,
                            checkedTrackColor = theme.accentColor.copy(alpha = 0.4f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.cardBackground)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("阅读时屏幕常亮", color = theme.textColor, fontSize = 12.sp)
                        Text("防止手表过快熄屏", color = theme.secondaryTextColor, fontSize = 9.sp)
                    }
                    Switch(
                        checked = settings.keepScreenOn,
                        onCheckedChange = { onSettingsChanged(settings.copy(keepScreenOn = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.accentColor,
                            checkedTrackColor = theme.accentColor.copy(alpha = 0.4f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
