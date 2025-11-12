package com.ling.box.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun NavigationRailBar(
    titles: List<String>,
    icons: List<ImageVector>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    animationDurationMillis: Int = 300
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh // 使用更暗的表面容器颜色，对比更柔和
    ) {
        // 使用Column让按钮垂直排列，并在上下添加Spacer来居中偏下
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 使用weight让按钮组位于中间偏下位置，更灵活适应不同屏幕高度
            Spacer(modifier = Modifier.weight(1f))
            
            titles.forEachIndexed { index, title ->
                val selected = selectedIndex == index

                val iconSize by animateDpAsState(
                    targetValue = if (selected) 32.dp else 24.dp,
                    animationSpec = tween(durationMillis = animationDurationMillis),
                    label = "iconSizeAnimation"
                )

                val iconColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = animationDurationMillis),
                    label = "iconColorAnimation"
                )

                NavigationRailItem(
                    icon = {
                        Icon(
                            icons[index],
                            contentDescription = title,
                            modifier = Modifier.size(iconSize),
                            tint = iconColor
                        )
                    },
                    selected = selected,
                    onClick = { onTabSelected(index) },
                    label = { Text(title) },
                    alwaysShowLabel = true
                )
            }
        }
    }
}

