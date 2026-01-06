package com.ling.box

// 导入 AnimatedContent 和 Transition 相关的 API
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ling.box.calculator.CalculatorScreen
import com.ling.box.navigation.components.BottomNavigationBar
import com.ling.box.navigation.components.NavigationRailBar
import com.ling.box.navigation.utils.getDynamicBottomNavTitles
import com.ling.box.navigation.utils.getDynamicIcons
import com.ling.box.number.CalculateContainerScreen
import com.ling.box.ui.theme.系数计算器Theme
import com.ling.box.update.components.UpdateDialog
import com.ling.box.update.data.UpdateInfo
import com.ling.box.update.utils.compareVersions
import com.ling.box.update.utils.fetchLatestReleaseInfo
import com.ling.box.update.utils.getAppVersionName
import com.ling.box.utils.VersionMigrationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在UI加载之前执行版本迁移检查
        VersionMigrationHelper.performMigrationIfNeeded(this)

        enableEdgeToEdge() // 启用边缘到边缘绘制
        setContent {
            系数计算器Theme { // 应用你的自定义 Material 3 主题
                val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
                // 从 SharedPreferences 读取默认启动页索引，如果不存在则默认为 0
                val defaultTabIndex = sharedPreferences.getInt("start_screen_index", 0)
                val context = LocalContext.current // 获取 Context

                // 记住上次检查更新的时间，初始值为 SharedPreferences 中的值或 0
                val lastUpdateTimeMillis = remember { mutableLongStateOf(sharedPreferences.getLong("last_update_check_time", 0L)) }
                // 定义检查更新的时间间隔 (这里设置为 24 小时)
                val updateCheckIntervalMillis = TimeUnit.DAYS.toMillis(1)
                // 记住是否正在检查更新的状态
                val isCheckingForUpdate = remember { mutableStateOf(false) }
                // 记住最新更新信息的状态
                val updateInfo = remember { mutableStateOf<UpdateInfo?>(null) }
                // 记住是否显示更新对话框的状态
                val showUpdateDialog = remember { mutableStateOf(false) }
                // 获取协程作用域，用于在 Compose 中启动异步任务
                val coroutineScope = rememberCoroutineScope()

                // 获取当前应用的 versionName
                fun getAppVersionName(): String {
                    return getAppVersionName(context)
                }

                // 检查更新的函数
                fun checkForUpdates(isAutomatic: Boolean = false) {
                    // 如果正在检查更新，则直接返回
                    if (isCheckingForUpdate.value) return

                    isCheckingForUpdate.value = true // 设置为正在检查更新
                    coroutineScope.launch { // 在协程中执行异步任务
                        val currentVersion = getAppVersionName() // 获取当前版本

                        Log.d("UpdateCheck", "Current Version: $currentVersion (Automatic: $isAutomatic)")

                        // 调用异步函数获取最新发布信息
                        val result = fetchLatestReleaseInfo()

                        withContext(Dispatchers.Main) { // 切换到主线程更新 UI
                            isCheckingForUpdate.value = false // 检查完成，设置状态为 false
                            // 保存本次检查更新的时间
                            sharedPreferences.edit {
                                putLong("last_update_check_time", System.currentTimeMillis())
                            }
                            lastUpdateTimeMillis.longValue = System.currentTimeMillis()

                            // 处理检查结果
                            result.onSuccess { latestUpdateInfo ->
                                Log.d("UpdateCheck", "Latest Version Found: ${latestUpdateInfo.latestVersion}")
                                // 比较版本号
                                if (compareVersions(latestUpdateInfo.latestVersion, currentVersion) > 0) {
                                    updateInfo.value = latestUpdateInfo // 保存最新更新信息
                                    showUpdateDialog.value = true // 显示更新对话框
                                } else if (isAutomatic) {
                                    Log.d("UpdateCheck", "Current version is up to date (automatic check).")
                                } else {
                                    // 如果不是自动检查，且已是最新版本，显示提示
                                    Toast.makeText(context, "当前已是最新版本 ($currentVersion)", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure { exception ->
                                Log.e("UpdateCheck", "Update check failed (Automatic: $isAutomatic)", exception)
                                // 如果不是自动检查，且检查失败，显示错误提示
                                if (!isAutomatic) {
                                    Toast.makeText(context, "检查更新失败: ${exception.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

                // 使用 LaunchedEffect 在 Composable 首次创建时或 key 改变时执行副作用
                // 将 lastUpdateTimeMillis 作为一个 key，当它被更新时，会重新评估这个 LaunchedEffect
                LaunchedEffect(key1 = lastUpdateTimeMillis.longValue) {
                    val currentTimeMillis = System.currentTimeMillis()
                    // 确保当前读取的 lastUpdateTimeMillis.longValue 是最新的，以便正确判断
                    if (currentTimeMillis - lastUpdateTimeMillis.longValue > updateCheckIntervalMillis) {
                        Log.d("UpdateCheck", "Performing automatic update check on startup.")
                        checkForUpdates(isAutomatic = true)
                    } else {
                        Log.d("UpdateCheck", "Skipping automatic update check on startup (last check was ${
                            TimeUnit.MILLISECONDS.toHours(currentTimeMillis - lastUpdateTimeMillis.longValue)
                        } hours ago).")
                    }
                }

                MainAppScreen(
                    initialTabIndex = defaultTabIndex
                )

                // --- Update Available Dialog (更新可用对话框) ---
                if (showUpdateDialog.value && updateInfo.value != null) {
                    UpdateDialog(
                        updateInfo = updateInfo.value!!,
                        onDismiss = { showUpdateDialog.value = false }
                    )
                }
            }
        }
    }
}

// ===========================================================================
// 底部导航栏和屏幕内容 Composable
// ===========================================================================

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainAppScreen(
    initialTabIndex: Int
) {
    // 检测屏幕尺寸：使用Configuration来检测屏幕宽度
    val configuration = LocalConfiguration.current
    // 当屏幕宽度 >= 600dp 时，认为是平板/大屏幕设备
    val isExpandedScreen = configuration.screenWidthDp >= 600
    
    // 记住当前选中的底部导航索引
    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex) }
    val context = LocalContext.current
    // 获取 SharedPreferences 实例
    val sharedPreferences = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    val onStartScreenSelectedCallback = { index: Int ->
        val bottomNavTitles = getDynamicBottomNavTitles()
        if (index >= 0 && index < bottomNavTitles.size) {
            // 将选中的索引保存到 SharedPreferences
            sharedPreferences.edit {
                putInt("start_screen_index", index)
            }
            // 显示保存成功的提示
            Toast.makeText(context, "启动页面已设置为: ${bottomNavTitles[index]}", Toast.LENGTH_SHORT).show()
            selectedTabIndex = index
        } else {
            Toast.makeText(context, "启动页面索引无效", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // 底部导航栏标题
    val bottomNavTitles = getDynamicBottomNavTitles()

    // 图标列表
    val icons = getDynamicIcons()

    // 屏幕列表
    val screens: List<@Composable (PaddingValues) -> Unit> = remember {
        listOf(
            // 0: 磅梯页面
            { padding -> CalculatorScreen(padding) },
            // 1: 计算页面
            { padding -> CalculateContainerScreen(padding) },
            // 2: 设置页面
            { padding -> ShowPage(onStartScreenSelected = onStartScreenSelectedCallback, paddingValues = padding) }
        )
    }

    val animationDurationMillis = 300 // 动画时长

    // 根据屏幕尺寸选择不同的布局
    if (isExpandedScreen) {
        // 大屏幕布局：使用 NavigationRail 和内容宽度限制
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 导航轨道
            NavigationRailBar(
                titles = bottomNavTitles,
                icons = icons,
                selectedIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                animationDurationMillis = animationDurationMillis
            )
            
            // 内容区域，限制最大宽度并居中，添加背景色避免白色闪烁
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 1200.dp) // 限制最大宽度为1200dp
                            .fillMaxHeight() // 确保内容区域填满可用高度
                    ) {
                        AnimatedContent(
                            targetState = selectedTabIndex,
                            transitionSpec = {
                                // 大屏幕模式下使用淡入淡出动画，增加重叠时间避免闪烁
                                // 使用更长的动画时间和重叠，让过渡更平滑
                                fadeIn(
                                    animationSpec = tween(
                                        durationMillis = animationDurationMillis + 150,
                                        easing = FastOutSlowInEasing
                                    )
                                ) togetherWith fadeOut(
                                    animationSpec = tween(
                                        durationMillis = animationDurationMillis,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }, label = "screenTransition"
                        ) { targetIndex ->
                            // 确保每个屏幕都有背景色，避免白色闪烁
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                if (targetIndex >= 0 && targetIndex < screens.size) {
                                    // 大屏幕使用空的 PaddingValues 因为不需要底部导航栏的间距
                                    screens[targetIndex](PaddingValues())
                                } else {
                                    screens[0](PaddingValues())
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // 小屏幕布局：使用底部导航栏
        Scaffold(
            // 将 scrollBehavior 的 nestedScrollConnection 传递给 Scaffold
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

            // 移除 TopAppBar，现在 TopAppBar 在 CalculatorScreen 内部，这样可以包含在截图中
            // topBar = {
            //     if (selectedTabIndex == 0) {
            //         TopAppBar(
            //             title = {
            //                 Text(
            //                     "平衡系数计算",
            //                     style = MaterialTheme.typography.titleLarge
            //                 )
            //             },
            //             // 传递 scrollBehavior 对象给 TopAppBar ===
            //             scrollBehavior = scrollBehavior,
            //
            //             // 设置颜色过渡
            //             colors = TopAppBarDefaults.topAppBarColors(
            //                 containerColor = MaterialTheme.colorScheme.background,
            //                 scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer
            //             )
            //         )
            //     }
            // },
            bottomBar = {
                BottomNavigationBar(
                    titles = bottomNavTitles,
                    icons = icons,
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    animationDurationMillis = animationDurationMillis
                )
            }
        ) { innerPadding -> // innerPadding 由 Scaffold 提供，用于主内容区域
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(
                                animationSpec = tween(animationDurationMillis, easing = FastOutSlowInEasing)
                            ) { width -> width } + fadeIn(
                                animationSpec = tween(
                                    durationMillis = animationDurationMillis + 150,
                                    easing = FastOutSlowInEasing
                                )
                            ) togetherWith
                                    slideOutHorizontally(
                                        animationSpec = tween(animationDurationMillis, easing = FastOutSlowInEasing)
                                    ) { width -> -width } + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = animationDurationMillis,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(animationDurationMillis, easing = FastOutSlowInEasing)
                            ) { width -> -width } + fadeIn(
                                animationSpec = tween(
                                    durationMillis = animationDurationMillis + 150,
                                    easing = FastOutSlowInEasing
                                )
                            ) togetherWith
                                    slideOutHorizontally(
                                        animationSpec = tween(animationDurationMillis, easing = FastOutSlowInEasing)
                                    ) { width -> width } + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = animationDurationMillis,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                        }.using(
                            SizeTransform(clip = true)
                        )
                    }, label = "screenTransition"
                ) { targetIndex ->
                    // 确保每个屏幕都有背景色，避免白色闪烁
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (targetIndex >= 0 && targetIndex < screens.size) {
                            screens[targetIndex](innerPadding)
                        } else {
                            screens[0](innerPadding) // 默认显示第一个屏幕
                        }
                    }
                }
            }
        }
    }
}
