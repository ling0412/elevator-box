package com.ling.box.number

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberBaseConverterPage() {
    var inputText by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf(InputMode.NUMERIC) }
    var manualBase by remember { mutableStateOf<Int?>(null) }

    val (autoDetectedType, autoDetectedBase) = remember(inputText, inputMode) {
        detectInputBase(inputText, inputMode)
    }
    val selectedBase = manualBase ?: autoDetectedBase

    val context = LocalContext.current
    val clipboardManager = remember { 
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager 
    }
    val keyboardController = LocalSoftwareKeyboardController.current // 保留，但下面clear中注释掉了hide

    val conversionResults = remember(inputText, selectedBase) {
        if (inputText.isEmpty() || selectedBase == null) emptyMap()
        else calculateAllBases(inputText, selectedBase)
    }

    // 检测是否为大屏幕模式
    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600

    if (isExpandedScreen) {
        // 大屏幕模式：并排布局
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 左侧：输入区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                InputControlSection(
                    inputText = inputText,
                    inputMode = inputMode,
                    selectedBase = selectedBase,
                    onInputChange = { inputText = it },
                    onModeChange = {
                        inputMode = it
                        keyboardController?.show()
                    },
                    onBaseChange = { manualBase = it },
                    onClear = {
                        inputText = ""
                        manualBase = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (inputText.isNotEmpty()) {
                    BaseIndicator(
                        autoDetectedType = autoDetectedType,
                        autoDetectedBase = autoDetectedBase,
                        selectedBase = selectedBase,
                        onResetAutoDetect = { manualBase = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 右侧：结果区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                if (conversionResults.isNotEmpty()) {
                    ConversionResultsSection(
                        results = conversionResults,
                        onCopy = { value ->
                            val clip = ClipData.newPlainText("转换结果", value)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "已复制: $value", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (inputText.isNotEmpty()) {
                    Text(
                        text = "无法转换：无效的数字格式",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }
    } else {
        // 小屏幕模式：垂直布局
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            InputControlSection(
                inputText = inputText,
                inputMode = inputMode,
                selectedBase = selectedBase,
                onInputChange = { inputText = it },
                onModeChange = {
                    inputMode = it
                    keyboardController?.show()
                },
                onBaseChange = { manualBase = it },
                onClear = {
                    inputText = ""
                    manualBase = null
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (inputText.isNotEmpty()) {
                BaseIndicator(
                    autoDetectedType = autoDetectedType,
                    autoDetectedBase = autoDetectedBase,
                    selectedBase = selectedBase,
                    onResetAutoDetect = { manualBase = null },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (conversionResults.isNotEmpty()) {
                ConversionResultsSection(
                    results = conversionResults,
                    onCopy = { value ->
                        val clip = ClipData.newPlainText("转换结果", value)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制: $value", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (inputText.isNotEmpty()) {
                Text(
                    text = "无法转换：无效的数字格式",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

// 输入模式枚举
enum class InputMode {
    NUMERIC, ALPHANUMERIC
}

// 输入控制区域
@Composable
private fun InputControlSection(
    inputText: String,
    inputMode: InputMode,
    selectedBase: Int?,
    onInputChange: (String) -> Unit,
    onModeChange: (InputMode) -> Unit,
    onBaseChange: (Int?) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 模式选择行 (保持不变)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "输入模式:", style = MaterialTheme.typography.labelLarge)
            InputChip(
                selected = inputMode == InputMode.NUMERIC,
                onClick = { onModeChange(InputMode.NUMERIC) },
                label = { Text("纯数字") }
            )
            InputChip(
                selected = inputMode == InputMode.ALPHANUMERIC,
                onClick = { onModeChange(InputMode.ALPHANUMERIC) },
                label = { Text("字母+数字") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 输入框: 使用 AnimatedContent 包装以实现模式切换时的平滑过渡 ---
        val focusManager = LocalFocusManager.current

        AnimatedContent(
            targetState = inputMode,
            transitionSpec = {
                // 当内容（键盘类型、标签）切换时，使用淡入淡出效果
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            label = "InputModeTransition"
        ) { currentMode ->
            // 注意：OutlinedTextField 内部的逻辑需要根据 currentMode (即 targetState) 来确定
            OutlinedTextField(
                value = inputText,
                onValueChange = { text ->
                    val filtered = when (currentMode) { // 使用 currentMode
                        InputMode.NUMERIC -> text.filter { it.isDigit() }
                        InputMode.ALPHANUMERIC -> text
                            .uppercase()
                            .filter { it.isDigit() || (it in 'A'..'F') }
                    }
                    if (filtered.length <= 16) onInputChange(filtered)
                },
                label = {
                    Text(
                        when (currentMode) { // 使用 currentMode
                            InputMode.NUMERIC -> "输入数字 (0-9)"
                            InputMode.ALPHANUMERIC -> "输入16进制 (0-9, A-F)"
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && inputText.isEmpty()) {
                            focusManager.clearFocus()
                        }
                    },
                keyboardOptions = KeyboardOptions( // 选项也依赖于 currentMode
                    keyboardType = when (currentMode) {
                        InputMode.NUMERIC -> KeyboardType.Number
                        InputMode.ALPHANUMERIC -> KeyboardType.Text
                    },
                    capitalization = when (currentMode) {
                        InputMode.NUMERIC -> KeyboardCapitalization.None
                        InputMode.ALPHANUMERIC -> KeyboardCapitalization.Characters
                    },
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = {
                            onClear()
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 手动进制选择: 使用 AnimatedVisibility 实现平滑的滑入/滑出 ---
        AnimatedVisibility(
            visible = inputText.isNotEmpty(),
            enter = fadeIn(tween(300)) + expandVertically(expandFrom = Alignment.Top, animationSpec = tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(300))
        ) {
            Column {
                Text(text = "手动指定进制:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(2, 8, 10, 16).forEach { base ->
                        FilterChip(
                            selected = selectedBase == base,
                            onClick = { onBaseChange(base) },
                            label = { Text("$base 进制") }
                        )
                    }
                }
            }
        }
    }
}

// 进制识别指示器
@Composable
private fun BaseIndicator(
    autoDetectedType: String,
    autoDetectedBase: Int?,
    selectedBase: Int?,
    onResetAutoDetect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp))  {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "自动识别:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$autoDetectedType (${autoDetectedBase ?: "?"}进制)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight  = FontWeight.Bold)
                )
            }

            if (selectedBase != null && selectedBase != autoDetectedBase) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "当前使用:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${selectedBase}进制",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onResetAutoDetect,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "恢复自动识别",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// 转换结果区域
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversionResultsSection(
    results: Map<String, String>,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "转换结果:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom  = 4.dp)
        )

        results.forEach  { (baseName, value) ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 基础结果展示
                ResultCard(
                    title = baseName,
                    value = value,
                    onCopy = { onCopy(value) },
                    modifier = Modifier.fillMaxWidth()
                )

                // 二进制位分析
                if (baseName == "二进制") {
                    BinaryBitAnalysis(
                        binaryValue = value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// 二进制位分析组件
@Composable
private fun BinaryBitAnalysis(
    binaryValue: String,
    modifier: Modifier = Modifier
) {
    val bitPositionsFromZero = remember(binaryValue) {
        binaryValue
            .reversed() // 从右往左计算位数
            .mapIndexedNotNull { index, c ->
                if (c == '1') index else null // 直接使用 index，从 0 开始计数
            }
            .reversed() // 恢复原始顺序
    }

    val bitPositionsFromOne = remember(bitPositionsFromZero) {
        bitPositionsFromZero.map { it + 1 }
    }

    var showZeroBased by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.clickable { showZeroBased = !showZeroBased }, // 添加点击事件
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "二进制位分析:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (bitPositionsFromZero.isEmpty()) {
                Text(
                    text = "没有为1的位",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                if (showZeroBased) {
                    // 显示从0开始计数
                    Text(
                        text = "从0计数: 第 ${bitPositionsFromZero.joinToString(" 、")} 位为1",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                } else {
                    // 显示从1开始计数
                    Text(
                        text = "从1计数: 第 ${bitPositionsFromOne.joinToString("、")} 位为1",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击切换显示计数方式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// 单个结果卡片
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultCard(
    title: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = {},
                onLongClick = onCopy
            ),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f))  {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha  = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            Text(
                text = "长按复制",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha  = 0.6f)
            )
        }
    }
}

// 进制检测逻辑
private fun detectInputBase(input: String, mode: InputMode): Pair<String, Int?> {
    if (input.isEmpty())  return Pair("未知", null)

    return when {
        input.all  { it in '0'..'1' } -> Pair("二进制", 2)
        mode == InputMode.NUMERIC && input.all  { it.isDigit()  } -> Pair("十进制", 10) // 将十进制判断提前
        input.all  { it in '0'..'7' } -> Pair("八进制", 8)
        input.any  { it in 'A'..'F' } -> Pair("十六进制", 16)
        input.all  { it.isDigit()  } -> Pair("十进制", 10)
        else -> Pair("未知格式", null)
    }
}


// 进制计算逻辑
private fun calculateAllBases(input: String, fromBase: Int): Map<String, String> {
    val decimal = try {
        input.toLong(fromBase)
    } catch (_: NumberFormatException) {
        return emptyMap()
    }

    return mapOf(
        "二进制" to decimal.toString(2),
        "八进制" to decimal.toString(8),
        "十进制" to decimal.toString(),
        "十六进制" to decimal.toString(16).uppercase()
    )
}