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
        val elevator = _unitStateList.value.getOrNull(index)
        if (elevator != null) {
            val updated = elevator.withUpdatedName(newName)
            _unitStateList.value = _unitStateList.value.toMutableList().also { it[index] = updated }
            repository.saveState(index, updated)
        }
    }

    fun addStandardBlockSlot(blocks: Int) {
        updateCurrentElevator { data ->
            var updated = data
            if (!data.useCustomBlockInput) {
                updated = updated.withUpdatedUseCustomBlockInput(true)
                    .withUpdatedUseManualBalance(false)
            }
            updated = updated.withAddedCustomBlockCountSlot()
            // Update the added slot with the block count
            val lastIndex = updated.customBlockCounts.size - 1
            updated = updated.withUpdatedCustomBlockCount(lastIndex, blocks.toString())
            repository.saveState(getCurrentElevatorIndex(), updated)
            updated
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
                updateCurrentElevator { it.addInitialBlockSlot() }
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
                updateCurrentElevator { it.addInitialBlockSlot() }
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
            updateCurrentElevator { data ->
                val updated = data.withUpdatedRatedLoad(value)
                repository.saveState(getCurrentElevatorIndex(), updated)
                updated
            }
            triggerFullRecalculation()
        }
    }

    fun updateCounterweightWeight(value: String) {
        if (value.matches(UNSIGNED_DECIMAL_REGEX)) {
            updateCurrentElevator { data ->
                val updated = data.withUpdatedCounterweightWeight(value)
                repository.saveState(getCurrentElevatorIndex(), updated)
                updated
            }
            triggerRecalculationAndRecommendation()
        }
    }

    fun updateCounterweightBlockWeight(value: String) {
        if (value.matches(UNSIGNED_DECIMAL_REGEX)) {
            updateCurrentElevator { data ->
                val updated = data.withUpdatedCounterweightBlockWeight(value)
                repository.saveState(getCurrentElevatorIndex(), updated)
                updated
            }
            triggerFullRecalculation()
        }
    }

    fun updateManualBalanceCoefficientK(value: String?) {
        val minKThreshold = -50.0
        val maxKThreshold = 200.0
        val filteredValue = value.takeIf { it?.matches(SIGNED_DECIMAL_REGEX) == true }
        val parsedK = filteredValue?.toDoubleOrNull()

        updateCurrentElevator { data ->
            val isValueInValidRange = parsedK == null || (parsedK >= minKThreshold && parsedK <= maxKThreshold)
            val updated = if (isValueInValidRange) {
                data.withUpdatedManualBalanceCoefficientK(filteredValue)
            } else if (filteredValue.isNullOrEmpty()) {
                data.withUpdatedManualBalanceCoefficientK(null)
            } else {
                data
            }
            repository.saveState(getCurrentElevatorIndex(), updated)
            updated
        }

        if (getCurrentData()?.useManualBalance == true) {
            calculateRecommendedBlocksForCurrent()
        }
    }

    fun toggleManualBalance(isChecked: Boolean) {
        updateCurrentElevator { data ->
            var updated = data.withUpdatedUseManualBalance(isChecked)
            if (isChecked) {
                updated = updated.withUpdatedUseCustomBlockInput(false)
            } else {
                updated = updated.withUpdatedUseCustomBlockInput(true)
                if (updated.customBlockCounts.isEmpty()) {
                    updated = updated.addInitialBlockSlot()
                }
            }
            repository.saveState(getCurrentElevatorIndex(), updated)
            updated
        }
        if (!isChecked) {
            triggerFullRecalculation()
        } else {
            triggerRecalculationOnly()
            calculateRecommendedBlocksForCurrent()
        }
    }

    fun toggleCustomBlockInput(isChecked: Boolean) {
        updateCurrentElevator { data ->
            var updated = data.withUpdatedUseCustomBlockInput(isChecked)
            if (isChecked) {
                updated = updated.withUpdatedUseManualBalance(false)
                if (updated.customBlockCounts.isEmpty()) {
                    updated = updated.addInitialBlockSlot()
                }
            } else {
                updated = updated.copy(
                    customBlockCounts = emptyList(),
                    customBlockPercentages = emptyList(),
                    currentReadings = listOf(emptyList(), emptyList())
                )
            }
            repository.saveState(getCurrentElevatorIndex(), updated)
            updated
        }
        triggerFullRecalculation()
    }

    fun updateCustomBlockCount(index: Int, value: String) {
        if (value.matches(INTEGER_REGEX)) {
            updateCurrentElevator { data ->
                // Ensure the list has enough elements
                var updated = data
                while (updated.customBlockCounts.size <= index) {
                    updated = updated.withAddedCustomBlockCountSlot()
                }
                updated = updated.withUpdatedCustomBlockCount(index, value)
                updated = ElevatorCalculationService.updateCustomBlockPercentages(updated)
                repository.saveState(getCurrentElevatorIndex(), updated)
                updated
            }
            triggerRecalculationOnly()
            calculateRecommendedBlocksForCurrent()
        }
    }

    fun addCustomBlockCountSlot() {
        updateCurrentElevator { data ->
            val updated = data.addInitialBlockSlot()
            repository.saveState(getCurrentElevatorIndex(), updated)
            updated
        }
        triggerRecalculationOnly()
    }

    fun removeCustomBlockCountSlot(index: Int) {
        updateCurrentElevator { data ->
            val updated = data.withRemovedCustomBlockCountSlot(index)
            if (updated != data) {
                repository.saveState(getCurrentElevatorIndex(), updated)
            }
            updated
        }
        triggerFullRecalculation()
    }

    fun updateCurrentReading(directionIndex: Int, pointIndex: Int, value: String) {
        if (!(directionIndex == 0 || directionIndex == 1)) return
        if (value.matches(UNSIGNED_DECIMAL_REGEX)) {
            updateCurrentElevator { data ->
                // Ensure the list has enough elements
                var updated = data
                while (updated.customBlockCounts.size <= pointIndex) {
                    updated = updated.withAddedCustomBlockCountSlot()
                }
                updated = updated.withUpdatedCurrentReading(directionIndex, pointIndex, value)
                repository.saveState(getCurrentElevatorIndex(), updated)
                updated
            }
            triggerRecalculationOnly()
            calculateRecommendedBlocksForCurrent()
        }
    }

    fun clearCurrentElevatorData() {
        updateCurrentElevator { data ->
            val updated = data.resetCurrentInputData()
            repository.saveState(getCurrentElevatorIndex(), updated)
            updated
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

    private inline fun updateCurrentElevator(crossinline transform: (UnitState) -> UnitState) {
        val index = _currentElevatorIndex.value
        val currentList = _unitStateList.value
        if (index in currentList.indices) {
            val updatedState = transform(currentList[index])
            _unitStateList.value = currentList.toMutableList().also { it[index] = updatedState }
        }
    }

    private fun triggerRecalculationOnly() {
        val index = _currentElevatorIndex.value
        val currentList = _unitStateList.value
        val data = currentList.getOrNull(index) ?: return
        
        var updated = data
        if (updated.useManualBalance) {
            updated = updated.withUpdatedBalanceCoefficientK(updated.manualBalanceCoefficientK?.toDoubleOrNull())
                .withUpdatedHasActualIntersection(true)
                .withUpdatedUpwardCurrentPoints(emptyList())
                .withUpdatedDownwardCurrentPoints(emptyList())
        } else {
            updated = ElevatorCalculationService.updateCurrentPoints(updated)
            val algorithm = BalanceCoefficientAlgorithm.entries.getOrNull(
                repository.getAlgorithmSelection(BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION.ordinal)
            ) ?: BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION
            _selectedAlgorithm.value = algorithm

            val calculator = when (algorithm) {
                BalanceCoefficientAlgorithm.TWO_POINT_INTERSECTION -> twoPointCalculator
                BalanceCoefficientAlgorithm.LINEAR_REGRESSION -> linearRegressionCalculator
            }
            val (k, hasActual, r2) = calculator.calculate(
                updated.upwardCurrentPoints,
                updated.downwardCurrentPoints
            )
            updated = updated.withUpdatedBalanceCoefficientK(k)
                .withUpdatedHasActualIntersection(hasActual)
                .withUpdatedLinearRegressionR2(r2)
        }
        val ratedLoadVal = updated.ratedLoad.toDoubleOrNull()
        val counterweightVal = updated.counterweightWeight.toDoubleOrNull()
        updated = if (ratedLoadVal != null && counterweightVal != null && ratedLoadVal > 0) {
            updated.withUpdatedBalanceCoefficient((counterweightVal / ratedLoadVal) * 100)
        } else {
            updated.withUpdatedBalanceCoefficient(null)
        }
        _unitStateList.value = currentList.toMutableList().also { it[index] = updated }
    }

    private fun calculateRecommendedBlocksForCurrent() {
        val index = _currentElevatorIndex.value
        val currentList = _unitStateList.value
        val data = currentList.getOrNull(index) ?: return
        
        val currentK = if (data.useManualBalance) {
            data.manualBalanceCoefficientK?.toDoubleOrNull()
        } else {
            data.balanceCoefficientK
        }

        val targetKMin = repository.getBalanceRangeMin().toDouble()
        val targetKMax = repository.getBalanceRangeMax().toDouble()
        val idealK = repository.getBalanceIdeal().toDouble()

        val message = RecommendedBlocksCalculator.calculate(
            currentBalanceCoefficient = currentK,
            ratedLoadVal = data.ratedLoad.toDoubleOrNull(),
            singleBlockWeightVal = data.counterweightWeight.toDoubleOrNull(),
            targetKMin = targetKMin,
            targetKMax = targetKMax,
            idealK = idealK
        )
        val updated = data.withUpdatedRecommendedBlocksMessage(message)
        _unitStateList.value = currentList.toMutableList().also { it[index] = updated }
    }

    private fun triggerFullRecalculation() {
        updateCurrentElevator { data ->
            ElevatorCalculationService.updateCustomBlockPercentages(data)
        }
        triggerRecalculationOnly()
        calculateRecommendedBlocksForCurrent()
    }

    private fun triggerRecalculationAndRecommendation() {
        triggerRecalculationOnly()
        calculateRecommendedBlocksForCurrent()
    }
}
