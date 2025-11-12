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
        val newCount = unitStateList.size
        val oldCount = prefs.getInt(ELEVATOR_COUNT_KEY, 0)
        
        // 清理超出范围的旧电梯数据（防止删除电梯后残留数据）
        if (oldCount > newCount) {
            for (i in newCount until oldCount) {
                clearElevatorData(i)
            }
        }
        
        editor.putInt(ELEVATOR_COUNT_KEY, newCount).apply()
        unitStateList.forEachIndexed { index, unitState ->
            saveState(index, unitState)
        }
    }
    
    /**
     * 保存单个电梯状态
     * 注意：在保存动态列表前会清理旧的键值对，防止存储膨胀
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

            // 清理旧的 customBlockCounts 数据（防止列表缩小时残留旧数据）
            val oldCustomBlockCountsSize = prefs.getInt(prefix + "customBlockCounts_size", 0)
            val newCustomBlockCountsSize = state.customBlockCounts.size
            if (oldCustomBlockCountsSize > newCustomBlockCountsSize) {
                for (i in newCustomBlockCountsSize until oldCustomBlockCountsSize) {
                    remove(prefix + "customBlockCount_${i}")
                }
            }
            
            // 动态保存 customBlockCounts
            putInt(prefix + "customBlockCounts_size", newCustomBlockCountsSize)
            state.customBlockCounts.forEachIndexed { index, value -> 
                putString(prefix + "customBlockCount_${index}", value) 
            }

            // 清理旧的 currentReadings 数据（防止列表缩小时残留旧数据）
            val oldSize0 = prefs.getInt(prefix + "currentReadings_0_size", 0)
            val newSize0 = state.currentReadings.getOrNull(0)?.size ?: 0
            if (oldSize0 > newSize0) {
                for (i in newSize0 until oldSize0) {
                    remove(prefix + "currentReading_0_${i}")
                }
            }
            
            val oldSize1 = prefs.getInt(prefix + "currentReadings_1_size", 0)
            val newSize1 = state.currentReadings.getOrNull(1)?.size ?: 0
            if (oldSize1 > newSize1) {
                for (i in newSize1 until oldSize1) {
                    remove(prefix + "currentReading_1_${i}")
                }
            }

            // 动态保存 currentReadings
            putInt(prefix + "currentReadings_0_size", newSize0)
            state.currentReadings.getOrNull(0)?.forEachIndexed { pointIndex, value -> 
                putString(prefix + "currentReading_0_${pointIndex}", value) 
            }

            putInt(prefix + "currentReadings_1_size", newSize1)
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
    
    /**
     * 清理指定电梯的所有数据
     * 用于删除电梯时完全清理所有相关键，防止存储残留
     */
    fun clearElevatorData(elevatorIndex: Int) {
        val prefix = "elevator_${elevatorIndex}_"
        with(editor) {
            // 清理基本字段
            remove(prefix + "name")
            remove(prefix + "creationDate")
            remove(prefix + "ratedLoad")
            remove(prefix + "counterweightWeight")
            remove(prefix + "counterweightBlockWeight")
            remove(prefix + "manualBalanceCoefficientK")
            remove(prefix + "useManualBalance")
            remove(prefix + "useCustomBlockInput")
            
            // 清理 customBlockCounts
            val customBlockCountsSize = prefs.getInt(prefix + "customBlockCounts_size", 0)
            for (i in 0 until customBlockCountsSize) {
                remove(prefix + "customBlockCount_${i}")
            }
            remove(prefix + "customBlockCounts_size")
            
            // 清理 currentReadings
            val size0 = prefs.getInt(prefix + "currentReadings_0_size", 0)
            for (i in 0 until size0) {
                remove(prefix + "currentReading_0_${i}")
            }
            remove(prefix + "currentReadings_0_size")
            
            val size1 = prefs.getInt(prefix + "currentReadings_1_size", 0)
            for (i in 0 until size1) {
                remove(prefix + "currentReading_1_${i}")
            }
            remove(prefix + "currentReadings_1_size")
            
            apply()
        }
    }
}



