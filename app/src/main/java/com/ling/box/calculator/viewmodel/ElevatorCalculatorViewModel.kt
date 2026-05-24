package com.ling.box.calculator.viewmodel

import android.app.Application
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- 定义计算器模式枚举 ---
enum class CalculatorMode { CUSTOM_BLOCKS, MANUAL_K }

class ElevatorCalculatorViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val UNSIGNED_DECIMAL_REGEX = Regex("[0-9]*\\.?[0-9]*")
        private val SIGNED_DECIMAL_REGEX = Regex("^-?[0-9]*\\.?[0-9]*$")
        private val INTEGER_REGEX = Regex("[0-9]*")
        private val DEFAULT_NAME_REGEX = Regex("电梯 \\d+")
        private val DEFAULT_NAME_CAPTURE_REGEX = Regex("电梯 (\\d+)")
    }

    private val repository = ElevatorRepository(application)
    private val twoPointCalculator: BalanceCoefficientCalculator = TwoPointIntersectionCalculator()
    private val linearRegressionCalculator: BalanceCoefficientCalculator = LinearRegressionCalculator()

    private val _unitStateList = MutableStateFlow<List<UnitState>>(emptyList())
    val unitStateList: StateFlow<List<UnitState>> = _unitStateList.asStateFlow()

    private val _currentElevatorIndex = MutableStateFlow(repository.getCurrentElevatorIndex())
    val currentElevatorIndex: StateFlow<Int> = _currentElevatorIndex.asStateFlow()

    private val _selectedAlgorithm = MutableStateFlow(
        BalanceCoefficientAlgorithm.entries.getOrNull(
            repository.getAlgorithmSelection(BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION.ordinal)
        ) ?: BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION
    )
    val selectedAlgorithm: StateFlow<BalanceCoefficientAlgorithm> = _selectedAlgorithm.asStateFlow()

    // 设置算法选择
    fun setBalanceCoefficientAlgorithm(algorithm: BalanceCoefficientAlgorithm) {
        _selectedAlgorithm.value = algorithm
        repository.saveAlgorithmSelection(algorithm.ordinal)
        triggerRecalculationOnly()
    }

    fun addElevator() {
        val currentList = _unitStateList.value
        val newIndex = currentList.size
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _unitStateList.value = currentList + UnitState(
            name = "电梯 ${newIndex + 1}",
            creationDate = currentDate,
            useCustomBlockInput = true
        )
        _currentElevatorIndex.value = newIndex
        repository.updateLastAccessTime(newIndex)
        repository.saveElevatorList(_unitStateList.value)
    }

    fun removeElevator(indexToRemove: Int) {
        removeElevators(setOf(indexToRemove))
    }

    fun updateElevatorName(index: Int, newName: String) {
        mutateAt(index) { it.copy(name = newName.trim()) }
        repository.saveState(index, _unitStateList.value.getOrNull(index)!!)
    }

    fun addStandardBlockSlot(blocks: Int) {
        mutateCurrentElevator { data ->
            if (!data.useCustomBlockInput) {
                data.useCustomBlockInput = true
                data.useManualBalance = false
            }
            data.customBlockCounts.add(blocks.toString())
            data.customBlockPercentages.add(null)
            data.currentReadings.forEach { it.add("") }
            repository.saveState(getCurrentElevatorIndex(), data)
        }
        triggerFullRecalculation()
    }

    val currentElevatorUiState: StateFlow<ElevatorUiState> = combine(
        _currentElevatorIndex,
        _unitStateList
    ) { index, list ->
        list.getOrNull(index)?.toUiState() ?: ElevatorUiState.DEFAULT
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ElevatorUiState.DEFAULT
    )

    init {
        try {
            val loadedElevators = repository.loadInitialElevators()
            _unitStateList.value = loadedElevators

            if (_unitStateList.value.isEmpty()) {
                addElevator()
            }

            val savedIndex = repository.getCurrentElevatorIndex()
            val list = _unitStateList.value
            val mostRecentIndex = if (list.isNotEmpty()) {
                list.indices.maxByOrNull { repository.getLastAccessTime(it) }
                    ?: savedIndex.coerceIn(0, list.lastIndex)
            } else {
                0
            }

            _currentElevatorIndex.value = mostRecentIndex.coerceIn(0, list.lastIndex.coerceAtLeast(0))
            repository.saveCurrentElevatorIndex(_currentElevatorIndex.value)
            repository.updateLastAccessTime(_currentElevatorIndex.value)

            if (getCurrentData()?.useCustomBlockInput == true && getCurrentData()?.customBlockCounts?.isEmpty() == true) {
                mutateCurrentElevator { it.addInitialBlockSlot() }
            }

            triggerFullRecalculation()
        } catch (e: Exception) {
            android.util.Log.e("ElevatorCalculatorViewModel", "初始化失败，创建默认电梯", e)
            _unitStateList.value = emptyList()
            addElevator()
        }
    }

    fun selectElevator(index: Int) {
        if (index in _unitStateList.value.indices) {
            _currentElevatorIndex.value = index
            repository.saveCurrentElevatorIndex(index)
            repository.updateLastAccessTime(index)
            if (getCurrentData()?.useCustomBlockInput == true && getCurrentData()?.customBlockCounts?.isEmpty() == true) {
                mutateCurrentElevator { it.addInitialBlockSlot() }
            }
            triggerFullRecalculation()
        }
    }

    fun removeElevators(indices: Set<Int>) {
        if (indices.isEmpty()) return

        val isDeleteAll = indices.size >= _unitStateList.value.size

        if (isDeleteAll) {
            _unitStateList.value.forEachIndexed { index, _ ->
                repository.removeLastAccessTime(index)
                repository.clearElevatorData(index)
            }
            _unitStateList.value = emptyList()
            addElevator()
            repository.saveElevatorList(_unitStateList.value)
            return
        }

        val sortedIndices = indices.sortedDescending()
        val currentIndex = _currentElevatorIndex.value
        var newList = _unitStateList.value.toMutableList()

        sortedIndices.forEach { indexToRemove ->
            if (indexToRemove in newList.indices) {
                repository.removeLastAccessTime(indexToRemove)
                repository.clearElevatorData(indexToRemove)
                newList.removeAt(indexToRemove)
            }
        }

        var newIndex = currentIndex
        sortedIndices.forEach { removedIndex ->
            when {
                newIndex == removedIndex -> {
                    newIndex = if (newList.isNotEmpty()) {
                        (removedIndex - 1).coerceAtLeast(0).coerceAtMost(newList.lastIndex)
                    } else -1
                }
                newIndex > removedIndex -> newIndex--
            }
        }

        if (newList.isNotEmpty()) {
            newIndex = newIndex.coerceIn(0, newList.lastIndex)
            _unitStateList.value = newList
            _currentElevatorIndex.value = newIndex
            repository.saveCurrentElevatorIndex(newIndex)
            repository.updateLastAccessTime(newIndex)

            if (newList.size == 1) {
                val remainingElevator = newList[0]
                val currentNumber = getElevatorNumberFromName(remainingElevator.name)
                if (currentNumber != null && currentNumber != 1 && isDefaultElevatorName(remainingElevator.name)) {
                    newList = newList.toMutableList()
                    newList[0] = remainingElevator.copy(name = "电梯 1")
                    _unitStateList.value = newList
                }
                _currentElevatorIndex.value = 0
            }
        } else {
            addElevator()
        }

        repository.saveElevatorList(_unitStateList.value)
    }

    fun updateRatedLoad(value: String) {
        if (value.matches(UNSIGNED_DECIMAL_REGEX)) {
            mutateCurrentElevator { data ->
                data.ratedLoad = value
                repository.saveState(getCurrentElevatorIndex(), data)
            }
            triggerFullRecalculation()
        }
    }

    fun updateCounterweightWeight(value: String) {
        if (value.matches(UNSIGNED_DECIMAL_REGEX)) {
            mutateCurrentElevator { data ->
                data.counterweightWeight = value
                repository.saveState(getCurrentElevatorIndex(), data)
            }
            triggerRecalculationAndRecommendation()
        }
    }

    fun updateCounterweightBlockWeight(value: String) {
        if (value.matches(UNSIGNED_DECIMAL_REGEX)) {
            mutateCurrentElevator { data ->
                data.counterweightBlockWeight = value
                repository.saveState(getCurrentElevatorIndex(), data)
            }
            triggerFullRecalculation()
        }
    }

    fun updateManualBalanceCoefficientK(value: String?) {
        val minKThreshold = -50.0
        val maxKThreshold = 200.0
        val filteredValue = value.takeIf { it?.matches(SIGNED_DECIMAL_REGEX) == true }
        val parsedK = filteredValue?.toDoubleOrNull()

        mutateCurrentElevator { data ->
            val isValueInValidRange = parsedK == null || (parsedK >= minKThreshold && parsedK <= maxKThreshold)
            if (isValueInValidRange) {
                data.manualBalanceCoefficientK = filteredValue
                repository.saveState(getCurrentElevatorIndex(), data)
            } else if ((filteredValue as String?).isNullOrEmpty()) {
                data.manualBalanceCoefficientK = null
                repository.saveState(getCurrentElevatorIndex(), data)
            }
        }

        if (getCurrentData()?.useManualBalance == true) {
            calculateRecommendedBlocksForCurrent()
        }
    }

    fun toggleManualBalance(isChecked: Boolean) {
        mutateCurrentElevator { data ->
            data.useManualBalance = isChecked
            if (isChecked) {
                data.useCustomBlockInput = false
            } else {
                data.useCustomBlockInput = true
                if (data.customBlockCounts.isEmpty()) {
                    data.addInitialBlockSlot()
                }
            }
            repository.saveState(getCurrentElevatorIndex(), data)
        }
        if (!isChecked) {
            triggerFullRecalculation()
        } else {
            triggerRecalculationOnly()
            calculateRecommendedBlocksForCurrent()
        }
    }

    fun toggleCustomBlockInput(isChecked: Boolean) {
        mutateCurrentElevator { data ->
            data.useCustomBlockInput = isChecked
            if (isChecked) {
                data.useManualBalance = false
                if (data.customBlockCounts.isEmpty()) {
                    data.addInitialBlockSlot()
                }
            } else {
                data.customBlockCounts.clear()
                data.customBlockPercentages.clear()
                data.currentReadings.forEach { it.clear() }
            }
            repository.saveState(getCurrentElevatorIndex(), data)
        }
        triggerFullRecalculation()
    }

    fun updateCustomBlockCount(index: Int, value: String) {
        if (value.matches(INTEGER_REGEX)) {
            mutateCurrentElevator { data ->
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

    fun addCustomBlockCountSlot() {
        mutateCurrentElevator { data ->
            data.addInitialBlockSlot()
            repository.saveState(getCurrentElevatorIndex(), data)
        }
        triggerRecalculationOnly()
    }

    fun removeCustomBlockCountSlot(index: Int) {
        mutateCurrentElevator { data ->
            if (data.customBlockCounts.size > 1 && index in data.customBlockCounts.indices) {
                data.customBlockCounts.removeAt(index)
                if (index < data.customBlockPercentages.size) {
                    data.customBlockPercentages.removeAt(index)
                }
                data.currentReadings.forEach { it.removeAt(index) }
                repository.saveState(getCurrentElevatorIndex(), data)
            }
        }
        triggerFullRecalculation()
    }

    fun updateCurrentReading(directionIndex: Int, pointIndex: Int, value: String) {
        if (!(directionIndex == 0 || directionIndex == 1)) return
        mutateCurrentElevator { data ->
            if (value.matches(UNSIGNED_DECIMAL_REGEX)) {
                ElevatorCalculationService.ensureListSize(data.customBlockCounts, pointIndex + 1, "")
                ElevatorCalculationService.ensureListSize(data.customBlockPercentages, pointIndex + 1, null)
                ElevatorCalculationService.ensureListSize(data.currentReadings[directionIndex], pointIndex + 1, "")
                data.currentReadings[directionIndex][pointIndex] = value
                val otherDirection = 1 - directionIndex
                ElevatorCalculationService.ensureListSize(data.currentReadings[otherDirection], pointIndex + 1, "")
                repository.saveState(getCurrentElevatorIndex(), data)
            }
        }
        triggerRecalculationOnly()
        calculateRecommendedBlocksForCurrent()
    }

    fun clearCurrentElevatorData() {
        mutateCurrentElevator { data ->
            data.resetCurrentInputData()
            repository.saveState(getCurrentElevatorIndex(), data)
        }
        triggerFullRecalculation()
    }

    // --- 私有辅助方法 ---

    private fun isDefaultElevatorName(name: String): Boolean {
        return name.matches(DEFAULT_NAME_REGEX)
    }

    private fun getElevatorNumberFromName(name: String): Int? {
        return DEFAULT_NAME_CAPTURE_REGEX.find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun getCurrentData(): UnitState? {
        return _unitStateList.value.getOrNull(_currentElevatorIndex.value)
    }

    private fun getCurrentElevatorIndex(): Int {
        return _currentElevatorIndex.value
    }

    private inline fun mutateAt(index: Int, crossinline transform: (UnitState) -> UnitState) {
        _unitStateList.value = _unitStateList.value.toMutableList().also { list ->
            list.getOrNull(index)?.let { old -> list[index] = transform(old) }
        }
    }

    private inline fun mutateCurrentElevator(crossinline action: (UnitState) -> Unit) {
        val index = _currentElevatorIndex.value
        val currentList = _unitStateList.value
        if (index in currentList.indices) {
            val updatedList = currentList.toMutableList()
            val copy = updatedList[index].deepCopy()
            action(copy)
            updatedList[index] = copy
            _unitStateList.value = updatedList
        }
    }

    private fun triggerRecalculationOnly() {
        val index = _currentElevatorIndex.value
        val currentList = _unitStateList.value
        val data = currentList.getOrNull(index) ?: return
        val updatedList = currentList.toMutableList()
        val copy = updatedList[index].deepCopy()

        if (copy.useManualBalance) {
            copy.balanceCoefficientK = copy.manualBalanceCoefficientK?.toDoubleOrNull()
            copy.hasActualIntersection = true
            copy.upwardCurrentPoints.clear()
            copy.downwardCurrentPoints.clear()
        } else {
            ElevatorCalculationService.updateCurrentPoints(copy)
            val algorithm = BalanceCoefficientAlgorithm.entries.getOrNull(
                repository.getAlgorithmSelection(BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION.ordinal)
            ) ?: BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION
            _selectedAlgorithm.value = algorithm

            val calculator = when (algorithm) {
                BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION -> twoPointCalculator
                BalanceCoefficientAlgorithm.LINEAR_REGRESSION -> linearRegressionCalculator
            }
            val (k, hasActual, r2) = calculator.calculate(
                copy.upwardCurrentPoints.toList(),
                copy.downwardCurrentPoints.toList()
            )
            copy.balanceCoefficientK = k
            copy.hasActualIntersection = hasActual
            copy.linearRegressionR2 = r2
        }
        val ratedLoadVal = copy.ratedLoad.toDoubleOrNull()
        val counterweightVal = copy.counterweightWeight.toDoubleOrNull()
        if (ratedLoadVal != null && counterweightVal != null && ratedLoadVal > 0) {
            copy.balanceCoefficient = (counterweightVal / ratedLoadVal) * 100
        } else {
            copy.balanceCoefficient = null
        }
        updatedList[index] = copy
        _unitStateList.value = updatedList
    }

    private fun calculateRecommendedBlocksForCurrent() {
        val index = _currentElevatorIndex.value
        val currentList = _unitStateList.value
        val data = currentList.getOrNull(index) ?: return
        val updatedList = currentList.toMutableList()
        val copy = updatedList[index].deepCopy()

        val currentK = if (copy.useManualBalance) {
            copy.manualBalanceCoefficientK?.toDoubleOrNull()
        } else {
            copy.balanceCoefficientK
        }

        val targetKMin = repository.getBalanceRangeMin().toDouble()
        val targetKMax = repository.getBalanceRangeMax().toDouble()
        val idealK = repository.getBalanceIdeal().toDouble()

        copy.recommendedBlocksMessage = RecommendedBlocksCalculator.calculate(
            currentBalanceCoefficient = currentK,
            ratedLoadVal = copy.ratedLoad.toDoubleOrNull(),
            singleBlockWeightVal = copy.counterweightWeight.toDoubleOrNull(),
            targetKMin = targetKMin,
            targetKMax = targetKMax,
            idealK = idealK
        )
        updatedList[index] = copy
        _unitStateList.value = updatedList
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
