package com.ling.box.calculator.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ling.box.calculator.algorithm.BalanceCoefficientCalculator
import com.ling.box.calculator.algorithm.LinearRegressionCalculator
import com.ling.box.calculator.algorithm.TwoPointIntersectionCalculator
import com.ling.box.calculator.calculator.RecommendedBlocksCalculator
import com.ling.box.calculator.model.BalanceCoefficientAlgorithm
import com.ling.box.calculator.model.ElevatorUiState
import com.ling.box.calculator.model.UnitState
import com.ling.box.calculator.model.toUiState
import com.ling.box.calculator.repository.ElevatorRepository
import com.ling.box.calculator.service.ElevatorCalculationService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- 定义计算器模式枚举 ---
enum class CalculatorMode { CUSTOM_BLOCKS, MANUAL_K }

class ElevatorCalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ElevatorRepository(application)
    private val twoPointCalculator: BalanceCoefficientCalculator = TwoPointIntersectionCalculator()
    private val linearRegressionCalculator: BalanceCoefficientCalculator = LinearRegressionCalculator()

    // 移除单个电梯的初始化，改为空列表
    private val _unitStateList = mutableStateListOf<UnitState>()
    val unitStateList: SnapshotStateList<UnitState> = _unitStateList

    private val _currentElevatorIndex = MutableStateFlow(repository.getCurrentElevatorIndex())
    val currentElevatorIndex: StateFlow<Int> = _currentElevatorIndex.asStateFlow()

    // 算法选择：从 Repository 读取，默认使用两点直线交点法
    private val _selectedAlgorithm = MutableStateFlow(
        BalanceCoefficientAlgorithm.values().getOrNull(
            repository.getAlgorithmSelection(BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION.ordinal)
        ) ?: BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION
    )
    val selectedAlgorithm: StateFlow<BalanceCoefficientAlgorithm> = _selectedAlgorithm.asStateFlow()

    // 设置算法选择
    fun setBalanceCoefficientAlgorithm(algorithm: BalanceCoefficientAlgorithm) {
        _selectedAlgorithm.value = algorithm
        repository.saveAlgorithmSelection(algorithm.ordinal)
        // 重新计算当前电梯的平衡系数
        triggerRecalculationOnly()
    }

    fun addElevator() {
        val newIndex = _unitStateList.size
        // 获取当前日期并格式化
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _unitStateList.add(
            UnitState(
                name = "电梯 ${newIndex + 1}",
                creationDate = currentDate,
                useCustomBlockInput = true
            )
        )
        _currentElevatorIndex.value = newIndex
        repository.updateLastAccessTime(newIndex)
        repository.saveElevatorList(_unitStateList.toList())
    }

    fun removeElevator(indexToRemove: Int) {
        removeElevators(setOf(indexToRemove))
    }

    // --- 更新电梯名称的方法 ---
    fun updateElevatorName(index: Int, newName: String) {
        if (index in _unitStateList.indices) {
            _unitStateList[index] = _unitStateList[index].copy(name = newName.trim())
            repository.saveState(index, _unitStateList[index])
        }
    }

    // 从标准百分比添加块数
    fun addStandardBlockSlot(blocks: Int) {
        updateDataForCurrentElevator { data ->
            // 首先确保当前是自定义模式
            if (!data.useCustomBlockInput) {
                data.useCustomBlockInput = true
                data.useManualBalance = false
            }

            // 添加新的块数到列表
            data.customBlockCounts.add(blocks.toString())
            data.customBlockPercentages.add(null)
            data.currentReadings.forEach { it.add("") }

            repository.saveState(getCurrentElevatorIndex(), data)
        }
        triggerFullRecalculation() // 添加新行后需要重新计算所有相关数据
    }

    // --- 公开给 UI 的、当前电梯的、不可变的状态 ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentElevatorUiState: StateFlow<ElevatorUiState> = _currentElevatorIndex
        .flatMapLatest { index ->
            snapshotFlow {
                _unitStateList.getOrNull(index)?.toUiState() // 移除参数
                    ?: ElevatorUiState.Companion.DEFAULT
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ElevatorUiState.Companion.DEFAULT
        )

    init {
        try {
            val loadedElevators = repository.loadInitialElevators()
            _unitStateList.clear()
            _unitStateList.addAll(loadedElevators)

            if (_unitStateList.isEmpty()) {
                addElevator()
            }
            
            // 自动选择最近访问的电梯
            val savedIndex = repository.getCurrentElevatorIndex()
            val mostRecentIndex = if (_unitStateList.isNotEmpty()) {
                _unitStateList.indices.maxByOrNull { repository.getLastAccessTime(it) } 
                    ?: savedIndex.coerceIn(0, _unitStateList.lastIndex)
            } else {
                0
            }
            
            _currentElevatorIndex.value = mostRecentIndex.coerceIn(0, _unitStateList.lastIndex.coerceAtLeast(0))
            repository.saveCurrentElevatorIndex(_currentElevatorIndex.value)
            repository.updateLastAccessTime(_currentElevatorIndex.value)
            
            // 确保当前电梯有至少一个块数输入行
            if (getCurrentData()?.useCustomBlockInput == true && getCurrentData()?.customBlockCounts?.isEmpty() == true) {
                getCurrentData()?.addInitialBlockSlot()
            }

            triggerFullRecalculation()
        } catch (e: Exception) {
            // 如果加载数据失败，创建一个新的默认电梯，避免应用崩溃
            android.util.Log.e("ElevatorCalculatorViewModel", "初始化失败，创建默认电梯", e)
            _unitStateList.clear()
            addElevator()
        }
    }

    // --- 更新状态的公共方法 ---

    fun selectElevator(index: Int) {
        if (index in _unitStateList.indices) {
            _currentElevatorIndex.value = index
            repository.saveCurrentElevatorIndex(index)
            repository.updateLastAccessTime(index)
            // 确保切换电梯后，如果当前是自定义模式，至少有一个块数输入框
            if (getCurrentData()?.useCustomBlockInput == true && getCurrentData()?.customBlockCounts?.isEmpty() == true) {
                getCurrentData()?.addInitialBlockSlot()
            }
            triggerFullRecalculation()
        }
    }
    
    // 批量删除电梯
    fun removeElevators(indices: Set<Int>) {
        if (indices.isEmpty()) return
        
        // 如果全选删除，清空所有后创建新电梯
        val isDeleteAll = indices.size >= _unitStateList.size
        
        if (isDeleteAll) {
            // 清空所有电梯
            _unitStateList.forEachIndexed { index, _ ->
                repository.removeLastAccessTime(index)
                // 显式清理所有电梯数据，防止存储残留
                repository.clearElevatorData(index)
            }
            _unitStateList.clear()
            // 创建新的空白电梯
            addElevator()
            repository.saveElevatorList(_unitStateList.toList())
            return
        }
        
        // 按索引从大到小排序，避免删除时索引变化
        val sortedIndices = indices.sortedDescending()
        val currentIndex = _currentElevatorIndex.value
        
        sortedIndices.forEach { indexToRemove ->
            if (indexToRemove in _unitStateList.indices) {
                repository.removeLastAccessTime(indexToRemove)
                // 显式清理被删除电梯的所有数据，防止存储残留
                repository.clearElevatorData(indexToRemove)
                _unitStateList.removeAt(indexToRemove)
            }
        }
        
        // 调整当前索引
        var newIndex = currentIndex
        sortedIndices.forEach { removedIndex ->
            when {
                newIndex == removedIndex -> {
                    newIndex = if (_unitStateList.isNotEmpty()) {
                        (removedIndex - 1).coerceAtLeast(0).coerceAtMost(_unitStateList.lastIndex)
                    } else {
                        -1
                    }
                }
                newIndex > removedIndex -> {
                    newIndex--
                }
            }
        }
        
        // 确保索引有效
        if (_unitStateList.isNotEmpty()) {
            newIndex = newIndex.coerceIn(0, _unitStateList.lastIndex)
            _currentElevatorIndex.value = newIndex
            repository.saveCurrentElevatorIndex(newIndex)
            repository.updateLastAccessTime(newIndex)
            
            // --- 新增逻辑：当只剩一个电梯时，检查并重命名为 "电梯 1" ---
            if (_unitStateList.size == 1) {
                val remainingElevator = _unitStateList[0]
                val currentNumber = getElevatorNumberFromName(remainingElevator.name)

                // 如果只剩一个电梯，且它的名称是默认格式但不是 "电梯 1"
                if (currentNumber != null && currentNumber != 1 && isDefaultElevatorName(remainingElevator.name)) {
                    // 自动重命名为 "电梯 1"
                    remainingElevator.name = "电梯 1"
                }
                _currentElevatorIndex.value = 0 // 确保索引指向这个唯一的电梯
            }
            // --- 结束新增逻辑 ---
        } else {
            // 如果全部删除，添加一个新电梯
            addElevator()
        }
        
        repository.saveElevatorList(_unitStateList.toList())
    }

    fun updateRatedLoad(value: String) {
        updateDataForCurrentElevator { data ->
            if (value.matches(Regex("[0-9]*\\.?[0-9]*"))) {
                data.ratedLoad = value
                repository.saveState(getCurrentElevatorIndex(), data)
            }
        }
        triggerFullRecalculation()
    }

    fun updateCounterweightWeight(value: String) {
        updateDataForCurrentElevator { data ->
            if (value.matches(Regex("[0-9]*\\.?[0-9]*"))) {
                data.counterweightWeight = value
                repository.saveState(getCurrentElevatorIndex(), data)
            }
        }
        triggerRecalculationAndRecommendation()
    }

    fun updateCounterweightBlockWeight(value: String) {
        updateDataForCurrentElevator { data ->
            if (value.matches(Regex("[0-9]*\\.?[0-9]*"))) {
                data.counterweightBlockWeight = value
                repository.saveState(getCurrentElevatorIndex(), data)
            }
        }
        triggerFullRecalculation()
    }

    fun updateManualBalanceCoefficientK(value: String?) {
        // 定义 K 值允许的物理范围：-50.0% 到 200.0%
        val minKThreshold = -50.0
        val maxKThreshold = 200.0

        // 1. 正则表达式：允许可选负号、数字和小数点
        val signedDecimalRegex = Regex("^-?[0-9]*\\.?[0-9]*$")

        // 2. 过滤输入：确保只包含有效的数字字符
        val filteredValue = value.takeIf { it?.matches(signedDecimalRegex) == true }

        // 3. 尝试解析为 Double
        val parsedK = filteredValue?.toDoubleOrNull()

        updateDataForCurrentElevator { data ->
            // 判断是否在有效范围内：值为空（不完整输入）或值在 [-50.0, 200.0] 之间
            val isValueInValidRange = parsedK == null || (parsedK >= minKThreshold && parsedK <= maxKThreshold)

            if (isValueInValidRange) {
                data.manualBalanceCoefficientK = filteredValue
                repository.saveState(getCurrentElevatorIndex(), data)
            } else if ((filteredValue as String?).isNullOrEmpty()) {
                data.manualBalanceCoefficientK = null
                repository.saveState(getCurrentElevatorIndex(), data)
            }
        }

        // 如果处于手动模式，触发后续的块数计算
        if (getCurrentData()?.useManualBalance == true) {
            calculateRecommendedBlocksForCurrent()
        }
    }

    fun toggleManualBalance(isChecked: Boolean) {
        updateDataForCurrentElevator { data ->
            data.useManualBalance = isChecked
            if (isChecked) { // 如果切换到手动模式
                data.useCustomBlockInput = false // 确保自定义模式关闭
            } else { // 如果从手动模式切换回来，默认进入自定义模式
                data.useCustomBlockInput = true
                if (data.customBlockCounts.isEmpty()) { // 确保至少有一行
                    data.addInitialBlockSlot()
                }
            }
            repository.saveState(getCurrentElevatorIndex(), data)
        }
        if (!isChecked) { // 如果从手动模式切换到自定义模式
            triggerFullRecalculation() // 重新计算所有
        } else { // 如果切换到手动模式
            triggerRecalculationOnly() // 只需要计算K
            calculateRecommendedBlocksForCurrent() // 重新计算推荐
        }
    }

    fun toggleCustomBlockInput(isChecked: Boolean) {
        updateDataForCurrentElevator { data ->
            data.useCustomBlockInput = isChecked
            if (isChecked) { // 如果切换到自定义模式
                data.useManualBalance = false // 确保手动模式关闭
                if (data.customBlockCounts.isEmpty()) { // 确保至少有一行
                    data.addInitialBlockSlot()
                }
            } else { // 如果从自定义模式切换回来
                // 这里只负责清空数据，模式切换由 UI 触发的 toggleManualBalance 完成
                data.customBlockCounts.clear() // 清空所有自定义块
                data.customBlockPercentages.clear()
                data.currentReadings.forEach { it.clear() }
            }
            repository.saveState(getCurrentElevatorIndex(), data)
        }
        triggerFullRecalculation()
    }

    fun updateCustomBlockCount(index: Int, value: String) {
        if (value.matches(Regex("[0-9]*"))) {
            updateDataForCurrentElevator { data ->
                // 确保列表大小足以容纳这个索引
                ElevatorCalculationService.ensureListSize(data.customBlockCounts, index + 1, "")
                ElevatorCalculationService.ensureListSize(data.customBlockPercentages, index + 1, null)
                data.currentReadings.forEach { currentList -> ElevatorCalculationService.ensureListSize(currentList, index + 1, "") }

                data.customBlockCounts[index] = value
                repository.saveState(getCurrentElevatorIndex(), data)
            }
            getCurrentData()?.let { ElevatorCalculationService.updateCustomBlockPercentages(it) }
            triggerRecalculationOnly()
            calculateRecommendedBlocksForCurrent()
        }
    }

    // 将 addCustomBlockCountSlotIfNeeded() 改为 addCustomBlockCountSlot()
    fun addCustomBlockCountSlot() {
        updateDataForCurrentElevator { data ->
            data.addInitialBlockSlot() // 调用 UnitState 中的方法
            repository.saveState(getCurrentElevatorIndex(), data)
        }
        triggerRecalculationOnly() // 添加新槽位后，可能需要重新计算
    }

    // 新增删除方法
    fun removeCustomBlockCountSlot(index: Int) {
        updateDataForCurrentElevator { data ->
            if (data.customBlockCounts.size > 1 && index in data.customBlockCounts.indices) { // 至少保留一个输入行
                data.customBlockCounts.removeAt(index)
                if (index < data.customBlockPercentages.size) { // 避免索引越界
                    data.customBlockPercentages.removeAt(index)
                }
                data.currentReadings.forEach { it.removeAt(index) }
                repository.saveState(getCurrentElevatorIndex(), data)
            }
        }
        triggerFullRecalculation() // 删除后需要重新计算所有
    }


    fun updateCurrentReading(directionIndex: Int, pointIndex: Int, value: String) {
        if (!(directionIndex == 0 || directionIndex == 1)) return

        updateDataForCurrentElevator { data ->
            if (value.matches(Regex("[0-9]*\\.?[0-9]*"))) {
                // 确保列表大小足以容纳这个索引
                ElevatorCalculationService.ensureListSize(data.customBlockCounts, pointIndex + 1, "") // 也确保块数列表足够大
                ElevatorCalculationService.ensureListSize(data.customBlockPercentages, pointIndex + 1, null) // 百分比列表
                ElevatorCalculationService.ensureListSize(data.currentReadings[directionIndex], pointIndex + 1, "")

                data.currentReadings[directionIndex][pointIndex] = value
                val otherDirection = 1 - directionIndex
                ElevatorCalculationService.ensureListSize(data.currentReadings[otherDirection], pointIndex + 1, "") // 确保另一方向也同步
                repository.saveState(getCurrentElevatorIndex(), data)
            }
        }
        triggerRecalculationOnly()
        calculateRecommendedBlocksForCurrent()
    }

    fun clearCurrentElevatorData() {
        updateDataForCurrentElevator { data ->
            data.resetCurrentInputData() // 调用新的重置方法
            repository.saveState(getCurrentElevatorIndex(), data)
        }
        triggerFullRecalculation()
    }

    // --- 私有辅助方法 ---

    // --- 判断是否为默认电梯名称格式 ---
    private fun isDefaultElevatorName(name: String): Boolean {
        // 使用正则表达式判断是否是 "电梯 数字" 的格式
        return name.matches(Regex("电梯 \\d+"))
    }

    // --- 从电梯名称中提取数字 ---
    private fun getElevatorNumberFromName(name: String): Int? {
        val matchResult = Regex("电梯 (\\d+)").find(name)
        return matchResult?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun getCurrentData(): UnitState? {
        return _unitStateList.getOrNull(_currentElevatorIndex.value)
    }

    private fun getCurrentElevatorIndex(): Int {
        return _currentElevatorIndex.value
    }

    private fun updateDataForCurrentElevator(updateAction: (UnitState) -> Unit) {
        val index = _currentElevatorIndex.value
        if (index in _unitStateList.indices) {
            val currentDataCopy = _unitStateList[index].copy()
            updateAction(currentDataCopy)
            _unitStateList[index] = currentDataCopy
        }
    }

    private fun triggerRecalculationOnly() {
        val data = getCurrentData() ?: return
        if (data.useManualBalance) {
            data.balanceCoefficientK = data.manualBalanceCoefficientK?.toDoubleOrNull()
            data.hasActualIntersection = true
            data.upwardCurrentPoints.clear()
            data.downwardCurrentPoints.clear()
        } else {
            ElevatorCalculationService.updateCurrentPoints(data)
            // 从 Repository 读取最新的算法选择
            val algorithm = BalanceCoefficientAlgorithm.values().getOrNull(
                repository.getAlgorithmSelection(BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION.ordinal)
            ) ?: BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION
            _selectedAlgorithm.value = algorithm
            
            // 根据选择的算法计算平衡系数
            val calculator = when (algorithm) {
                BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION -> twoPointCalculator
                BalanceCoefficientAlgorithm.LINEAR_REGRESSION -> linearRegressionCalculator
            }
            val (k, hasActual, r2) = calculator.calculate(
                data.upwardCurrentPoints.toList(),
                data.downwardCurrentPoints.toList()
            )
            data.balanceCoefficientK = k
            data.hasActualIntersection = hasActual
            data.linearRegressionR2 = r2
        }
        val ratedLoadVal = data.ratedLoad.toDoubleOrNull()
        val counterweightVal = data.counterweightWeight.toDoubleOrNull()
        if (ratedLoadVal != null && counterweightVal != null && ratedLoadVal > 0) {
            data.balanceCoefficient = (counterweightVal / ratedLoadVal) * 100
        } else {
            data.balanceCoefficient = null
        }
    }

    private fun calculateRecommendedBlocksForCurrent() {
        val data = getCurrentData() ?: return

        val currentK = if (data.useManualBalance) {
            data.manualBalanceCoefficientK?.toDoubleOrNull()
        } else {
            data.balanceCoefficientK
        }

        // 从 repository 读取平衡系数范围设置
        val targetKMin = repository.getBalanceRangeMin().toDouble()
        val targetKMax = repository.getBalanceRangeMax().toDouble()
        val idealK = repository.getBalanceIdeal().toDouble()

        data.recommendedBlocksMessage = RecommendedBlocksCalculator.calculate(
            currentBalanceCoefficient = currentK,
            ratedLoadVal = data.ratedLoad.toDoubleOrNull(),
            singleBlockWeightVal = data.counterweightWeight.toDoubleOrNull(),
            targetKMin = targetKMin,
            targetKMax = targetKMax,
            idealK = idealK
        )
    }


    private fun triggerFullRecalculation() {
        getCurrentData()?.let { ElevatorCalculationService.updateCustomBlockPercentages(it) }
        triggerRecalculationOnly()
        calculateRecommendedBlocksForCurrent()
    }

    private fun triggerRecalculationAndRecommendation() {
        triggerRecalculationOnly()
        calculateRecommendedBlocksForCurrent()
    }
}
