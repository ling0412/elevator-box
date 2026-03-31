package com.ling.box.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ling.box.R
import com.ling.box.calculator.model.ElevatorUiState
import com.ling.box.calculator.repository.ElevatorRepository
import com.ling.box.calculator.ui.components.BalanceCoefficientDisplay
import com.ling.box.calculator.ui.components.BalanceWarningSection
import com.ling.box.calculator.ui.components.CalculatorModeSelector
import com.ling.box.calculator.ui.components.CurrentChart
import com.ling.box.calculator.ui.components.CurrentReadingsCard
import com.ling.box.calculator.ui.components.ElevatorInputFields
import com.ling.box.calculator.ui.components.ElevatorSelector
import com.ling.box.calculator.ui.components.EstimatedBlocksTable
import com.ling.box.calculator.ui.components.StandardPercentageReference
import com.ling.box.calculator.viewmodel.CalculatorMode
import com.ling.box.calculator.viewmodel.ElevatorCalculatorViewModel


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalculatorScreen(paddingValues: PaddingValues) {
    val calculatorViewModel: ElevatorCalculatorViewModel = viewModel()

    val uiState by calculatorViewModel.currentElevatorUiState.collectAsStateWithLifecycle()
    val currentElevatorIndex by calculatorViewModel.currentElevatorIndex.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = currentElevatorIndex,
        transitionSpec = {
            (fadeIn(animationSpec = tween(durationMillis = 350, delayMillis = 50)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 350, delayMillis = 50))) togetherWith
                    (fadeOut(animationSpec = tween(durationMillis = 250)) +
                            scaleOut(targetScale = 0.92f, animationSpec = tween(durationMillis = 250))) using (
                    SizeTransform(clip = false)
                    )
        },
        label = "ElevatorPageTransition"
    ) { targetElevatorIndex ->
        ElevatorBalanceCalculatorScreen(
            uiState = uiState,
            currentElevatorIndex = targetElevatorIndex,
            onElevatorSelect = { calculatorViewModel.selectElevator(it) },
            onRatedLoadChange = { calculatorViewModel.updateRatedLoad(it) },
            onCounterweightChange = { calculatorViewModel.updateCounterweightWeight(it) },
            onBlockWeightChange = { calculatorViewModel.updateCounterweightBlockWeight(it) },
            onManualKChange = { calculatorViewModel.updateManualBalanceCoefficientK(it) },
            onToggleManualBalance = { calculatorViewModel.toggleManualBalance(it) },
            onToggleCustomInput = { calculatorViewModel.toggleCustomBlockInput(it) },
            onCustomBlockCountChange = { index, value -> calculatorViewModel.updateCustomBlockCount(index, value) },
            onCurrentReadingChange = { dir, point, value -> calculatorViewModel.updateCurrentReading(dir, point, value) },
            onClearData = { calculatorViewModel.clearCurrentElevatorData() },
            onAddCustomBlockSlot = { calculatorViewModel.addCustomBlockCountSlot() },
            onRemoveCustomBlockSlot = { calculatorViewModel.removeCustomBlockCountSlot(it) },
            onAddStandardBlockSlot = { calculatorViewModel.addStandardBlockSlot(it) },
            viewModel = calculatorViewModel,
            contentPadding = paddingValues
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
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
    contentPadding: PaddingValues
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

    val repository = remember { ElevatorRepository(context) }
    val balanceRangeMin = repository.getBalanceRangeMin().toDouble()
    val balanceRangeMax = repository.getBalanceRangeMax().toDouble()

    val layoutDirection = LocalLayoutDirection.current
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding()),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp + contentPadding.calculateStartPadding(layoutDirection),
                    end = 16.dp + contentPadding.calculateEndPadding(layoutDirection)
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(
                title = { Text(stringResource(R.string.title_balance_calculation), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = TopAppBarDefaults.windowInsets,
                modifier = Modifier.fillMaxWidth()
            )

            ElevatorSelector(viewModel = viewModel, onElevatorSelect = onElevatorSelect)

            Spacer(modifier = Modifier.height(16.dp))

            CalculatorModeSelector(
                currentMode = currentMode,
                onModeChange = { mode ->
                    when (mode) {
                        CalculatorMode.CUSTOM_BLOCKS -> {
                            onToggleCustomInput(true)
                            onToggleManualBalance(false)
                        }
                        CalculatorMode.MANUAL_K -> onToggleManualBalance(true)
                    }
                    focusManager.clearFocus()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            val inputAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(300), label = "inputAlpha")
            val inputTranslationY by animateDpAsState(
                targetValue = 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "inputTranslationY"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = inputAlpha; translationY = inputTranslationY.toPx() },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElevatorInputFields(
                    ratedLoad = uiState.ratedLoad,
                    counterweightBlockWeight = uiState.counterweightBlockWeight,
                    counterweightWeight = uiState.counterweightWeight,
                    onRatedLoadChange = onRatedLoadChange,
                    onBlockWeightChange = onBlockWeightChange,
                    onCounterweightChange = onCounterweightChange,
                    focusManager = focusManager
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
                    val alpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(200), label = "alpha")
                    OutlinedTextField(
                        value = uiState.manualBalanceCoefficientK ?: "",
                        onValueChange = { onManualKChange(it.ifEmpty { null }) },
                        label = { Text(stringResource(R.string.label_balance_coefficient_percent)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
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
                    CurrentReadingsCard(
                        uiState = uiState,
                        onCustomBlockCountChange = onCustomBlockCountChange,
                        onCurrentReadingChange = onCurrentReadingChange,
                        onAddCustomBlockSlot = onAddCustomBlockSlot,
                        onRemoveCustomBlockSlot = onRemoveCustomBlockSlot,
                        onClearRequest = { showClearDataDialog = true }
                    )
                }
            }

            if (showClearDataDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDataDialog = false },
                    title = { Text(stringResource(R.string.dialog_confirm_clear_title)) },
                    text = { Text(stringResource(R.string.dialog_confirm_clear_message)) },
                    confirmButton = {
                        TextButton(onClick = { onClearData(); showClearDataDialog = false }) {
                            Text(stringResource(R.string.clear))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDataDialog = false }) { Text(stringResource(R.string.cancel)) }
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
                    text = stringResource(R.string.label_single_balance_percent, totalPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (currentMode != CalculatorMode.MANUAL_K) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.title_realtime_chart), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrentChart(
                        upwardCurrentPoints = uiState.upwardCurrentPoints,
                        downwardCurrentPoints = uiState.downwardCurrentPoints,
                        useCustomBlockInput = uiState.useCustomBlockInput,
                        defaultLoadPercentages = defaultLoadPercentages,
                        algorithm = selectedAlgorithm,
                        r2Value = uiState.linearRegressionR2,
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    )
                }
            }

            if (currentMode != CalculatorMode.MANUAL_K) {
                Text(
                    stringResource(R.string.title_estimated_blocks),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (uiState.isCalculationPossible) {
                    EstimatedBlocksTable(tableData = uiState.estimatedBlocksTableData)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
