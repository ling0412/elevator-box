package com.ling.box

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.ling.box.calculator.model.BalanceCoefficientAlgorithm
import com.ling.box.calculator.repository.ElevatorRepository
import com.ling.box.settings.components.AlgorithmSelectionDialog
import com.ling.box.settings.components.AppInfoCard
import com.ling.box.settings.components.BalanceRangeSettingsDialog
import com.ling.box.settings.components.ExportImportDialog
import com.ling.box.settings.components.LicenseDialog
import com.ling.box.settings.components.SettingsCard
import com.ling.box.settings.components.SettingsUpdateDialog
import com.ling.box.settings.components.StartScreenDialog
import com.ling.box.update.data.UpdateInfo
import com.ling.box.settings.utils.DataExportImportHelper
import com.ling.box.update.config.UpdateConfig
import com.ling.box.update.utils.compareVersions
import com.ling.box.update.utils.fetchLatestReleaseInfo
import com.ling.box.update.utils.getAppVersionName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// --- ShowPage Composable：显示应用信息页面 ---
@Composable
fun ShowPage(
    onStartScreenSelected: (Int) -> Unit,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val calculatorPrefs = remember { context.getSharedPreferences("elevator_calculator_prefs", Context.MODE_PRIVATE) }
    val startScreenIndex = remember { mutableIntStateOf(sharedPreferences.getInt("start_screen_index", 0)) }
    val showStartScreenDialog = remember { mutableStateOf(false) }
    val showAlgorithmDialog = remember { mutableStateOf(false) }
    val showBalanceRangeDialog = remember { mutableStateOf(false) }
    
    // 读取当前算法选择
    val currentAlgorithm = remember {
        mutableStateOf(
            BalanceCoefficientAlgorithm.values().getOrNull(
                calculatorPrefs.getInt("balance_coefficient_algorithm", BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION.ordinal)
            ) ?: BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION
        )
    }
    
    // 读取当前平衡系数范围设置
    val repository = remember { ElevatorRepository(context) }
    val balanceRangeMin = remember { mutableStateOf<Float>(repository.getBalanceRangeMin()) }
    val balanceRangeMax = remember { mutableStateOf<Float>(repository.getBalanceRangeMax()) }
    val balanceIdeal = remember { mutableStateOf<Float>(repository.getBalanceIdeal()) }
    
    // 生成设置项的副标题
    val balanceRangeSubtitle = remember(balanceRangeMin.value, balanceRangeMax.value, balanceIdeal.value) {
        "范围: ${String.format("%.1f", balanceRangeMin.value)}%-${String.format("%.1f", balanceRangeMax.value)}%, 最佳: ${String.format("%.1f", balanceIdeal.value)}%"
    }

    val screenTitles = remember {
        listOf("磅梯", "计算", "设置")
    }
    val coroutineScope = rememberCoroutineScope()

    // 导出/导入相关状态
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    // 文件选择器 Launcher - 用于导入
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    DataExportImportHelper.importFromUri(context, uri)
                }
                withContext(Dispatchers.Main) {
                    isImporting = false
                    if (success) {
                        Toast.makeText(context, "数据导入成功！请重启应用以查看导入的数据。", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "数据导入失败，请检查文件格式是否正确。", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // 文件创建 Launcher - 用于导出
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            isExporting = true
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    DataExportImportHelper.exportToUri(context, uri)
                }
                withContext(Dispatchers.Main) {
                    isExporting = false
                    if (success) {
                        Toast.makeText(context, "数据导出成功！", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "数据导出失败，请重试。", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // 导出数据
    fun handleExport() {
        val fileName = DataExportImportHelper.generateExportFileName()
        exportFileLauncher.launch(fileName)
    }

    // 导入数据
    fun handleImport() {
        importFileLauncher.launch("application/json")
    }

    var isCheckingForUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showExportImportDialog by remember { mutableStateOf(false) }

    fun getAppVersionName(): String {
        return getAppVersionName(context)
    }

    // --- Function to trigger update check ---
    fun checkForUpdates() {
        if (isCheckingForUpdate) return

        isCheckingForUpdate = true
        coroutineScope.launch {
            val currentVersion = getAppVersionName()

            val result = fetchLatestReleaseInfo()

            withContext(Dispatchers.Main) {
                isCheckingForUpdate = false
                result.onSuccess { latestUpdateInfo ->
                    if (compareVersions(latestUpdateInfo.latestVersion, currentVersion) > 0) {
                        updateInfo = latestUpdateInfo
                        showUpdateDialog = true
                    } else {
                        Toast.makeText(context, "当前已是最新版本 ($currentVersion)", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { exception ->
                    Toast.makeText(context, "检查更新失败: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // 确保背景色与主题一致
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
            // 外部 Column 不应用任何 paddingValues，确保它占据整个屏幕
        ) {
        // 1. TopAppBar - 静态 (固定顶部栏)
        TopAppBar(
            title = {
                Text(
                    "电梯工具箱",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
            windowInsets = TopAppBarDefaults.windowInsets
        )

        // 2. 可滚动内容区域 (内部 Column)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // === 关键修复：将 paddingValues 的底部内边距应用于此可滚动内容 Column 的底部 ===
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // 确保与卡片对齐
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "我们度过的每个平凡的日常，其实是接连不断发生的奇迹。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 版权信息移到文字下方
                    Text(
                        text = "版权所有 © 2025 ling",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

            // --- 应用信息卡片 ---
            AppInfoCard(
                isCheckingForUpdate = isCheckingForUpdate,
                onCheckUpdateClick = { checkForUpdates() },
                onLicenseClick = { showLicenseDialog = true },
                onSourceCodeClick = {
                    val intent = Intent(Intent.ACTION_VIEW, UpdateConfig.GITHUB_REPO_URL.toUri())
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 通用设置卡片 ---
            SettingsCard(
                screenTitles = screenTitles,
                currentStartScreenIndex = startScreenIndex.intValue,
                onStartScreenClick = { showStartScreenDialog.value = true },
                currentAlgorithm = currentAlgorithm.value,
                onAlgorithmClick = { showAlgorithmDialog.value = true },
                balanceRangeSubtitle = balanceRangeSubtitle,
                onBalanceRangeClick = { showBalanceRangeDialog.value = true },
                onExportImportClick = { showExportImportDialog = true }
            )

            Spacer(modifier = Modifier.weight(1f)) // 占据剩余空间
        }
    }
    }

    // --- 对话框区域 ---
    LicenseDialog(
        showDialog = showLicenseDialog,
        onDismiss = { showLicenseDialog = false }
    )

    if (showExportImportDialog) {
        ExportImportDialog(
            onExportClick = {
                if (!isExporting) {
                    handleExport()
                } else {
                    Toast.makeText(context, "正在导出中，请稍候...", Toast.LENGTH_SHORT).show()
                }
            },
            onImportClick = {
                if (!isImporting) {
                    handleImport()
                } else {
                    Toast.makeText(context, "正在导入中，请稍候...", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showExportImportDialog = false }
        )
    }

    if (showStartScreenDialog.value) {
        StartScreenDialog(
            screenTitles = screenTitles,
            currentIndex = startScreenIndex.intValue,
            onConfirm = { index ->
                sharedPreferences.edit {
                    putInt("start_screen_index", index)
                }
                onStartScreenSelected(index)
                showStartScreenDialog.value = false
            },
            onDismiss = { showStartScreenDialog.value = false }
        )
    }

    if (showAlgorithmDialog.value) {
        AlgorithmSelectionDialog(
            currentAlgorithm = currentAlgorithm.value,
            onConfirm = { algorithm ->
                calculatorPrefs.edit {
                    putInt("balance_coefficient_algorithm", algorithm.ordinal)
                }
                currentAlgorithm.value = algorithm
                showAlgorithmDialog.value = false
                Toast.makeText(context, "算法已切换为: ${when (algorithm) {
                    BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION -> "两点直线交点法"
                    BalanceCoefficientAlgorithm.LINEAR_REGRESSION -> "线性拟合算法"
                }}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAlgorithmDialog.value = false }
        )
    }

    if (showUpdateDialog && updateInfo != null) {
        SettingsUpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false }
        )
    }
    
    if (showBalanceRangeDialog.value) {
        BalanceRangeSettingsDialog(
            currentMin = balanceRangeMin.value,
            currentMax = balanceRangeMax.value,
            currentIdeal = balanceIdeal.value,
            onConfirm = { min, max, ideal ->
                repository.saveBalanceRangeSettings(min, max, ideal)
                balanceRangeMin.value = min
                balanceRangeMax.value = max
                balanceIdeal.value = ideal
                showBalanceRangeDialog.value = false
                Toast.makeText(context, "平衡系数范围设置已保存", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showBalanceRangeDialog.value = false }
        )
    }
}