package com.ling.box.navigation.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ling.box.R

@Composable
fun getDynamicBottomNavTitles(): List<String> {
    return listOf(
        stringResource(R.string.nav_balance),
        stringResource(R.string.nav_calculator),
        stringResource(R.string.nav_settings)
    )
}

fun getDynamicIcons(): List<ImageVector> {
    return listOf(
        Icons.Filled.AddChart,
        Icons.Filled.Calculate,
        Icons.Filled.Settings
    )
}

