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
import androidx.compose.ui.unit.dp
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
                Text("参数", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            listOf(
                "实际载荷(kg)" to { data: EstimatedBlocksRowData -> data.actualLoadKg.toString() },
                "砝码块数" to { data: EstimatedBlocksRowData -> data.blockCount + "块" },
                "实际百分比" to { data: EstimatedBlocksRowData -> data.actualPercentage }
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

