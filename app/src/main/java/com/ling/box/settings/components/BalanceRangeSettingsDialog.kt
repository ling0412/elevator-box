package com.ling.box.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BalanceRangeSettingsDialog(
    currentMin: Float,
    currentMax: Float,
    currentIdeal: Float,
    onConfirm: (Float, Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    // 确保当前值在有效范围内（40-50）
    var minValue by remember(currentMin) { mutableStateOf(currentMin.coerceIn(40f, 50f)) }
    var maxValue by remember(currentMax) { mutableStateOf(currentMax.coerceIn(40f, 50f)) }
    var idealValue by remember(currentIdeal) { mutableStateOf(currentIdeal.coerceIn(40f, 50f)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "平衡系数范围设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // 最小合格范围滑块
                Text(
                    text = "最小合格范围: ${String.format("%.1f", minValue)}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = minValue,
                    onValueChange = { newValue ->
                        minValue = newValue
                        // 确保最小值不超过最大值和理想值
                        if (minValue >= maxValue) {
                            maxValue = (minValue + 0.5f).coerceIn(40f, 50f)
                        }
                        if (minValue >= idealValue) {
                            idealValue = ((minValue + maxValue) / 2f).coerceIn(minValue, maxValue)
                        }
                    },
                    valueRange = 40f..50f,
                    steps = 99 // (50-40)*10 - 1，允许0.1的步进
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 最大合格范围滑块
                Text(
                    text = "最大合格范围: ${String.format("%.1f", maxValue)}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = maxValue,
                    onValueChange = { newValue ->
                        maxValue = newValue
                        // 确保最大值不小于最小值和理想值
                        if (maxValue <= minValue) {
                            minValue = (maxValue - 0.5f).coerceIn(40f, 50f)
                        }
                        if (maxValue <= idealValue) {
                            idealValue = ((minValue + maxValue) / 2f).coerceIn(minValue, maxValue)
                        }
                    },
                    valueRange = 40f..50f,
                    steps = 99 // (50-40)*10 - 1，允许0.1的步进
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 最佳平衡点滑块
                Text(
                    text = "最佳平衡点: ${String.format("%.1f", idealValue)}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = idealValue,
                    onValueChange = { newValue ->
                        idealValue = newValue.coerceIn(minValue, maxValue)
                    },
                    valueRange = minValue..maxValue,
                    steps = ((maxValue - minValue) * 10).toInt().coerceAtLeast(0).coerceAtMost(99)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "合格范围: ${String.format("%.1f", minValue)}% - ${String.format("%.1f", maxValue)}%\n最佳平衡点: ${String.format("%.1f", idealValue)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(minValue, maxValue, idealValue) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

