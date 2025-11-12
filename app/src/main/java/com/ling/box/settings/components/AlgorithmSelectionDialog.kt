package com.ling.box.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ling.box.calculator.model.BalanceCoefficientAlgorithm

@Composable
fun AlgorithmSelectionDialog(
    currentAlgorithm: BalanceCoefficientAlgorithm,
    onConfirm: (BalanceCoefficientAlgorithm) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAlgorithm by remember(currentAlgorithm) { mutableStateOf(currentAlgorithm) }

    val algorithmOptions = listOf(
        BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION to "两点直线交点法",
        BalanceCoefficientAlgorithm.LINEAR_REGRESSION to "线性拟合算法"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择平衡系数计算算法") },
        text = {
            Column {
                algorithmOptions.forEach { (algorithm, name) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedAlgorithm = algorithm }
                    ) {
                        RadioButton(
                            selected = selectedAlgorithm == algorithm,
                            onClick = { selectedAlgorithm = algorithm }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (algorithm) {
                                    BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION -> 
                                        "直线计算选择实际交点，若无实际交点则虚拟延长线"
                                    BalanceCoefficientAlgorithm.LINEAR_REGRESSION -> 
                                        "拟合所有数据点，加权平均。\n⚠️ 实验功能"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedAlgorithm) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

