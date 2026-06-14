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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ling.box.R
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
import com.ling.box.settings.utils.DataExportImportHelper
import com.ling.box.update.config.UpdateConfig
import com.ling.box.update.viewmodel.UpdateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// --- ShowPage Composable：显示应用信息页面 ---
@Composable
fun ShowPage(
    onStartScreenSelected: (Int) -> Unit,
    paddingValues: PaddingValues,
    updateViewModel: UpdateViewModel
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val calculatorPrefs = remember { context.getSharedPreferences("elevator_calculator_prefs", Context.MODE_PRIVATE) }
    val startScreenIndex = remember { mutableIntStateOf(sharedPreferences.getInt("start_screen_index", 0)) }
    val showStartScreenDialog = remember { mutableStateOf(false) }
    val showAlgorithmDialog = remember { mutableStateOf(false) }
    val showBalanceRangeDialog = remember { mutableStateOf(false) }

    val toastImportSuccess = stringResource(R.string.toast_import_success)
    val toastImportFailure = stringResource(R.string.toast_import_failure)
    val toastExportSuccess = stringResource(R.string.toast_export_success)
    val toastExportFailure = stringResource(R.string.toast_export_failure)
    val toastCannotOpenBrowser = stringResource(R.string.toast_cannot_open_browser)
    val toastExporting = stringResource(R.string.toast_exporting)
    val toastImporting = stringResource(R.string.toast_importing)
    val algorithmTwoPoint = stringResource(R.string.algorithm_two_point)
    val algorithmLinearRegression = stringResource(R.string.algorithm_linear_regression)
    val toastAlgorithmSwitched = stringResource(R.string.toast_algorithm_switched)
    val toastBalanceRangeSaved = stringResource(R.string.toast_balance_range_saved)
    
    // 读取当前算法选择
    val currentAlgorithm = remember {
        mutableStateOf(
            BalanceCoefficientAlgorithm.entries.getOrNull(
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
    val balanceRangeSubtitle = stringResource(
        R.string.balance_range_subtitle_format,
        String.format("%.1f", balanceRangeMin.value),
        String.format("%.1f", balanceRangeMax.value),
        String.format("%.1f", balanceIdeal.value)
    )

    val screenTitles = listOf(
        stringResource(R.string.nav_balance),
        stringResource(R.string.nav_calculator),
        stringResource(R.string.nav_settings)
    )
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
                        Toast.makeText(context, toastImportSuccess, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, toastImportFailure, Toast.LENGTH_LONG).show()
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
                        Toast.makeText(context, toastExportSuccess, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, toastExportFailure, Toast.LENGTH_SHORT).show()
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

    val isCheckingForUpdate by updateViewModel.isChecking.collectAsStateWithLifecycle()
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()
    val showUpdateDialog by updateViewModel.showDialog.collectAsStateWithLifecycle()

    var showLicenseDialog by remember { mutableStateOf(false) }
    var showExportImportDialog by remember { mutableStateOf(false) }


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
                    stringResource(R.string.app_name),
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
                        text = stringResource(R.string.quote_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 版权信息移到文字下方
                    Text(
                        text = stringResource(R.string.copyright_text),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

            // --- 应用信息卡片 ---
            AppInfoCard(
                isCheckingForUpdate = isCheckingForUpdate,
                onCheckUpdateClick = { updateViewModel.checkForUpdates() },
                onLicenseClick = { showLicenseDialog = true },
                onSourceCodeClick = {
                    val intent = Intent(Intent.ACTION_VIEW, UpdateConfig.GITHUB_REPO_URL.toUri())
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, toastCannotOpenBrowser, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, toastExporting, Toast.LENGTH_SHORT).show()
                }
            },
            onImportClick = {
                if (!isImporting) {
                    handleImport()
                } else {
                    Toast.makeText(context, toastImporting, Toast.LENGTH_SHORT).show()
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
                val algorithmName = when (algorithm) {
                    BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION -> algorithmTwoPoint
                    BalanceCoefficientAlgorithm.LINEAR_REGRESSION -> algorithmLinearRegression
                }
                Toast.makeText(context, String.format(toastAlgorithmSwitched, algorithmName), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAlgorithmDialog.value = false }
        )
    }

    if (showUpdateDialog) {
        val info = updateInfo
        if (info != null) {
            SettingsUpdateDialog(
                updateInfo = info,
                onDismiss = { updateViewModel.dismissDialog() }
            )
        }
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
                Toast.makeText(context, toastBalanceRangeSaved, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showBalanceRangeDialog.value = false }
        )
    }
}