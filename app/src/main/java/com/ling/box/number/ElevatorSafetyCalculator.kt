package com.ling.box.number

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ling.box.number.safety.components.InputSection
import com.ling.box.number.safety.components.LoadDialog
import com.ling.box.number.safety.components.ResultSection
import com.ling.box.number.safety.components.SaveDialog
import com.ling.box.number.safety.data.CalculationResult
import com.ling.box.number.safety.data.ElevatorData
import com.ling.box.number.safety.data.hasValidInput
import com.ling.box.number.safety.utils.PrefsHelper
import com.ling.box.number.safety.utils.calculateMaximumOvertravel

@Composable
fun ElevatorSafetyCalculator() {
    val context = LocalContext.current
    var elevatorData by remember { mutableStateOf(ElevatorData()) }
    var savedElevators by remember { mutableStateOf<Map<String, ElevatorData>>(emptyMap()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        savedElevators = PrefsHelper.loadElevatorMap(context)
    }

    val calculationResult by remember {
        derivedStateOf {
            if (elevatorData.hasValidInput()) {
                calculateMaximumOvertravel(elevatorData)
            } else {
                CalculationResult(
                    a = 0f, b = 0f, c = 0f, d = 0f, e = 0f,
                    maxOvertravel = 0f,
                    limitingCondition = "请选择额定速度",
                    isComplete = false
                )
            }
        }
    }

    // 检测是否为大屏幕模式
    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InputSection(
                        elevatorData = elevatorData,
                        onDataChange = { newData -> elevatorData = newData }
                    )
                }

                // 右侧：结果区域和按钮
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ResultSection(
                        result = calculationResult,
                        elevatorData = elevatorData
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("保存") }

                        Button(
                            onClick = { showLoadDialog = true },
                            enabled = savedElevators.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("加载/管理") }

                        Button(
                            onClick = { elevatorData = ElevatorData() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) { Text("清空输入") }
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

                InputSection(
                    elevatorData = elevatorData,
                    onDataChange = { newData -> elevatorData = newData }
                )

                Spacer(modifier = Modifier.height(24.dp))

                ResultSection(
                    result = calculationResult,
                    elevatorData = elevatorData
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("保存") }

                    Button(
                        onClick = { showLoadDialog = true },
                        enabled = savedElevators.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("加载/管理") }

                    Button(
                        onClick = { elevatorData = ElevatorData() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text("清空输入") }
                }
            }
        }

        if (showSaveDialog) {
            SaveDialog(
                onDismiss = { showSaveDialog = false },
                onSave = { name ->
                    if (name.isNotBlank()) {
                        val newMap = savedElevators.toMutableMap().apply { put(name, elevatorData) }
                        savedElevators = newMap
                        PrefsHelper.saveElevatorMap(context, newMap)
                        showSaveDialog = false
                    }
                }
            )
        }

        if (showLoadDialog) {
            LoadDialog(
                savedElevatorsMap = savedElevators,
                onDismiss = { showLoadDialog = false },
                onLoad = { data ->
                    elevatorData = data
                    showLoadDialog = false
                },
                onDelete = { nameToDelete ->
                    val newMap = savedElevators.toMutableMap().apply { remove(nameToDelete) }
                    savedElevators = newMap
                    PrefsHelper.saveElevatorMap(context, newMap)
                }
            )
        }
    }
}
