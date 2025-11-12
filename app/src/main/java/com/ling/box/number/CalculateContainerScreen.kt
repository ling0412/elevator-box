package com.ling.box.number

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration

// 定义计算器类型的枚举，并关联索引
enum class CalculatorType(val title: String, val icon: ImageVector, val index: Int) {
    NUMBER_BASE("进制转换工具", Icons.Filled.SwapHoriz, 0),
    SELF_CHECK("电梯自检参数计算", Icons.Filled.Build, 1)

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CalculateContainerScreen(paddingValues: PaddingValues) {
    var selectedTabIndex by remember { mutableIntStateOf(CalculatorType.NUMBER_BASE.index) }

    val selectedCalculator = remember(selectedTabIndex) {
        CalculatorType.entries.first { it.index == selectedTabIndex }
    }

    // 检测是否为大屏幕模式（与大屏幕布局判断保持一致）
    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600

    // 1. 定义内部 TopAppBar 的滚动行为
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // 2. 使用内部 Scaffold 来构建这个屏幕
    Scaffold(
        // === 关键修改 1: 将外部 Scaffold 的 paddingValues 应用到这里 (处理底部导航栏间距) ===
        // 注意：这里只应用 bottom 间距，避免双重计算 TopBar
        modifier = Modifier
            .fillMaxSize()
            // 外部 paddingValues 只包含 BottomBar 的高度，因为 TopBar 由内部处理
            .padding(bottom = paddingValues.calculateBottomPadding())
            // === 关键修改 2: 将内部 TopAppBar 的 nestedScrollConnection 传递给内部 Scaffold ===
            .nestedScroll(scrollBehavior.nestedScrollConnection),

        // 3. 定义内部 TopAppBar
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = selectedCalculator.title,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = tween(300))) togetherWith
                                    (fadeOut(animationSpec = tween(200)) +
                                            scaleOut(targetScale = 0.92f, animationSpec = tween(200)))
                        },
                        label = "TopAppBarTitleAnimation"
                    ) { targetTitle ->
                        Text(
                            text = targetTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                scrollBehavior = scrollBehavior, // 传递给 TopAppBar
                // 设置颜色过渡：大屏幕模式下保持background颜色，避免与NavigationRail颜色不一致
                // 小屏幕模式下使用secondaryContainer作为滚动后的颜色，提供视觉反馈
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = if (isExpandedScreen) {
                        // 大屏幕模式下保持背景色不变，与NavigationRail保持一致
                        MaterialTheme.colorScheme.background
                    } else {
                        // 小屏幕模式下使用secondaryContainer，提供滚动视觉反馈
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
                // === 关键修改 3: 恢复使用 TopAppBarDefaults.windowInsets，这是正确处理状态栏边衬区的官方方式 ===
                // 确保 TopAppBar 可以绘制到状态栏下方
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { innerScaffoldPadding -> // 内部 Scaffold 提供的内容内边距 (顶部 TopAppBar 高度)

        // 4. 内容区域：根据屏幕尺寸选择布局
        if (isExpandedScreen) {
            // 大屏幕模式：不使用外层滚动，让内部组件自己处理滚动
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerScaffoldPadding)
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    CalculatorType.entries.forEach { calculatorType ->
                        Tab(
                            selected = selectedTabIndex == calculatorType.index,
                            onClick = { selectedTabIndex = calculatorType.index },
                            text = { Text(calculatorType.title) },
                            icon = { Icon(calculatorType.icon, contentDescription = calculatorType.title) }
                        )
                    }
                }

                AnimatedContent(
                    targetState = selectedCalculator,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // 占用剩余空间
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(durationMillis = 350, delayMillis = 50))) togetherWith
                                (fadeOut(animationSpec = tween(durationMillis = 250)))
                    },
                    label = "CalculatorContentAnimation"
                ) { targetCalculator ->
                    when (targetCalculator) {
                        CalculatorType.SELF_CHECK -> ElevatorSafetyCalculator()
                        CalculatorType.NUMBER_BASE -> NumberBaseConverterPage()
                    }
                }
            }
        } else {
            // 小屏幕模式：使用外层滚动
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerScaffoldPadding)
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    CalculatorType.entries.forEach { calculatorType ->
                        Tab(
                            selected = selectedTabIndex == calculatorType.index,
                            onClick = { selectedTabIndex = calculatorType.index },
                            text = { Text(calculatorType.title) },
                            icon = { Icon(calculatorType.icon, contentDescription = calculatorType.title) }
                        )
                    }
                }

                AnimatedContent(
                    targetState = selectedCalculator,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(durationMillis = 350, delayMillis = 50))) togetherWith
                                (fadeOut(animationSpec = tween(durationMillis = 250)))
                    },
                    label = "CalculatorContentAnimation"
                ) { targetCalculator ->
                    when (targetCalculator) {
                        CalculatorType.SELF_CHECK -> ElevatorSafetyCalculator()
                        CalculatorType.NUMBER_BASE -> NumberBaseConverterPage()
                    }
                }
            }
        }
    }
}
