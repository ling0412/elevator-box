package com.ling.box.calculator.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class DataStatus {
    NOT_INPUT,
    ACCURACY_WARNING,
    NORMAL
}

@Composable
fun BalanceCoefficientDisplay(
    useManualBalance: Boolean,
    manualBalanceCoefficientK: String?,
    balanceCoefficientK: Double?,
    upwardCurrentPoints: List<Pair<Double, Float>>,
    downwardCurrentPoints: List<Pair<Double, Float>>,
    hasActualIntersection: Boolean,
    balanceRangeMin: Double = 45.0,
    balanceRangeMax: Double = 50.0
) {
    val (displayValue, dataStatus) = remember(useManualBalance, manualBalanceCoefficientK, balanceCoefficientK, hasActualIntersection, upwardCurrentPoints, downwardCurrentPoints) {
        val value = if (useManualBalance) {
            manualBalanceCoefficientK?.toDoubleOrNull()
        } else {
            balanceCoefficientK
        }

        val status = when {
            value == null && useManualBalance && manualBalanceCoefficientK.isNullOrEmpty() -> DataStatus.NOT_INPUT
            value == null && !useManualBalance && upwardCurrentPoints.isEmpty() && downwardCurrentPoints.isEmpty() -> DataStatus.NOT_INPUT
            value == null && !useManualBalance -> DataStatus.NOT_INPUT
            !useManualBalance && !hasActualIntersection -> DataStatus.ACCURACY_WARNING
            else -> DataStatus.NORMAL
        }
        Pair(value, status)
    }

    val animatedDisplayValue by animateFloatAsState(
        targetValue = (displayValue ?: 0.0).toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "BalanceValueAnimation"
    )

    val progressFraction = when {
        displayValue == null -> 0f
        displayValue <= 0 -> 0f
        displayValue >= 100 -> 1f
        else -> (displayValue / 100.0).toFloat()
    }
    val animatedProgressFraction by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1000),
        label = "BalanceProgressAnimation"
    )

    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "平衡系数 ${if (useManualBalance) "(手动输入)" else "(电流法)"}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = dataStatus,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(300)) { it / 2 } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutVertically(animationSpec = tween(300)) { -it / 2 } + fadeOut(animationSpec = tween(300)))
            },
            label = "DataStatusAnimation"
        ) { targetStatus ->
            when (targetStatus) {
                DataStatus.NOT_INPUT -> {
                    Text(
                        text = when {
                            useManualBalance -> "请输入平衡系数"
                            else -> "数据不足或结果异常"
                        },
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                DataStatus.ACCURACY_WARNING -> {
                    val color = Color(0xFFFFC107)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.2f %%".format(animatedDisplayValue),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = color
                        )
                        Text(
                            text = "精度存疑 (延长线计算)",
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                DataStatus.NORMAL -> {
                    val value = displayValue ?: 0.0
                    val (color, statusText) = when {
                        value > balanceRangeMin && value < balanceRangeMax -> Pair(MaterialTheme.colorScheme.primary, "理想范围")
                        else -> Pair(MaterialTheme.colorScheme.error, "超出范围")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.2f %%".format(animatedDisplayValue),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = color
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        if (dataStatus != DataStatus.NOT_INPUT && displayValue != null) {
            val value = displayValue
            val progressColor = when (dataStatus) {
                DataStatus.ACCURACY_WARNING -> Color(0xFFFFC107)
                DataStatus.NORMAL -> if (value > balanceRangeMin && value < balanceRangeMax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.6f)
                    .background(progressColor.copy(alpha = 0.3f), shape = CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgressFraction)
                        .fillMaxHeight()
                        .background(progressColor, shape = CircleShape)
                )
            }
        }
    }
}

