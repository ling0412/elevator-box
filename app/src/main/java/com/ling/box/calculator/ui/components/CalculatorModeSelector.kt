package com.ling.box.calculator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ling.box.calculator.viewmodel.CalculatorMode

@Composable
fun CalculatorModeSelector(
    currentMode: CalculatorMode,
    onModeChange: (CalculatorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(CalculatorMode.CUSTOM_BLOCKS, CalculatorMode.MANUAL_K)
    val selectedIndex = remember(currentMode) { modes.indexOf(currentMode) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEachIndexed { index, mode ->
            val isSelected = index == selectedIndex

            val cornerRadius = animateDpAsState(
                targetValue = if (isSelected) 24.dp else 16.dp,
                animationSpec = tween(durationMillis = 200),
                label = "cornerRadiusAnimation"
            ).value

            val buttonShape: Shape = RoundedCornerShape(cornerRadius)

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                animationSpec = tween(durationMillis = 200),
                label = "containerColorAnimation"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 200),
                label = "contentColorAnimation"
            )

            val animatedZIndex by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "zIndexAnimation"
            )

            Button(
                onClick = { onModeChange(mode) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .then(Modifier.zIndex(animatedZIndex)),
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = when (mode) {
                        CalculatorMode.CUSTOM_BLOCKS -> "自动"
                        CalculatorMode.MANUAL_K -> "手动"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.5.sp),
                    maxLines = 1
                )
            }
        }
    }
}
