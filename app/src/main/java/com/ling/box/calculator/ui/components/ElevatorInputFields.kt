package com.ling.box.calculator.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ling.box.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElevatorInputFields(
    ratedLoad: String,
    counterweightBlockWeight: String,
    counterweightWeight: String,
    onRatedLoadChange: (String) -> Unit,
    onBlockWeightChange: (String) -> Unit,
    onCounterweightChange: (String) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val ratedLoadOptions = remember { listOf("630", "825", "1000", "1050", "1350", "1600", "1800", "2000", "3000") }
            var ratedLoadExpanded by remember { mutableStateOf(false) }

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
                        value = ratedLoad,
                        onValueChange = onRatedLoadChange,
                        label = { Text(stringResource(R.string.label_rated_load), style = MaterialTheme.typography.labelLarge) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        suffix = { Text("kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ratedLoadExpanded) },
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
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
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    )

                    ExposedDropdownMenu(
                        expanded = ratedLoadExpanded,
                        onDismissRequest = { ratedLoadExpanded = false },
                        modifier = Modifier
                            .exposedDropdownSize()
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                    ) {
                        ratedLoadOptions.forEachIndexed { index, selectionOption ->
                            val itemAlpha by animateFloatAsState(
                                targetValue = if (ratedLoadExpanded) 1f else 0f,
                                animationSpec = tween(durationMillis = 150, delayMillis = index * 20),
                                label = "itemAlpha_$index"
                            )

                            DropdownMenuItem(
                                text = { Text(selectionOption, style = MaterialTheme.typography.bodyLarge) },
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

            var isBlockWeightFocused by remember { mutableStateOf(false) }
            val blockWeightScale by animateFloatAsState(
                targetValue = if (isBlockWeightFocused) 1.02f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "blockWeightScale"
            )

            OutlinedTextField(
                value = counterweightBlockWeight,
                onValueChange = onBlockWeightChange,
                label = { Text(stringResource(R.string.label_block_weight), style = MaterialTheme.typography.labelLarge) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                suffix = { Text("kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { scaleX = blockWeightScale; scaleY = blockWeightScale }
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
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
            )
        }

        var isCounterweightFocused by remember { mutableStateOf(false) }
        val counterweightScale by animateFloatAsState(
            targetValue = if (isCounterweightFocused) 1.02f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "counterweightScale"
        )

        OutlinedTextField(
            value = counterweightWeight,
            onValueChange = onCounterweightChange,
            label = { Text(stringResource(R.string.label_counterweight), style = MaterialTheme.typography.labelLarge) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            suffix = { Text("kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = counterweightScale; scaleY = counterweightScale }
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
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
        )
    }
}
