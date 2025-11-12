package com.ling.box.calculator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ling.box.calculator.viewmodel.ElevatorCalculatorViewModel

@Composable
fun LoadElevatorDialog(
    viewModel: ElevatorCalculatorViewModel,
    onDismiss: () -> Unit,
    onSelectElevator: (Int) -> Unit
) {
    val allElevatorList = viewModel.unitStateList
    val currentElevatorIndex by viewModel.currentElevatorIndex.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // 记录对话框打开时的列表大小，用于判断是否是删除操作导致的变化
    val initialListSize = remember { allElevatorList.size }

    val filteredElevatorList = remember(allElevatorList, searchQuery) {
        if (searchQuery.isBlank()) {
            allElevatorList.mapIndexed { index, unitState -> Pair(unitState, index) }
        } else {
            allElevatorList.mapIndexedNotNull { index, unitState ->
                if (unitState.name.contains(searchQuery, ignoreCase = true) ||
                    unitState.creationDate.contains(searchQuery, ignoreCase = true)) {
                    Pair(unitState, index)
                } else {
                    null
                }
            }
        }
    }
    
    // 全选/取消全选
    val allSelected = selectedIndices.size == filteredElevatorList.size && filteredElevatorList.isNotEmpty()
    val toggleSelectAll = {
        if (allSelected) {
            selectedIndices = emptySet()
        } else {
            selectedIndices = filteredElevatorList.map { it.second }.toSet()
        }
    }
    
    // 切换选择状态
    val toggleSelection = { index: Int ->
        selectedIndices = if (selectedIndices.contains(index)) {
            selectedIndices - index
        } else {
            selectedIndices + index
        }
    }
    
    // 执行批量删除
    val performBatchDelete = {
        if (selectedIndices.isNotEmpty()) {
            val indicesToDelete = selectedIndices.toSet()
            val sizeBeforeDelete = allElevatorList.size
            viewModel.removeElevators(indicesToDelete)
            selectedIndices = emptySet()
            isMultiSelectMode = false
            showDeleteConfirmDialog = false
            // 删除后如果只剩一个或没有电梯，自动关闭对话框
            // 注意：由于删除是同步的，我们需要在下一个重组周期检查
        }
    }
    
    // 单个删除处理函数
    val handleSingleDelete = { indexToDelete: Int ->
        val sizeBeforeDelete = allElevatorList.size
        viewModel.removeElevator(indexToDelete)
        // 清除选中索引中已删除的项
        selectedIndices = selectedIndices.filter { it != indexToDelete && it < sizeBeforeDelete }
            .map { if (it > indexToDelete) it - 1 else it }
            .toSet()
    }
    
    // 监听列表变化，自动关闭对话框当只剩一个电梯时
    // 同时更新选中索引以反映新的索引
    LaunchedEffect(allElevatorList.size) {
        // 清除无效的选中索引（索引超出范围）
        selectedIndices = selectedIndices.filter { it < allElevatorList.size }.toSet()
        
        // 如果删除后只剩一个电梯，且之前有多个电梯，自动关闭对话框
        // 这样可以避免在对话框打开时列表就是1个电梯的情况下关闭对话框
        if (allElevatorList.size == 1 && initialListSize > 1) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("载入/管理电梯")
                if (isMultiSelectMode) {
                    TextButton(onClick = { isMultiSelectMode = false; selectedIndices = emptySet() }) {
                        Text("取消")
                    }
                }
            }
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜索") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "搜索", modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                    
                    if (allElevatorList.size > 1) {
                        FilledTonalButton(
                            onClick = { isMultiSelectMode = !isMultiSelectMode; selectedIndices = emptySet() },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                if (isMultiSelectMode) "取消" else "批量删除",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                if (isMultiSelectMode && filteredElevatorList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { toggleSelectAll() }
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { toggleSelectAll() }
                            )
                            Text(
                                text = if (allSelected) "取消全选" else "全选",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        if (selectedIndices.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("删除 (${selectedIndices.size})")
                            }
                        }
                    }
                }

                LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)) {
                    itemsIndexed(filteredElevatorList) { displayIndex, (elevatorUnitState, originalIndex) ->
                        val isSelected = selectedIndices.contains(originalIndex)
                        val isCurrent = originalIndex == currentElevatorIndex
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { 
                                    if (isMultiSelectMode) {
                                        toggleSelection(originalIndex)
                                    } else {
                                        onSelectElevator(originalIndex)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isMultiSelectMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { toggleSelection(originalIndex) }
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = elevatorUnitState.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "创建日期: ${elevatorUnitState.creationDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                if (!isMultiSelectMode && allElevatorList.size > 1) {
                                    IconButton(
                                        onClick = {
                                            handleSingleDelete(originalIndex)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "删除电梯")
                                    }
                                }
                            }
                        }
                    }
                    if (filteredElevatorList.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            Text(
                                text = "未找到相关电梯",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
    
    // 批量删除确认对话框
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = { 
                Text("确定要删除选中的 ${selectedIndices.size} 个电梯吗？此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = performBatchDelete,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

