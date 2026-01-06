package com.ling.box.calculator

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import com.ling.box.calculator.repository.ElevatorRepository
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ling.box.calculator.model.ElevatorUiState
import com.ling.box.calculator.ui.components.BalanceCoefficientDisplay
import com.ling.box.calculator.ui.components.BalanceWarningSection
import com.ling.box.calculator.ui.components.CurrentChart
import com.ling.box.calculator.ui.components.ElevatorSelector
import com.ling.box.calculator.ui.components.EstimatedBlocksTable
import com.ling.box.calculator.ui.components.StandardPercentageReference
import com.ling.box.calculator.viewmodel.CalculatorMode
import com.ling.box.calculator.viewmodel.ElevatorCalculatorViewModel


// --- UI 入口 Composable ---
@OptIn(ExperimentalAnimationApi::class) // 需要 ExperimentalAnimationApi 注解
@Composable
fun CalculatorScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val calculatorViewModel: ElevatorCalculatorViewModel = remember {
        ElevatorCalculatorViewModel(context.applicationContext as Application)
    }

    // 从 ViewModel 收集当前电梯的 UI 状态
    val uiState by calculatorViewModel.currentElevatorUiState.collectAsStateWithLifecycle()
    val currentElevatorIndex by calculatorViewModel.currentElevatorIndex.collectAsStateWithLifecycle()

    // *** 使用 AnimatedContent 包裹主要内容，根据 currentElevatorIndex 切换 ***
    AnimatedContent(
        targetState = currentElevatorIndex, // 目标状态是当前选中的电梯索引
        transitionSpec = {
            // 使用与之前内容区域相似的淡入淡出 + 缩放动画
            (fadeIn(animationSpec = tween(durationMillis = 350, delayMillis = 50)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 350, delayMillis = 50))) togetherWith
                    (fadeOut(animationSpec = tween(durationMillis = 250)) +
                            scaleOut(targetScale = 0.92f, animationSpec = tween(durationMillis = 250))) using (
                    // 使用 SizeTransform 处理容器尺寸变化
                    SizeTransform(clip = false)
                    )
        },
        label = "ElevatorPageTransition" // 添加 label
    ) { targetElevatorIndex ->
        // 在 AnimatedContent 内部，根据 targetElevatorIndex 显示对应的页面内容
        // 这里我们始终显示 ElevatorBalanceCalculatorScreen，但它会接收到对应索引的 uiState
        // ViewModel 会根据 currentElevatorIndex 提供正确的 uiState
        ElevatorBalanceCalculatorScreen(
            uiState = uiState, // uiState 会随着 ViewModel 的状态变化而变化
            currentElevatorIndex = targetElevatorIndex, // 将 AnimatedContent 的 target 索引传递下去
            onElevatorSelect = { index -> calculatorViewModel.selectElevator(index) },
            onRatedLoadChange = { value -> calculatorViewModel.updateRatedLoad(value) },
            onCounterweightChange = { value -> calculatorViewModel.updateCounterweightWeight(value) },
            onBlockWeightChange = { value -> calculatorViewModel.updateCounterweightBlockWeight(value) },
            onManualKChange = { value -> calculatorViewModel.updateManualBalanceCoefficientK(value) },
            onToggleManualBalance = { checked -> calculatorViewModel.toggleManualBalance(checked) },
            onToggleCustomInput = { checked -> calculatorViewModel.toggleCustomBlockInput(checked) },
            onCustomBlockCountChange = { index, value -> calculatorViewModel.updateCustomBlockCount(index, value) },
            onCurrentReadingChange = { dirIndex, pointIndex, value -> calculatorViewModel.updateCurrentReading(dirIndex, pointIndex, value) },
            onClearData = { calculatorViewModel.clearCurrentElevatorData() },
            onAddCustomBlockSlot = { calculatorViewModel.addCustomBlockCountSlot() },
            onRemoveCustomBlockSlot = { index -> calculatorViewModel.removeCustomBlockCountSlot(index) },
            onAddStandardBlockSlot = { blocks -> calculatorViewModel.addStandardBlockSlot(blocks) }, // **新增回调**
            viewModel = calculatorViewModel, // 将 ViewModel 实例传递给 ElevatorBalanceCalculatorScreen
            contentPadding = paddingValues
        )
    }
}


// --- 主屏幕 UI (现在是无状态的，接收 UiState 和事件回调) ---
@OptIn(ExperimentalMaterial3Api::class) // 确保这个 OptIn 还在
@Composable
fun ElevatorBalanceCalculatorScreen(
    uiState: ElevatorUiState,
    currentElevatorIndex: Int,
    onElevatorSelect: (Int) -> Unit,
    onRatedLoadChange: (String) -> Unit,
    onCounterweightChange: (String) -> Unit,
    onBlockWeightChange: (String) -> Unit,
    onManualKChange: (String?) -> Unit,
    onToggleManualBalance: (Boolean) -> Unit,
    onToggleCustomInput: (Boolean) -> Unit,
    onCustomBlockCountChange: (index: Int, value: String) -> Unit,
    onCurrentReadingChange: (directionIndex: Int, pointIndex: Int, value: String) -> Unit,
    onClearData: () -> Unit,
    onAddCustomBlockSlot: () -> Unit,
    onRemoveCustomBlockSlot: (Int) -> Unit,
    onAddStandardBlockSlot: (Int) -> Unit,
    viewModel: ElevatorCalculatorViewModel,
    contentPadding: PaddingValues // 接收 Scaffold 传递的内边距
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val defaultLoadPercentages = remember { listOf(30, 40, 45, 50, 60) }
    val currentMode = remember(uiState.useCustomBlockInput, uiState.useManualBalance) {
        when {
            uiState.useManualBalance -> CalculatorMode.MANUAL_K
            else -> CalculatorMode.CUSTOM_BLOCKS
        }
    }
    val selectedAlgorithm by viewModel.selectedAlgorithm.collectAsStateWithLifecycle()
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    // 平衡系数范围设置
    val repository = remember { ElevatorRepository(context) }
    val balanceRangeMin = repository.getBalanceRangeMin().toDouble()
    val balanceRangeMax = repository.getBalanceRangeMax().toDouble()

    val layoutDirection = LocalLayoutDirection.current
    Surface(
        modifier = Modifier
            .fillMaxSize()
            // 关键修复：在 Surface 级别应用底部 padding，防止 Surface 填充到底部导航栏空间
            // 这样长截图时就不会包含底部导航栏的空间，避免重复截断
            .padding(bottom = contentPadding.calculateBottomPadding()),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    // 移除顶部 padding，TopAppBar 会自动处理状态栏间距
                    // 只应用水平方向的 padding
                    start = 16.dp + contentPadding.calculateStartPadding(layoutDirection),
                    end = 16.dp + contentPadding.calculateEndPadding(layoutDirection)
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 在可滚动内容开头添加 TopAppBar，这样截图时可以包含标题
            TopAppBar(
                title = {
                    Text(
                        "平衡系数计算",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                // TopAppBar 会自动处理状态栏的 windowInsets，不需要手动添加顶部 padding
                windowInsets = TopAppBarDefaults.windowInsets,
                modifier = Modifier.fillMaxWidth()
            )

            ElevatorSelector(
                viewModel = viewModel,
                onElevatorSelect = onElevatorSelect
            )

            Spacer(modifier = Modifier.height(16.dp))

            val modes = listOf(CalculatorMode.CUSTOM_BLOCKS, CalculatorMode.MANUAL_K)
            val selectedIndex = remember(currentMode) { modes.indexOf(currentMode) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                modes.forEachIndexed { index, mode ->
                    val isSelected = index == selectedIndex

                    val cornerRadius = animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 16.dp,
                        animationSpec = tween(durationMillis = 200),
                        label = "cornerRadiusAnimation"
                    ).value

                    val buttonShape: Shape = RoundedCornerShape(cornerRadius)

                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        animationSpec = tween(durationMillis = 200),
                        label = "containerColorAnimation"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 200),
                        label = "contentColorAnimation"
                    )

                    val animatedZIndex by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "zIndexAnimation"
                    )

                    Button(
                        onClick = {
                            when (mode) {
                                CalculatorMode.CUSTOM_BLOCKS -> {
                                    onToggleCustomInput(true)
                                    onToggleManualBalance(false)
                                }
                                CalculatorMode.MANUAL_K -> {
                                    onToggleManualBalance(true)
                                }
                            }
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .then(Modifier.zIndex(animatedZIndex)),
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = contentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = when (mode) {
                                CalculatorMode.CUSTOM_BLOCKS -> "自动"
                                CalculatorMode.MANUAL_K -> "手动"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.5.sp),
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 入场动画
            val inputAlpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300),
                label = "inputAlpha"
            )

            val inputTranslationY by animateDpAsState(
                targetValue = 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "inputTranslationY"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = inputAlpha
                        translationY = inputTranslationY.toPx()
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 第一行：额定载重 + 砝码重量
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ratedLoadOptions = remember { listOf("630", "825", "1000", "1050", "1350", "1600", "1800", "2000", "3000") }
                    var ratedLoadExpanded by remember { mutableStateOf(false) }

                    // 额定载重下拉框
                    Box(modifier = Modifier.weight(1f)) {
                        val focusAlpha by animateFloatAsState(
                            targetValue = if (ratedLoadExpanded) 1f else 0.8f,
                            animationSpec = tween(durationMillis = 200),
                            label = "focusAlpha"
                        )

                        ExposedDropdownMenuBox(
                            expanded = ratedLoadExpanded,
                            onExpandedChange = { ratedLoadExpanded = !ratedLoadExpanded },
                        ) {
                            OutlinedTextField(
                                value = uiState.ratedLoad,
                                onValueChange = onRatedLoadChange,
                                label = {
                                    Text(
                                        "额定载重",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                singleLine = true,
                                suffix = {
                                    Text(
                                        "kg",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = ratedLoadExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(
                                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = true
                                    )
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = focusAlpha },
                                shape = MaterialTheme.shapes.medium,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                                ),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = ratedLoadExpanded,
                                onDismissRequest = { ratedLoadExpanded = false },
                                modifier = Modifier
                                    .exposedDropdownSize()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.shapes.medium
                                    )
                            ) {
                                ratedLoadOptions.forEachIndexed { index, selectionOption ->
                                    val itemAlpha by animateFloatAsState(
                                        targetValue = if (ratedLoadExpanded) 1f else 0f,
                                        animationSpec = tween(
                                            durationMillis = 150,
                                            delayMillis = index * 20
                                        ),
                                        label = "itemAlpha_$index"
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                selectionOption,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        onClick = {
                                            onRatedLoadChange(selectionOption)
                                            ratedLoadExpanded = false
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier.graphicsLayer { alpha = itemAlpha },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 砝码重量输入框
                    var isBlockWeightFocused by remember { mutableStateOf(false) }
                    val blockWeightScale by animateFloatAsState(
                        targetValue = if (isBlockWeightFocused) 1.02f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "blockWeightScale"
                    )

                    OutlinedTextField(
                        value = uiState.counterweightBlockWeight,
                        onValueChange = onBlockWeightChange,
                        label = {
                            Text(
                                "砝码重量",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        suffix = {
                            Text(
                                "kg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = blockWeightScale
                                scaleY = blockWeightScale
                            }
                            .onFocusChanged { isBlockWeightFocused = it.isFocused },
                        shape = MaterialTheme.shapes.medium,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
                            focusedLabelColor = MaterialTheme.colorScheme.tertiary,
                            cursorColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.08f)
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // 第二行：对重重量（独占一行）
                var isCounterweightFocused by remember { mutableStateOf(false) }
                val counterweightScale by animateFloatAsState(
                    targetValue = if (isCounterweightFocused) 1.02f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "counterweightScale"
                )

                OutlinedTextField(
                    value = uiState.counterweightWeight,
                    onValueChange = onCounterweightChange,
                    label = {
                        Text(
                            "对重重量",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    suffix = {
                        Text(
                            "kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = counterweightScale
                            scaleY = counterweightScale
                        }
                        .onFocusChanged { isCounterweightFocused = it.isFocused },
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.08f)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            BalanceCoefficientDisplay(
                useManualBalance = uiState.useManualBalance,
                manualBalanceCoefficientK = uiState.manualBalanceCoefficientK,
                balanceCoefficientK = uiState.balanceCoefficientK,
                upwardCurrentPoints = uiState.upwardCurrentPoints,
                downwardCurrentPoints = uiState.downwardCurrentPoints,
                hasActualIntersection = uiState.hasActualIntersection,
                balanceRangeMin = balanceRangeMin,
                balanceRangeMax = balanceRangeMax
            )

            if (currentMode == CalculatorMode.CUSTOM_BLOCKS) {
                StandardPercentageReference(
                    ratedLoadValue = uiState.ratedLoad.toDoubleOrNull() ?: 0.0,
                    blockWeightValue = uiState.counterweightBlockWeight.toDoubleOrNull() ?: 0.0,
                    defaultLoadPercentages = defaultLoadPercentages,
                    onBlockCountClick = onAddStandardBlockSlot
                )
            }

            when (currentMode) {
                CalculatorMode.MANUAL_K -> {
                    // 简洁的入场动画
                    val alpha by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 200),
                        label = "alpha"
                    )

                    OutlinedTextField(
                        value = uiState.manualBalanceCoefficientK ?: "",
                        onValueChange = { newValue ->
                            onManualKChange(newValue.ifEmpty { null })
                        },
                        label = { Text("平衡系数 (%)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .graphicsLayer { this.alpha = alpha },
                        shape = MaterialTheme.shapes.medium,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
                CalculatorMode.CUSTOM_BLOCKS -> {
                    // 轻量级入场动画
                    val cardAlpha by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 200),
                        label = "cardAlpha"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                            .graphicsLayer { alpha = cardAlpha },
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 3.dp,
                            hoveredElevation = 4.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 16.dp)
                        ) {
                            // 美化的标题
                            Text(
                                "上下行电流 (A)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )

                            // **新的横向结构：最外层 Row 用于横向滚动**
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()), // 允许整体横向滚动
                                horizontalArrangement = Arrangement.spacedBy(8.dp), // 列之间的间距
                                verticalAlignment = Alignment.Top // 确保列内容顶部对齐
                            ) {
                                uiState.customBlockCounts.forEachIndexed { index, count ->
                                    // 每个 index 代表一个“块”的数据列
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp), // 输入框之间的垂直间距
                                        modifier = Modifier.width(90.dp) // 固定每列的宽度，确保对齐
                                    ) {
                                        // 砝码块数输入框
                                        OutlinedTextField(
                                            value = count,
                                            onValueChange = { newValue ->
                                                onCustomBlockCountChange(index, newValue)
                                            },
                                            modifier = Modifier.fillMaxWidth(), // 填充列的宽度
                                            shape = MaterialTheme.shapes.medium,
                                            colors = TextFieldDefaults.colors(
                                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                cursorColor = MaterialTheme.colorScheme.primary,
                                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Next
                                            ),
                                            singleLine = true,
                                            label = {
                                                Text(
                                                    "块${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )

                                        // 上行电流输入框
                                        val upwardCurrent = uiState.currentReadings.getOrNull(0)?.getOrNull(index) ?: ""
                                        OutlinedTextField(
                                            value = upwardCurrent,
                                            onValueChange = { newValue ->
                                                onCurrentReadingChange(0, index, newValue)
                                            },
                                            modifier = Modifier.fillMaxWidth(), // 填充列的宽度
                                            shape = MaterialTheme.shapes.medium,
                                            colors = TextFieldDefaults.colors(
                                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                cursorColor = MaterialTheme.colorScheme.primary,
                                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Next
                                            ),
                                            singleLine = true,
                                            label = {
                                                Text(
                                                    "上行",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )

                                        // 下行电流输入框
                                        val downwardCurrent = uiState.currentReadings.getOrNull(1)?.getOrNull(index) ?: ""
                                        OutlinedTextField(
                                            value = downwardCurrent,
                                            onValueChange = { newValue ->
                                                onCurrentReadingChange(1, index, newValue)
                                            },
                                            modifier = Modifier.fillMaxWidth(), // 填充列的宽度
                                            shape = MaterialTheme.shapes.medium,
                                            colors = TextFieldDefaults.colors(
                                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                cursorColor = MaterialTheme.colorScheme.primary,
                                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done // 最后一项可以是 Done
                                            ),
                                            singleLine = true,
                                            label = {
                                                Text(
                                                    "下行",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )

                                        // 删除按钮 (现在位于每个 Column 的底部)
                                        if (uiState.customBlockCounts.size > 1) {
                                            IconButton(
                                                onClick = { onRemoveCustomBlockSlot(index) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Remove,
                                                    contentDescription = "删除当前列",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            // 操作按钮 - 美化的横向布局
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 添加按钮
                                var isAddPressed by remember { mutableStateOf(false) }
                                val addScale by animateFloatAsState(
                                    targetValue = if (isAddPressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "addScale"
                                )

                                FilledTonalButton(
                                    onClick = onAddCustomBlockSlot,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .graphicsLayer {
                                            scaleX = addScale
                                            scaleY = addScale
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isAddPressed = true
                                                    tryAwaitRelease()
                                                    isAddPressed = false
                                                }
                                            )
                                        },
                                    shape = MaterialTheme.shapes.large,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "添加",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                                // 清空按钮
                                var isClearPressed by remember { mutableStateOf(false) }
                                val clearScale by animateFloatAsState(
                                    targetValue = if (isClearPressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "clearScale"
                                )

                                OutlinedButton(
                                    onClick = { showClearDataDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .graphicsLayer {
                                            scaleX = clearScale
                                            scaleY = clearScale
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isClearPressed = true
                                                    tryAwaitRelease()
                                                    isClearPressed = false
                                                }
                                            )
                                        },
                                    shape = MaterialTheme.shapes.large,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(
                                        "清空",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // **清空确认对话框**
            if (showClearDataDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDataDialog = false }, // 点击对话框外部或返回键时隐藏
                    title = { Text("确认清空？") },
                    text = { Text("您确定要清空所有此电梯已输入的电流数据吗？") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onClearData() // 执行清空操作
                                showClearDataDialog = false // 隐藏对话框
                            }
                        ) {
                            Text("清空")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showClearDataDialog = false } // 隐藏对话框
                        ) {
                            Text("取消")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BalanceWarningSection(
                recommendedBlocksMessage = uiState.recommendedBlocksMessage,
                displayBalanceCoefficient = uiState.balanceCoefficientK,
                balanceRangeMin = balanceRangeMin,
                balanceRangeMax = balanceRangeMax
            )

            uiState.balanceCoefficient?.let { totalPercent ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "单对重百分比 = %.2f %%".format(totalPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 实时曲线图
            if (currentMode != CalculatorMode.MANUAL_K) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("实时曲线图", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrentChart(
                        upwardCurrentPoints = uiState.upwardCurrentPoints, // 传递不可变列表
                        downwardCurrentPoints = uiState.downwardCurrentPoints, // 传递不可变列表
                        useCustomBlockInput = uiState.useCustomBlockInput, // Chart 内部可能需要这个来区分 x 轴标签
                        defaultLoadPercentages = defaultLoadPercentages, // 传递常量列表
                        algorithm = selectedAlgorithm, // 传递当前选择的算法
                        r2Value = uiState.linearRegressionR2, // 传递R²值
                        modifier = Modifier.fillMaxWidth().height(300.dp) //.padding(8.dp) Chart 内部已有 padding
                    )
                }
            }

            // 预计砝码块数表格
            if (currentMode != CalculatorMode.MANUAL_K) {
                Text("预计砝码块数与实际载荷", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

                // 只有在计算可行 (isCalculationPossible) 时才显示表格
                if (uiState.isCalculationPossible) {
                    EstimatedBlocksTable(
                        tableData = uiState.estimatedBlocksTableData, // 使用 ViewModel 计算好的数据
                        // useCustomInput = uiState.useCustomBlockInput
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp)) // 底部额外空间
        }
    }
}
