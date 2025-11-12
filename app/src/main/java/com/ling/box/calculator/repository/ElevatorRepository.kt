package com.ling.box.calculator.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ling.box.calculator.model.UnitState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 电梯数据仓库
 * 负责电梯数据的持久化存储和加载
 */
class ElevatorRepository(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "elevator_calculator_prefs"
        private const val ELEVATOR_COUNT_KEY = "elevator_count"
        private const val LAST_ACCESS_TIME_PREFIX = "last_access_time_"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()
    
    /**
     * 保存电梯列表
     */
    fun saveElevatorList(unitStateList: List<UnitState>) {
        editor.putInt(ELEVATOR_COUNT_KEY, unitStateList.size).apply()
        unitStateList.forEachIndexed { index, unitState ->
            saveState(index, unitState)
        }
    }
    
    /**
     * 保存单个电梯状态
     */
    fun saveState(elevatorIndex: Int, state: UnitState) {
        val prefix = "elevator_${elevatorIndex}_"
        with(editor) {
            putString(prefix + "name", state.name)
            putString(prefix + "creationDate", state.creationDate)
            putString(prefix + "ratedLoad", state.ratedLoad)
            putString(prefix + "counterweightWeight", state.counterweightWeight)
            putString(prefix + "counterweightBlockWeight", state.counterweightBlockWeight)
            putString(prefix + "manualBalanceCoefficientK", state.manualBalanceCoefficientK)
            putBoolean(prefix + "useManualBalance", state.useManualBalance)
            putBoolean(prefix + "useCustomBlockInput", state.useCustomBlockInput)

            // 动态保存 customBlockCounts
            putInt(prefix + "customBlockCounts_size", state.customBlockCounts.size)
            state.customBlockCounts.forEachIndexed { index, value -> 
                putString(prefix + "customBlockCount_${index}", value) 
            }

            // 动态保存 currentReadings
            putInt(prefix + "currentReadings_0_size", state.currentReadings.getOrNull(0)?.size ?: 0)
            state.currentReadings.getOrNull(0)?.forEachIndexed { pointIndex, value -> 
                putString(prefix + "currentReading_0_${pointIndex}", value) 
            }

            putInt(prefix + "currentReadings_1_size", state.currentReadings.getOrNull(1)?.size ?: 0)
            state.currentReadings.getOrNull(1)?.forEachIndexed { pointIndex, value -> 
                putString(prefix + "currentReading_1_${pointIndex}", value) 
            }

            apply()
        }
    }
    
    /**
     * 加载所有电梯
     */
    fun loadInitialElevators(): List<UnitState> {
        val count = prefs.getInt(ELEVATOR_COUNT_KEY, 0)
        val result = mutableListOf<UnitState>()
        if (count > 0) {
            for (i in 0 until count) {
                result.add(loadState(i))
            }
        }
        return result
    }
    
    /**
     * 加载单个电梯状态
     */
    private fun loadState(elevatorIndex: Int): UnitState {
        val prefix = "elevator_${elevatorIndex}_"
        val loadedName = prefs.getString(prefix + "name", null)
        val defaultName = loadedName ?: "电梯 ${elevatorIndex + 1}"
        val loadedCreationDate = prefs.getString(prefix + "creationDate", null)
        val defaultCreationDate = loadedCreationDate 
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val loadedCustomBlockCounts = SnapshotStateList<String>().apply {
            val size = prefs.getInt(prefix + "customBlockCounts_size", 0)
            repeat(size) { index -> 
                add(prefs.getString(prefix + "customBlockCount_${index}", "") ?: "") 
            }
        }

        val loadedCurrentReadings = SnapshotStateList<SnapshotStateList<String>>().apply {
            // 上行电流
            val size0 = prefs.getInt(prefix + "currentReadings_0_size", 0)
            add(SnapshotStateList<String>().apply {
                repeat(size0) { pointIndex -> 
                    add(prefs.getString(prefix + "currentReading_0_${pointIndex}", "") ?: "") 
                }
            })
            // 下行电流
            val size1 = prefs.getInt(prefix + "currentReadings_1_size", 0)
            add(SnapshotStateList<String>().apply {
                repeat(size1) { pointIndex -> 
                    add(prefs.getString(prefix + "currentReading_1_${pointIndex}", "") ?: "") 
                }
            })
        }

        // 如果加载后 customBlockCounts 为空，确保添加一个初始空行
        if (loadedCustomBlockCounts.isEmpty()) {
            loadedCustomBlockCounts.add("")
            loadedCurrentReadings.forEach { it.add("") }
        }

        return UnitState(
            name = defaultName,
            creationDate = defaultCreationDate,
            ratedLoad = prefs.getString(prefix + "ratedLoad", "1050") ?: "1050",
            counterweightWeight = prefs.getString(prefix + "counterweightWeight", "") ?: "",
            counterweightBlockWeight = prefs.getString(prefix + "counterweightBlockWeight", "25") ?: "25",
            manualBalanceCoefficientK = prefs.getString(prefix + "manualBalanceCoefficientK", null),
            useManualBalance = prefs.getBoolean(prefix + "useManualBalance", false),
            useCustomBlockInput = prefs.getBoolean(prefix + "useCustomBlockInput", true),
            customBlockCounts = loadedCustomBlockCounts,
            customBlockPercentages = SnapshotStateList<Double?>().apply {
                repeat(loadedCustomBlockCounts.size) { add(null) }
            },
            currentReadings = loadedCurrentReadings,
            balanceCoefficientK = null,
            balanceCoefficient = null,
            recommendedBlocksMessage = null,
            hasActualIntersection = false,
            upwardCurrentPoints = SnapshotStateList(),
            downwardCurrentPoints = SnapshotStateList()
        )
    }
    
    /**
     * 更新最后访问时间
     */
    fun updateLastAccessTime(index: Int) {
        val currentTime = System.currentTimeMillis()
        editor.putLong(LAST_ACCESS_TIME_PREFIX + index, currentTime).apply()
    }
    
    /**
     * 获取最后访问时间
     */
    fun getLastAccessTime(index: Int): Long {
        return prefs.getLong(LAST_ACCESS_TIME_PREFIX + index, 0L)
    }
    
    /**
     * 删除最后访问时间记录
     */
    fun removeLastAccessTime(index: Int) {
        editor.remove(LAST_ACCESS_TIME_PREFIX + index).apply()
    }
    
    /**
     * 保存当前电梯索引
     */
    fun saveCurrentElevatorIndex(index: Int) {
        editor.putInt("current_elevator_index", index).apply()
    }
    
    /**
     * 获取当前电梯索引
     */
    fun getCurrentElevatorIndex(): Int {
        return prefs.getInt("current_elevator_index", 0)
    }
    
    /**
     * 保存算法选择
     */
    fun saveAlgorithmSelection(algorithmOrdinal: Int) {
        editor.putInt("balance_coefficient_algorithm", algorithmOrdinal).apply()
    }
    
    /**
     * 获取算法选择
     */
    fun getAlgorithmSelection(defaultOrdinal: Int): Int {
        return prefs.getInt("balance_coefficient_algorithm", defaultOrdinal)
    }
}



