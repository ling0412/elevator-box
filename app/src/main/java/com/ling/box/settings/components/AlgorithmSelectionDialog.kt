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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ling.box.R
import com.ling.box.calculator.model.BalanceCoefficientAlgorithm

@Composable
fun AlgorithmSelectionDialog(
    currentAlgorithm: BalanceCoefficientAlgorithm,
    onConfirm: (BalanceCoefficientAlgorithm) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAlgorithm by remember(currentAlgorithm) { mutableStateOf(currentAlgorithm) }

    val algorithmOptions = listOf(
        BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION to stringResource(R.string.algorithm_two_point),
        BalanceCoefficientAlgorithm.LINEAR_REGRESSION to stringResource(R.string.algorithm_linear_regression)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_select_algorithm)) },
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
                                        stringResource(R.string.algorithm_two_point_desc)
                                    BalanceCoefficientAlgorithm.LINEAR_REGRESSION -> 
                                        stringResource(R.string.algorithm_linear_desc)
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
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

