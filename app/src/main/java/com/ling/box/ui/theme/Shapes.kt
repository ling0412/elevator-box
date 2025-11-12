package com.ling.box.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp) // 示例：更大的圆角
    // Material 3 Expressive 可能会鼓励为特定组件定义更独特的形状
    // 例如:
    // extraLarge = RoundedCornerShape(24.dp)
    // button = RoundedCornerShape(50) // 圆形按钮
)