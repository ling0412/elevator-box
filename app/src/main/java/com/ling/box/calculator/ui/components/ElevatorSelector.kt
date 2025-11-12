package com.ling.box.calculator.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ling.box.calculator.viewmodel.ElevatorCalculatorViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 日期格式化器常量，避免每次重组都创建新实例
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun ElevatorSelector(
    viewModel: ElevatorCalculatorViewModel,
    onElevatorSelect: (Int) -> Unit
) {
    val currentElevatorIndex by viewModel.currentElevatorIndex.collectAsStateWithLifecycle()
    val allElevatorList = viewModel.unitStateList

    var showRenameDialog by remember { mutableStateOf(false) }
    var elevatorToRenameIndex by remember { mutableIntStateOf(-1) }
    var newElevatorNameInput by remember { mutableStateOf("") }
    var showLoadElevatorDialog by remember { mutableStateOf(false) }

    // 只显示最近3天的电梯，避免列表过长
    // 使用 derivedStateOf 确保当 allElevatorList 或其内容变化时能正确刷新
    // 访问 unitState.name 以确保名称变化时能触发重新计算
    val recentElevatorData = remember {
        derivedStateOf {
            val today = LocalDate.now()

            allElevatorList.mapIndexedNotNull { originalIndex, unitState ->
                // 访问名称以建立状态依赖，确保名称变化时能触发重新计算
                unitState.name.let { }
                try {
                    val creationDate = LocalDate.parse(unitState.creationDate, DATE_FORMATTER)
                    // 显示最近3天内的电梯
                    if (!creationDate.isBefore(today.minusDays(3)) && !creationDate.isAfter(today)) {
                        originalIndex
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    // 如果日期解析失败，也显示（兼容旧数据）
                    originalIndex
                }
            }
        }
    }.value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .padding(horizontal = 4.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 0.dp
        ) {
            val listState = rememberLazyListState()
            
            // 自动滚动到当前选中的电梯
            androidx.compose.runtime.LaunchedEffect(currentElevatorIndex) {
                val indexInRecent = recentElevatorData.indexOfFirst { it == currentElevatorIndex }
                if (indexInRecent >= 0) {
                    listState.animateScrollToItem(indexInRecent)
                }
            }
            
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                items(
                    count = recentElevatorData.size,
                    key = { displayIndex -> 
                        recentElevatorData[displayIndex]
                    }
                ) { displayIndex ->
                    val originalIndex = recentElevatorData[displayIndex]
                    // 从 allElevatorList 中实时获取最新的 unitState，确保名称更新时能正确刷新
                    val unitState = allElevatorList.getOrNull(originalIndex)
                    if (unitState == null) return@items // 如果 unitState 不存在，跳过渲染
                    val isSelected = currentElevatorIndex == originalIndex

                    val itemBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        animationSpec = tween(durationMillis = 250),
                        label = "ElevatorPillItemBgColor"
                    )

                    val itemTextColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        animationSpec = tween(durationMillis = 250),
                        label = "ElevatorPillItemTextColor"
                    )

                    val itemElevation by animateDpAsState(
                        targetValue = if (isSelected) 6.dp else 1.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "ElevatorPillItemElevation"
                    )
                    
                    val itemWidth by animateDpAsState(
                        targetValue = if (isSelected) 70.dp else 60.dp,
                        animationSpec = tween(durationMillis = 250),
                        label = "ElevatorPillItemWidth"
                    )

                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(40.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .shadow(itemElevation, shape = MaterialTheme.shapes.extraLarge, clip = false)
                            .background(itemBgColor)
                            .combinedClickable(
                                onClick = { onElevatorSelect(originalIndex) },
                                onLongClick = {
                                    elevatorToRenameIndex = originalIndex
                                    newElevatorNameInput = allElevatorList.getOrNull(originalIndex)?.name ?: ""
                                    showRenameDialog = true
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AnimatedContent(
                                targetState = unitState.name,
                                transitionSpec = {
                                    slideInVertically(animationSpec = tween(200, easing = LinearOutSlowInEasing)) { fullHeight -> fullHeight / 2 } + fadeIn(animationSpec = tween(200)) togetherWith
                                            slideOutVertically(animationSpec = tween(200, easing = LinearOutSlowInEasing)) { fullHeight -> -fullHeight / 2 } + fadeOut(animationSpec = tween(200))
                                }, label = "ElevatorNameTransition"
                            ) { targetText ->
                                // 如果是默认格式"电梯 X"，只显示数字；否则显示最后三个字符
                                val displayedText = if (targetText.matches(Regex("电梯 \\d+"))) {
                                    // 提取数字部分
                                    targetText.replace("电梯 ", "")
                                } else {
                                    // 自定义名称，显示最后三个字符
                                    if (targetText.length <= 3) {
                                        targetText
                                    } else {
                                        targetText.takeLast(3)
                                    }
                                }
                                Text(
                                    text = displayedText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = if (isSelected) 14.sp else 13.sp
                                    ),
                                    color = itemTextColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        val buttonContainerColor = MaterialTheme.colorScheme.primaryContainer
        val buttonContentColor = MaterialTheme.colorScheme.onPrimaryContainer

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { viewModel.addElevator() },
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加电梯", modifier = Modifier.size(24.dp))
            }
            
            if (allElevatorList.size > 1) {
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { viewModel.removeElevator(currentElevatorIndex) },
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = buttonContainerColor,
                        contentColor = buttonContentColor
                    )
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "删除电梯", modifier = Modifier.size(24.dp))
                }
            }
        }
        
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = { showLoadElevatorDialog = true },
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = buttonContainerColor,
                contentColor = buttonContentColor
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "载入/管理电梯", modifier = Modifier.size(24.dp))
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名电梯") },
            text = {
                TextField(
                    value = newElevatorNameInput,
                    onValueChange = { newElevatorNameInput = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (elevatorToRenameIndex != -1) {
                            viewModel.updateElevatorName(elevatorToRenameIndex, newElevatorNameInput)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showRenameDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    if (showLoadElevatorDialog) {
        LoadElevatorDialog(
            viewModel = viewModel,
            onDismiss = { showLoadElevatorDialog = false },
            onSelectElevator = { index ->
                viewModel.selectElevator(index)
                showLoadElevatorDialog = false
            }
        )
    }
}

