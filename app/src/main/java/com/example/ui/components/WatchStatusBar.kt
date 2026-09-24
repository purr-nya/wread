package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WatchStatusBar(
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    centerTitle: String? = null
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }
    var batteryLevel by remember { mutableIntStateOf(getBatteryPct(context)) }

    // Update time every 15 seconds
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeString()
            delay(15_000)
        }
    }

    // Battery receiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level != -1 && scale > 0) {
                        batteryLevel = (level * 100 / scale.toFloat()).toInt()
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentTime,
            color = textColor.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )

        if (!centerTitle.isNullOrBlank()) {
            Text(
                text = centerTitle,
                color = textColor.copy(alpha = 0.6f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$batteryLevel%",
                color = textColor.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 2.dp)
            )
            Icon(
                imageVector = Icons.Default.BatteryFull,
                contentDescription = "电池",
                tint = textColor.copy(alpha = 0.8f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private fun getCurrentTimeString(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

private fun getBatteryPct(context: Context): Int {
    return try {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 85
    } catch (_: Exception) {
        85
    }
}
