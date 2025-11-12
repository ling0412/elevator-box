package com.ling.box.number.safety.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ling.box.number.safety.data.ElevatorData
import com.ling.box.number.safety.utils.calculateMinPitVerticalDistance

private data class InputItem(
    val label: String,
    val value: Float,
    val updateData: (Float) -> ElevatorData
)

@Composable
private fun InputCard(
    title: String,
    items: List<InputItem>,
    onDataChange: (ElevatorData) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            items.forEach { item ->
                NumberInputField(
                    label = item.label,
                    initialValue = item.value,
                    onValueChange = { floatValue -> onDataChange(item.updateData(floatValue)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun InputCardHoistwayParams(
    title: String,
    elevatorData: ElevatorData,
    onDataChange: (ElevatorData) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            NumberInputField(label = "上端站对重缓冲距 h₁ (mm)", initialValue = elevatorData.bufferDistance, onValueChange = { onDataChange(elevatorData.copy(bufferDistance = it)) })
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(label = "下端站轿厢缓冲距 g (mm)", initialValue = elevatorData.inputCarOvertravel, onValueChange = { onDataChange(elevatorData.copy(inputCarOvertravel = it)) })
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(label = "上极限距离 m (mm)", initialValue = elevatorData.upperLimitDistance, onValueChange = { onDataChange(elevatorData.copy(upperLimitDistance = it)) })
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(label = "下极限距离 n (mm)", initialValue = elevatorData.lowerLimitDistance, onValueChange = { onDataChange(elevatorData.copy(lowerLimitDistance = it)) })
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(label = "主导轨距 (m)", initialValue = elevatorData.mainGuideDistance, onValueChange = { onDataChange(elevatorData.copy(mainGuideDistance = it)) })
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(label = "副导轨距 (m)", initialValue = elevatorData.auxGuideDistance, onValueChange = { onDataChange(elevatorData.copy(auxGuideDistance = it)) })

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "注：极限距离 (m, n) 不得大于对应的缓冲距 (h₁, g)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun InputCardPitParams(
    title: String,
    elevatorData: ElevatorData,
    onDataChange: (ElevatorData) -> Unit
) {
    val calculatedMinVertical = remember(elevatorData.lowestHorizontalDistance) {
        calculateMinPitVerticalDistance(elevatorData.lowestHorizontalDistance)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            NumberInputField(label = "最低部件水平距离 (m)", initialValue = elevatorData.lowestHorizontalDistance, onValueChange = { onDataChange(elevatorData.copy(lowestHorizontalDistance = it)) })
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(label = "最低部件垂直距离 (m)", initialValue = elevatorData.lowestVerticalDistance, onValueChange = { onDataChange(elevatorData.copy(lowestVerticalDistance = it)) })
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(label = "底坑最高距离 (m)", initialValue = elevatorData.pitHighestDistance, onValueChange = { onDataChange(elevatorData.copy(pitHighestDistance = it)) })

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "计算: 允许的最小底坑垂直距离: %.3f m".format(calculatedMinVertical),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "注：底坑最高部件垂直距离不得小于0.3m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun InputCardMiscParams(
    title: String,
    elevatorData: ElevatorData,
    onDataChange: (ElevatorData) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            IntegerInputField(label = "铁块对重 e₁ (块)", initialValue = elevatorData.ironCounterweight, onValueChange = { onDataChange(elevatorData.copy(ironCounterweight = it)) })
            Spacer(modifier = Modifier.height(8.dp))
            IntegerInputField(label = "水泥块对重 e₂ (块)", initialValue = elevatorData.concreteCounterweight, onValueChange = { onDataChange(elevatorData.copy(concreteCounterweight = it)) })
            Spacer(modifier = Modifier.height(8.dp))
            NumberInputField(label = "对重块高度 e₃ (m)", initialValue = elevatorData.counterweightHeight, onValueChange = { onDataChange(elevatorData.copy(counterweightHeight = it)) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSection(
    elevatorData: ElevatorData,
    onDataChange: (ElevatorData) -> Unit
) {
    val speedOptions = remember {
        listOf(
            "30 (0.5 m/s)" to 0.5f, "60 (1.0 m/s)" to 1.0f, "90 (1.5 m/s)" to 1.5f,
            "105 (1.75 m/s)" to 1.75f, "120 (2.0 m/s)" to 2.0f, "150 (2.5 m/s)" to 2.5f,
            "180 (3.0 m/s)" to 3.0f, "210 (3.5 m/s)" to 3.5f, "240 (4.0 m/s)" to 4.0f,
            "300 (5.0 m/s)" to 5.0f, "360 (6.0 m/s)" to 6.0f
        )
    }
    var speedDropdownExpanded by remember { mutableStateOf(false) }
    val currentSpeedText = speedOptions.find { it.second == elevatorData.speed }?.first
        ?: speedOptions.firstOrNull()?.first ?: ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = speedDropdownExpanded,
            onExpandedChange = { speedDropdownExpanded = !speedDropdownExpanded },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = currentSpeedText,
                onValueChange = {},
                label = { Text("额定速度 v") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speedDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = speedDropdownExpanded,
                onDismissRequest = { speedDropdownExpanded = false }
            ) {
                speedOptions.forEach { (text, value) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onDataChange(elevatorData.copy(speed = value))
                            speedDropdownExpanded = false
                        }
                    )
                }
            }
        }

        InputCard(
            title = "1. 导轨及行程参数",
            items = listOf(
                InputItem("轿厢导轨终止 S₁ (m)", elevatorData.carGuideTravel) { elevatorData.copy(carGuideTravel = it) },
                InputItem("站人高度 S₂ (m)", elevatorData.standingHeightTravel) { elevatorData.copy(standingHeightTravel = it) },
                InputItem("最高部件a S₃ (m)", elevatorData.highestComponentTravelA) { elevatorData.copy(highestComponentTravelA = it) },
                InputItem("最高部件b S₄ (m)", elevatorData.highestComponentTravelB) { elevatorData.copy(highestComponentTravelB = it) },
                InputItem("对重导轨终止 S₅ (m)", elevatorData.counterweightGuideTravel) { elevatorData.copy(counterweightGuideTravel = it) }
            ),
            onDataChange = onDataChange
        )

        InputCard(
            title = "2. 基本参数",
            items = listOf(
                InputItem("对重压缩行程 h (m)", elevatorData.bufferCompression) { elevatorData.copy(bufferCompression = it) },
                InputItem("轿厢压缩行程 k (m)", elevatorData.carBufferCompression) { elevatorData.copy(carBufferCompression = it) }
            ),
            onDataChange = onDataChange
        )

        InputCardHoistwayParams(
            title = "3. 井道尺寸参数",
            elevatorData = elevatorData,
            onDataChange = onDataChange
        )

        InputCardPitParams(
            title = "4. 底坑参数",
            elevatorData = elevatorData,
            onDataChange = onDataChange
        )

        InputCardMiscParams(
            title = "5. 杂项参数",
            elevatorData = elevatorData,
            onDataChange = onDataChange
        )
    }
}

