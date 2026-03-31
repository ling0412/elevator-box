package com.ling.box.calculator.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.edit
import com.ling.box.calculator.model.UnitState
import com.ling.box.settings.data.UnitStateExport
import com.ling.box.settings.data.toExport
import com.ling.box.settings.data.toUnitState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ElevatorRepository(context: Context) {

    companion object {
        private const val TAG = "ElevatorRepository"
        private const val PREFS_NAME = "elevator_calculator_prefs"
        private const val ELEVATOR_COUNT_KEY = "elevator_count"
        private const val LAST_ACCESS_TIME_PREFIX = "last_access_time_"

        private const val BALANCE_RANGE_MIN_KEY = "balance_range_min"
        private const val BALANCE_RANGE_MAX_KEY = "balance_range_max"
        private const val BALANCE_IDEAL_KEY = "balance_ideal"

        const val DEFAULT_BALANCE_RANGE_MIN = 45.0f
        const val DEFAULT_BALANCE_RANGE_MAX = 50.0f
        const val DEFAULT_BALANCE_IDEAL = 47.5f
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ======================== 电梯列表 CRUD ========================

    fun saveElevatorList(unitStateList: List<UnitState>) {
        val oldCount = prefs.getInt(ELEVATOR_COUNT_KEY, 0)
        val newCount = unitStateList.size

        if (oldCount > newCount) {
            for (i in newCount until oldCount) {
                clearElevatorData(i)
            }
        }

        prefs.edit { putInt(ELEVATOR_COUNT_KEY, newCount) }
        unitStateList.forEachIndexed { index, state -> saveState(index, state) }
    }

    fun saveState(elevatorIndex: Int, state: UnitState) {
        val export = state.toExport()
        prefs.edit {
            putString("elevator_${elevatorIndex}_json", json.encodeToString(export))
        }
    }

    fun loadInitialElevators(): List<UnitState> {
        val count = prefs.getInt(ELEVATOR_COUNT_KEY, 0)
        if (count == 0) return emptyList()

        return (0 until count).mapNotNull { i ->
            try {
                loadState(i)
            } catch (e: Exception) {
                Log.e(TAG, "加载电梯 $i 失败", e)
                null
            }
        }
    }

    private fun loadState(elevatorIndex: Int): UnitState {
        val jsonStr = prefs.getString("elevator_${elevatorIndex}_json", null)
        if (jsonStr != null) {
            return json.decodeFromString<UnitStateExport>(jsonStr).toUnitState()
        }
        return loadStateLegacy(elevatorIndex).also { state ->
            saveState(elevatorIndex, state)
            clearLegacyKeys(elevatorIndex)
        }
    }

    fun clearElevatorData(elevatorIndex: Int) {
        prefs.edit {
            remove("elevator_${elevatorIndex}_json")
        }
        clearLegacyKeys(elevatorIndex)
    }

    // ======================== 访问时间 / 索引 / 算法 ========================

    fun updateLastAccessTime(index: Int) {
        prefs.edit { putLong(LAST_ACCESS_TIME_PREFIX + index, System.currentTimeMillis()) }
    }

    fun getLastAccessTime(index: Int): Long =
        prefs.getLong(LAST_ACCESS_TIME_PREFIX + index, 0L)

    fun removeLastAccessTime(index: Int) {
        prefs.edit { remove(LAST_ACCESS_TIME_PREFIX + index) }
    }

    fun saveCurrentElevatorIndex(index: Int) {
        prefs.edit { putInt("current_elevator_index", index) }
    }

    fun getCurrentElevatorIndex(): Int =
        prefs.getInt("current_elevator_index", 0)

    fun saveAlgorithmSelection(algorithmOrdinal: Int) {
        prefs.edit { putInt("balance_coefficient_algorithm", algorithmOrdinal) }
    }

    fun getAlgorithmSelection(defaultOrdinal: Int): Int =
        prefs.getInt("balance_coefficient_algorithm", defaultOrdinal)

    // ======================== 平衡系数范围 ========================

    fun saveBalanceRangeSettings(min: Float, max: Float, ideal: Float) {
        prefs.edit {
            putFloat(BALANCE_RANGE_MIN_KEY, min)
            putFloat(BALANCE_RANGE_MAX_KEY, max)
            putFloat(BALANCE_IDEAL_KEY, ideal)
        }
    }

    fun getBalanceRangeMin(): Float = prefs.getFloat(BALANCE_RANGE_MIN_KEY, DEFAULT_BALANCE_RANGE_MIN)
    fun getBalanceRangeMax(): Float = prefs.getFloat(BALANCE_RANGE_MAX_KEY, DEFAULT_BALANCE_RANGE_MAX)
    fun getBalanceIdeal(): Float = prefs.getFloat(BALANCE_IDEAL_KEY, DEFAULT_BALANCE_IDEAL)

    // ======================== 导出/导入支持 ========================

    fun getAllElevatorsForExport(): List<UnitStateExport> {
        return loadInitialElevators().map { it.toExport() }
    }

    fun importElevators(elevators: List<UnitStateExport>, currentIndex: Int, algorithm: Int) {
        val oldCount = prefs.getInt(ELEVATOR_COUNT_KEY, 0)
        for (i in 0 until oldCount) {
            clearElevatorData(i)
        }

        prefs.edit {
            putInt(ELEVATOR_COUNT_KEY, elevators.size)
            putInt("current_elevator_index", currentIndex)
            putInt("balance_coefficient_algorithm", algorithm)
        }

        elevators.forEachIndexed { index, export ->
            prefs.edit {
                putString("elevator_${index}_json", json.encodeToString(export))
            }
        }
    }

    // ======================== 旧格式迁移 ========================

    private fun loadStateLegacy(elevatorIndex: Int): UnitState {
        val prefix = "elevator_${elevatorIndex}_"
        val name = prefs.getString(prefix + "name", null) ?: "电梯 ${elevatorIndex + 1}"
        val creationDate = prefs.getString(prefix + "creationDate", null)
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val customBlockCounts = SnapshotStateList<String>().apply {
            val size = prefs.getInt(prefix + "customBlockCounts_size", 0).coerceAtLeast(0)
            repeat(size) { i -> add(prefs.getString(prefix + "customBlockCount_$i", "") ?: "") }
        }

        val currentReadings = SnapshotStateList<SnapshotStateList<String>>().apply {
            val size0 = prefs.getInt(prefix + "currentReadings_0_size", 0).coerceAtLeast(0)
            add(SnapshotStateList<String>().apply {
                repeat(size0) { i -> add(prefs.getString(prefix + "currentReading_0_$i", "") ?: "") }
            })
            val size1 = prefs.getInt(prefix + "currentReadings_1_size", 0).coerceAtLeast(0)
            add(SnapshotStateList<String>().apply {
                repeat(size1) { i -> add(prefs.getString(prefix + "currentReading_1_$i", "") ?: "") }
            })
        }

        if (customBlockCounts.isEmpty()) {
            customBlockCounts.add("")
            currentReadings.forEach { it.add("") }
        }
        while (currentReadings.size < 2) currentReadings.add(SnapshotStateList())

        return UnitState(
            name = name,
            creationDate = creationDate,
            ratedLoad = prefs.getString(prefix + "ratedLoad", "1050") ?: "1050",
            counterweightWeight = prefs.getString(prefix + "counterweightWeight", "") ?: "",
            counterweightBlockWeight = prefs.getString(prefix + "counterweightBlockWeight", "25") ?: "25",
            manualBalanceCoefficientK = prefs.getString(prefix + "manualBalanceCoefficientK", null),
            useManualBalance = prefs.getBoolean(prefix + "useManualBalance", false),
            useCustomBlockInput = prefs.getBoolean(prefix + "useCustomBlockInput", true),
            customBlockCounts = customBlockCounts,
            customBlockPercentages = SnapshotStateList<Double?>().apply { repeat(customBlockCounts.size) { add(null) } },
            currentReadings = currentReadings
        )
    }

    private fun clearLegacyKeys(elevatorIndex: Int) {
        val prefix = "elevator_${elevatorIndex}_"
        val keysToRemove = prefs.all.keys.filter { it.startsWith(prefix) && it != "elevator_${elevatorIndex}_json" }
        if (keysToRemove.isNotEmpty()) {
            prefs.edit { keysToRemove.forEach { remove(it) } }
        }
    }
}
