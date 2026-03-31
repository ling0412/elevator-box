package com.ling.box.calculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ling.box.R
import com.ling.box.calculator.model.EstimatedBlocksRowData

@Composable
fun EstimatedBlocksTable(
    tableData: List<EstimatedBlocksRowData>
) {
    if (tableData.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.table_header_param), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                tableData.forEach { data ->
                    Text(
                        data.header,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val actualLoadLabel = stringResource(R.string.table_row_actual_load)
            val blockCountLabel = stringResource(R.string.table_row_block_count)
            val blocksSuffix = stringResource(R.string.table_suffix_blocks)
            val actualPercentageLabel = stringResource(R.string.table_row_actual_percentage)

            listOf(
                actualLoadLabel to { data: EstimatedBlocksRowData -> data.actualLoadKg.toString() },
                blockCountLabel to { data: EstimatedBlocksRowData -> data.blockCount + blocksSuffix },
                actualPercentageLabel to { data: EstimatedBlocksRowData -> data.actualPercentage }
            ).forEach { (label, dataExtractor) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(2f))
                    tableData.forEach { data ->
                        Text(
                            dataExtractor(data),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

