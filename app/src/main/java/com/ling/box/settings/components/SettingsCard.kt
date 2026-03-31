package com.ling.box.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ling.box.R
import com.ling.box.calculator.model.BalanceCoefficientAlgorithm

@Composable
fun SettingsCard(
    screenTitles: List<String>,
    currentStartScreenIndex: Int,
    onStartScreenClick: () -> Unit,
    currentAlgorithm: BalanceCoefficientAlgorithm,
    onAlgorithmClick: () -> Unit,
    balanceRangeSubtitle: String,
    onBalanceRangeClick: () -> Unit,
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
                title = stringResource(R.string.setting_start_screen),
                subtitle = stringResource(R.string.setting_current_format, screenTitles.getOrElse(currentStartScreenIndex) { screenTitles[0] }),
                onClick = onStartScreenClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            SettingItem(
                icon = Icons.Default.Science,
                title = stringResource(R.string.setting_algorithm),
                subtitle = when (currentAlgorithm) {
                    BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION -> stringResource(R.string.setting_current_format, stringResource(R.string.algorithm_two_point))
                    BalanceCoefficientAlgorithm.LINEAR_REGRESSION -> stringResource(R.string.setting_current_format, stringResource(R.string.algorithm_linear_regression))
                },
                onClick = onAlgorithmClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            SettingItem(
                icon = Icons.Default.Tune,
                title = stringResource(R.string.setting_balance_range),
                subtitle = balanceRangeSubtitle,
                onClick = onBalanceRangeClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            SettingItem(
                icon = Icons.Default.Storage,
                title = stringResource(R.string.setting_data_management),
                subtitle = stringResource(R.string.setting_data_management_subtitle),
                onClick = onExportImportClick
            )
        }
    }
}

