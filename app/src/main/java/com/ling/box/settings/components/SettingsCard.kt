package com.ling.box.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ling.box.calculator.model.BalanceCoefficientAlgorithm

@Composable
fun SettingsCard(
    screenTitles: List<String>,
    currentStartScreenIndex: Int,
    onStartScreenClick: () -> Unit,
    currentAlgorithm: BalanceCoefficientAlgorithm,
    onAlgorithmClick: () -> Unit,
    onExportImportClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingItem(
                icon = Icons.AutoMirrored.Filled.Login,
                title = "设置启动界面",
                subtitle = "当前: ${screenTitles.getOrElse(currentStartScreenIndex) { screenTitles[0] }}",
                onClick = onStartScreenClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            SettingItem(
                icon = Icons.Default.Science,
                title = "平衡系数算法",
                subtitle = when (currentAlgorithm) {
                    BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION -> "当前: 两点直线交点法"
                    BalanceCoefficientAlgorithm.LINEAR_REGRESSION -> "当前: 线性拟合算法"
                },
                onClick = onAlgorithmClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            SettingItem(
                icon = Icons.Default.Storage,
                title = "数据管理",
                subtitle = "导入或导出数据",
                onClick = onExportImportClick
            )
        }
    }
}

