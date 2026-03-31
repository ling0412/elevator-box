package com.ling.box.calculator.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ling.box.calculator.model.ElevatorUiState

@Composable
fun CurrentReadingsCard(
    uiState: ElevatorUiState,
    onCustomBlockCountChange: (index: Int, value: String) -> Unit,
    onCurrentReadingChange: (directionIndex: Int, pointIndex: Int, value: String) -> Unit,
    onAddCustomBlockSlot: () -> Unit,
    onRemoveCustomBlockSlot: (Int) -> Unit,
    onClearRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 200),
        label = "cardAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .graphicsLayer { alpha = cardAlpha },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, hoveredElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Text(
                "上下行电流 (A)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                uiState.customBlockCounts.forEachIndexed { index, count ->
                    CurrentReadingColumn(
                        index = index,
                        blockCount = count,
                        upwardCurrent = uiState.currentReadings.getOrNull(0)?.getOrNull(index) ?: "",
                        downwardCurrent = uiState.currentReadings.getOrNull(1)?.getOrNull(index) ?: "",
                        showDelete = uiState.customBlockCounts.size > 1,
                        onBlockCountChange = { onCustomBlockCountChange(index, it) },
                        onUpwardChange = { onCurrentReadingChange(0, index, it) },
                        onDownwardChange = { onCurrentReadingChange(1, index, it) },
                        onRemove = { onRemoveCustomBlockSlot(index) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ActionButtons(
                onAdd = onAddCustomBlockSlot,
                onClear = onClearRequest
            )
        }
    }
}

@Composable
private fun CurrentReadingColumn(
    index: Int,
    blockCount: String,
    upwardCurrent: String,
    downwardCurrent: String,
    showDelete: Boolean,
    onBlockCountChange: (String) -> Unit,
    onUpwardChange: (String) -> Unit,
    onDownwardChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val textFieldColors = TextFieldDefaults.colors(
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedContainerColor = MaterialTheme.colorScheme.surface
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(90.dp)
    ) {
        OutlinedTextField(
            value = blockCount,
            onValueChange = onBlockCountChange,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
            label = { Text("块${index + 1}", style = MaterialTheme.typography.labelSmall) }
        )

        OutlinedTextField(
            value = upwardCurrent,
            onValueChange = onUpwardChange,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
            label = { Text("上行", style = MaterialTheme.typography.labelSmall) }
        )

        OutlinedTextField(
            value = downwardCurrent,
            onValueChange = onDownwardChange,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            singleLine = true,
            label = { Text("下行", style = MaterialTheme.typography.labelSmall) }
        )

        if (showDelete) {
            IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "删除当前列",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onAdd: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var isAddPressed by remember { mutableStateOf(false) }
        val addScale by animateFloatAsState(
            targetValue = if (isAddPressed) 0.96f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "addScale"
        )

        FilledTonalButton(
            onClick = onAdd,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .graphicsLayer { scaleX = addScale; scaleY = addScale }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { isAddPressed = true; tryAwaitRelease(); isAddPressed = false })
                },
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("添加", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
        }

        var isClearPressed by remember { mutableStateOf(false) }
        val clearScale by animateFloatAsState(
            targetValue = if (isClearPressed) 0.96f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "clearScale"
        )

        OutlinedButton(
            onClick = onClear,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .graphicsLayer { scaleX = clearScale; scaleY = clearScale }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { isClearPressed = true; tryAwaitRelease(); isClearPressed = false })
                },
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Text("清空", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
        }
    }
}
