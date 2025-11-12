package com.ling.box.ui.theme

// 从 Color.kt 中导入颜色变量

// 从其他主题文件导入 Shapes 和 Typography
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


// 定义深色模式的颜色方案 (Expressive 风格调整)
private val DarkColorScheme = darkColorScheme(
    // primary 颜色用于突出元素和状态
    primary = Purple80, // 使用 Expressive 风格的 Purple80
    onPrimary = Color.Black, // 在 Primary 颜色上显示的内容颜色 (确保高对比度)
    primaryContainer = PurpleGrey80, // Primary 的容器颜色 (与 Primary 颜色有区分)
    onPrimaryContainer = Color.Black, // 在 Primary 容器上显示的内容颜色 (确保高对比度)

    // secondary 颜色常用于强调、交互元素 (如 FAB, 按钮) 和指示器
    secondary = Pink80, // 使用 Expressive 风格的 Pink80
    onSecondary = Color.Black, // 在 Secondary 颜色上显示的内容颜色 (确保高对比度)
    secondaryContainer = Pink40, // *** 示例：使用 Expressive Pink40 作为 Secondary 容器颜色，使其更醒目 ***
    onSecondaryContainer = Color.White, // *** 示例：使用白色作为 Secondary 容器上的内容颜色，与 Pink40 形成高对比 ***

    // tertiary 颜色提供额外的区分度和强调
    tertiary = Purple40, // 示例：使用 Expressive Purple40 作为 Tertiary
    onTertiary = Color.White, // 在 Tertiary 颜色上显示的内容颜色
    tertiaryContainer = Pink80, // 示例：使用 Expressive Pink80 作为 Tertiary 容器
    onTertiaryContainer = Color.Black, // 在 Tertiary 容器上显示的内容颜色

    // 错误颜色通常保持标准 M3 即可
    error = Color(0xFFF2B8B5),
    onError = Color.Black,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color.White,

    // 背景和表面颜色影响整体应用的氛围
    background = Color(0xFF1C1B1F), // 深色背景
    onBackground = Color(0xFFE6E1E5), // 在背景上显示的内容颜色
    surface = Color(0xFF1C1B1F), // 主表面颜色 (通常是组件的背景色), 与背景一致或略有不同
    onSurface = Color(0xFFE6E1E5), // 在 Surface 颜色上显示的内容颜色

    // 其他辅助颜色
    surfaceVariant = Color(0xFF49454F), // 用于导航栏等变化表面的颜色
    onSurfaceVariant = Color(0xFFCAC4D0), // 在 surfaceVariant 颜色上显示的内容颜色
    outline = Color(0xFF938F99), // 轮廓颜色
    inverseSurface = Color(0xFFE6E1E5), // 反色 Surface
    inverseOnSurface = Color(0xFF313033), // 在反色 Surface 上显示的内容颜色
    inversePrimary = Color(0xFFD0BCFF), // 反色 Primary
    surfaceTint = Color(0xFFD0BCFF), // Surface tint (通常用于高程阴影)
    outlineVariant = Color(0xFF49454F), // 轮廓变体颜色
    scrim = Color(0xFF000000), // Scrim 颜色 (用于蒙版效果)
)

// 定义浅色模式的颜色方案 (Expressive 风格调整)
private val LightColorScheme = lightColorScheme(
    // primary 颜色用于突出元素和状态
    primary = Purple40, // 使用 Expressive 风格的 Purple40
    onPrimary = Color.White, // 在 Primary 颜色上显示的内容颜色 (确保高对比度)
    primaryContainer = PurpleGrey40, // Primary 的容器颜色
    onPrimaryContainer = Color.Black, // 在 Primary 容器上显示的内容颜色

    // secondary 颜色常用于强调、交互元素 (如 FAB, 按钮) 和指示器
    secondary = Pink40, // 使用 Expressive 风格的 Pink40
    onSecondary = Color.White, // 在 Secondary 颜色上显示的内容颜色 (确保高对比度)
    secondaryContainer = Pink80, // *** 示例：使用 Expressive Pink80 作为 Secondary 容器颜色，使其更醒目 ***
    onSecondaryContainer = Color.Black, // *** 示例：使用黑色作为 Secondary 容器上的内容颜色，与 Pink80 形成高对比 ***

    // tertiary 颜色提供额外的区分度和强调
    tertiary = Purple80, // 示例：使用 Expressive Purple80 作为 Tertiary
    onTertiary = Color.White, // 在 Tertiary 颜色上显示的内容颜色
    tertiaryContainer = Purple40, // 示例：使用 Expressive Purple40 作为 Tertiary 容器
    onTertiaryContainer = Color.White, // 在 Tertiary 容器上显示的内容颜色

    // 错误颜色通常保持标准 M3 即可
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color.Black,

    // 背景和表面颜色影响整体应用的氛围
    background = Color(0xFFFFFBFE), // 浅色背景
    onBackground = Color(0xFF1C1B1F), // 在背景上显示的内容颜色
    surface = Color(0xFFFFFBFE), // 主表面颜色, 与背景一致或略有不同
    onSurface = Color(0xFF1C1B1F), // 在 Surface 颜色上显示的内容颜色

    // 其他辅助颜色
    surfaceVariant = Color(0xFFE7E0EC), // 用于导航栏等变化表面的颜色
    onSurfaceVariant = Color(0xFF49454F), // 在 surfaceVariant 颜色上显示的内容颜色
    outline = Color(0xFF79747E), // 轮廓颜色
    inverseSurface = Color(0xFF313033), // 反色 Surface
    inverseOnSurface = Color(0xFFF4EFF4), // 在反色 Surface 上显示的内容颜色
    inversePrimary = Color(0xFFD0BCFF), // 反色 Primary
    surfaceTint = Color(0xFF6750A4), // Surface tint (通常用于高程阴影)
    outlineVariant = Color(0xFFCAC4D0), // 轮廓变体颜色
    scrim = Color(0xFF000000), // Scrim 颜色 (用于蒙版效果)
)

@Composable
fun 系数计算器Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor 参数控制是否使用动态颜色方案 (Android 12+)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 只有当 dynamicColor 为 true 且系统版本支持时，才使用动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 否则使用我们定义的默认深色或浅色方案
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    // 确保不在编辑模式下运行 SideEffect
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // 设置状态栏颜色 (通常使用 primary 或 surface)
            // 注意：statusBarColor 在 API 34+ 中已弃用，但对于边缘到边缘应用仍需要设置
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.surface.toArgb()
            
            // 设置导航栏颜色为透明，实现沉浸式效果（修复MIUI/澎湃OS下不沉浸的问题）
            // 注意：navigationBarColor 在 API 34+ 中已弃用，但对于边缘到边缘应用仍需要设置
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()

            // 控制状态栏内容的颜色（图标、文字）以确保可读性
            // 如果状态栏背景是浅色，内容应为深色 (isAppearanceLightStatusBars = true)
            // 如果状态栏背景是深色，内容应为亮色 (isAppearanceLightStatusBars = false)
            insetsController.isAppearanceLightStatusBars = !darkTheme

            // 控制导航栏内容的颜色（图标、文字）以确保可读性
            insetsController.isAppearanceLightNavigationBars = !darkTheme

            // 对于沉浸式导航栏，不强制对比度（MIUI/澎湃OS需要）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            // 确保窗口可以绘制到系统栏区域（如果需要全屏或边缘到边缘显示）
            // 如果不需要全屏或边缘到边缘，可以设置为 true 或移除此行
            WindowCompat.setDecorFitsSystemWindows(window, false) // 保持边缘到边缘绘制
        }
    }

    // 应用 Material 3 主题
    MaterialTheme(
        colorScheme = colorScheme, // 应用调整后的颜色方案
        typography = Typography, // 引用 Typography.kt 中定义的 Typography
        shapes = Shapes,       // 引用 Shapes.kt 中定义的 Shapes
        content = content
    )
}