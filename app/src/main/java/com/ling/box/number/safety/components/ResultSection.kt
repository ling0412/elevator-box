package com.ling.box.number.safety.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ling.box.number.safety.data.CalculationResult
import com.ling.box.number.safety.data.ElevatorData
import com.ling.box.number.safety.utils.calculateMinPitVerticalDistance

private fun formatResult(value: Float, isCalculated: Boolean): String {
    return when {
        !isCalculated -> "等待计算"
        value < 0f -> "计算无效 (<0)"
        value >= 0f -> "%.3f m".format(value)
        else -> "错误"
    }
}

@Composable
private fun ResultItem(label: String, value: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatResult(value, value != 0f || (label == "计算轿厢越程上限 (e)")),
            color = if (value >= 0f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun ResultSection(
    result: CalculationResult,
    elevatorData: ElevatorData
) {
    val borderColor = if (result.isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.5.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("计算结果", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 12.dp))

            ResultItem("导轨制导条件 (a)", result.a)
            ResultItem("站人高度条件 (b)", result.b)
            ResultItem("最高部件a条件 (c)", result.c)
            ResultItem("最高部件b条件 (d)", result.d)
            ResultItem("轿厢越程上限 (e)", result.e)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "最大允许对重越程 (受限于 min(a,b,c,d)):",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = formatResult(result.maxOvertravel, result.isComplete && result.maxOvertravel >= 0),
                style = MaterialTheme.typography.titleLarge,
                color = if (result.isComplete && result.maxOvertravel >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )

            if (result.limitingCondition.isNotBlank() && result.limitingCondition != "请选择速度") {
                Text(
                    text = "限制条件: ${result.limitingCondition}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = if (result.isComplete && result.maxOvertravel >= 0 && elevatorData.bufferDistance > 0) 0.dp else 8.dp)
                )
            }
            if (result.isComplete && result.maxOvertravel >= 0 && elevatorData.bufferDistance > 0) {
                val bufferDistanceMeters = elevatorData.bufferDistance / 1000.0f
                if (result.maxOvertravel < bufferDistanceMeters) {
                    val diff = bufferDistanceMeters - result.maxOvertravel
                    Text(
                        text = "⚠️ 最大允许对重越程不足! 计算值(%.3f m) < 输入缓冲距h₁(%.0f mm), 需增加 %.3f m".format(
                            result.maxOvertravel,
                            elevatorData.bufferDistance,
                            diff
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "✅ 输入缓冲距h₁(%.0f mm)满足最大允许对重越程".format(elevatorData.bufferDistance),
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "最大允许轿厢越程 (e = S₅ - k - ...):",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = formatResult(result.e, result.e != 0f),
                style = MaterialTheme.typography.titleLarge,
                color = if (result.e != 0f && result.e >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
            )

            if (result.e != 0f && result.e >= 0 && elevatorData.inputCarOvertravel > 0) {
                val inputCarOvertravelMeters = elevatorData.inputCarOvertravel / 1000.0f
                if (result.e < inputCarOvertravelMeters) {
                    val diff = inputCarOvertravelMeters - result.e
                    Text(
                        text = "⚠️ 最大允许轿厢越程不足! 计算值(%.3f m) < 输入越程g(%.0f mm), 需增加 %.3f m".format(
                            result.e,
                            elevatorData.inputCarOvertravel,
                            diff
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "✅ 输入缓冲距g(%.0f mm)满足最大允许轿厢越程".format(elevatorData.inputCarOvertravel),
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (result.isComplete) {
                Text(
                    text = "✅ 所有必要计算已完成",
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Text(
                    text = "⏳ 等待所有相关参数输入以完成计算...",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

