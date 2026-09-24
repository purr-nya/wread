package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WatchTheme

@Composable
fun TextImportDialog(
    watchTheme: WatchTheme,
    onConfirm: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("text_import_dialog"),
        color = watchTheme.backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = "腕上速记/粘贴文本",
                color = watchTheme.textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题 (可选)", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = watchTheme.textColor,
                    unfocusedTextColor = watchTheme.textColor,
                    focusedBorderColor = watchTheme.accentColor,
                    unfocusedBorderColor = watchTheme.secondaryTextColor.copy(alpha = 0.5f),
                    focusedContainerColor = watchTheme.cardBackground,
                    unfocusedContainerColor = watchTheme.cardBackground
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("粘贴或输入小说文本...", fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = watchTheme.textColor,
                    unfocusedTextColor = watchTheme.textColor,
                    focusedBorderColor = watchTheme.accentColor,
                    unfocusedBorderColor = watchTheme.secondaryTextColor.copy(alpha = 0.5f),
                    focusedContainerColor = watchTheme.cardBackground,
                    unfocusedContainerColor = watchTheme.cardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("取消", color = watchTheme.textColor, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            onConfirm(title, content)
                        }
                    },
                    enabled = content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = watchTheme.accentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("存入书架", color = androidx.compose.ui.graphics.Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
