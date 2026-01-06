package com.ling.box.calculator.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BalanceWarningSection(
    recommendedBlocksMessage: String?,
    displayBalanceCoefficient: Double?,
    balanceRangeMin: Double = 45.0,
    balanceRangeMax: Double = 50.0
) {
    var expanded by remember { mutableStateOf(false) }
    val feasibleOptions = remember(recommendedBlocksMessage) {
        recommendedBlocksMessage?.lines()?.dropWhile { !it.startsWith("可行方案:") }?.drop(1)
            ?.takeWhile { !it.startsWith("当前状态:") && !it.startsWith("★ 推荐方案:") && it.isNotBlank() }
            ?.map { it.trim().removePrefix("•").trim() }
            ?: emptyList()
    }
    val feasibleOptionCount = feasibleOptions.size

    val enterAnimation = slideInVertically(animationSpec = tween(200)) { -it / 3 } + fadeIn(animationSpec = tween(200))
    val exitAnimation = slideOutVertically(animationSpec = tween(200)) { -it / 3 } + fadeOut(animationSpec = tween(200))

    AnimatedVisibility(
        visible = !recommendedBlocksMessage.isNullOrEmpty(),
        enter = enterAnimation,
        exit = exitAnimation
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = feasibleOptions.isNotEmpty()) {
                    expanded = !expanded
                },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val parts = (recommendedBlocksMessage ?: "").split("★ 推荐方案:")
                val feasiblePart = parts.getOrNull(0)
                val recommendationPart = parts.getOrNull(1)

                if (!feasiblePart.isNullOrBlank() && feasiblePart.contains("可行方案:")) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "可行方案 ($feasibleOptionCount 个)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedContent(
                                targetState = expanded,
                                label = "展开选项"
                            ) { isExpanded ->
                                if (isExpanded) {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        feasibleOptions.forEach { option ->
                                            Text(
                                                text = "  • $option",
                                                style = MaterialTheme.typography.bodyMedium,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                } else if (feasibleOptionCount > 0) {
                                    Text(
                                        text = "点击展开查看所有可行方案",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                if (!recommendationPart.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = "推荐", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "推荐方案",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        text = recommendationPart.trim(),
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                } else if (feasiblePart.isNullOrBlank() || !feasiblePart.contains("可行方案:")) {
                    Text(
                        text = recommendedBlocksMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }

    val showWarning = recommendedBlocksMessage.isNullOrEmpty() &&
            displayBalanceCoefficient != null &&
            displayBalanceCoefficient != 0.0 &&
            (displayBalanceCoefficient <= balanceRangeMin || displayBalanceCoefficient >= balanceRangeMax)

    AnimatedVisibility(
        visible = showWarning,
        enter = enterAnimation,
        exit = exitAnimation
    ) {
        val coeff = displayBalanceCoefficient ?: 0.0
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(MaterialTheme.shapes.medium),
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "警告",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "当前平衡系数 ${"%.2f".format(coeff)}% 超出推荐范围 (${"%.1f".format(balanceRangeMin)}%-${"%.1f".format(balanceRangeMax)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

