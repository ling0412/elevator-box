package com.ling.box.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    titles: List<String>,
    icons: List<ImageVector>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    animationDurationMillis: Int = 300
) {
    NavigationBar {
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

            NavigationBarItem(
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
                alwaysShowLabel = false
            )
        }
    }
}

