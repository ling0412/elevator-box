package com.ling.box.navigation.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

fun getDynamicBottomNavTitles(): List<String> {
    return listOf("磅梯", "计算", "设置")
}

fun getDynamicIcons(): List<ImageVector> {
    return listOf(
        Icons.Filled.AddChart,
        Icons.Filled.Calculate,
        Icons.Filled.Settings
    )
}

