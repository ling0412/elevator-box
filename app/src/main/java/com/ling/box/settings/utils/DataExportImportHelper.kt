package com.ling.box.settings.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.ling.box.number.safety.data.ElevatorData
import com.ling.box.number.safety.utils.PrefsHelper
import com.ling.box.settings.data.CalculatorExportData
import com.ling.box.settings.data.ElevatorDataExport
import com.ling.box.settings.data.ExportData
import com.ling.box.settings.data.SafetyExportData
import com.ling.box.settings.data.UnitStateExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExportImportHelper {
    private const val PREFS_CALCULATOR = "elevator_calculator_prefs"
    private const val ELEVATOR_COUNT_KEY = "elevator_count"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * 导出所有数据到 JSON 字符串
     */
    suspend fun exportDataToJson(context: Context): String = withContext(Dispatchers.IO) {
        val calculatorPrefs = context.getSharedPreferences(PREFS_CALCULATOR, Context.MODE_PRIVATE)
        
        // 导出磅梯数据
        val calculatorData = exportCalculatorData(calculatorPrefs)
        
        // 导出自检数据
        val safetyData = exportSafetyData(context)
        
        // 创建导出数据对象
        val exportData = ExportData(
            calculatorData = calculatorData,
            safetyData = safetyData
        )
        
        // 序列化为 JSON
        json.encodeToString(ExportData.serializer(), exportData)
    }

    /**
     * 从 JSON 字符串导入数据
     */
    suspend fun importDataFromJson(context: Context, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val exportData = json.decodeFromString<ExportData>(jsonString)
            
            // 导入磅梯数据
            importCalculatorData(context, exportData.calculatorData)
            
            // 导入自检数据
            importSafetyData(context, exportData.safetyData)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从 URI 读取文件并导入
     */
    suspend fun importFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                importDataFromJson(context, jsonString)
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 导出数据到 URI
     */
    suspend fun exportToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = exportDataToJson(context)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { it.write(jsonString) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 导出磅梯数据（计算器数据）
     */
    private fun exportCalculatorData(prefs: SharedPreferences): CalculatorExportData {
        val currentElevatorIndex = prefs.getInt("current_elevator_index", 0)
        val balanceCoefficientAlgorithm = prefs.getInt("balance_coefficient_algorithm", 0)
        val elevatorCount = prefs.getInt(ELEVATOR_COUNT_KEY, 0)
        
        val elevators = mutableListOf<UnitStateExport>()
        for (i in 0 until elevatorCount) {
            val prefix = "elevator_${i}_"
            val name = prefs.getString(prefix + "name", null) ?: "电梯 ${i + 1}"
            val creationDate = prefs.getString(prefix + "creationDate", null) 
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val ratedLoad = prefs.getString(prefix + "ratedLoad", "1050") ?: "1050"
            val counterweightWeight = prefs.getString(prefix + "counterweightWeight", "") ?: ""
            val counterweightBlockWeight = prefs.getString(prefix + "counterweightBlockWeight", "25") ?: "25"
            val manualBalanceCoefficientK = prefs.getString(prefix + "manualBalanceCoefficientK", null)
            val useManualBalance = prefs.getBoolean(prefix + "useManualBalance", false)
            val useCustomBlockInput = prefs.getBoolean(prefix + "useCustomBlockInput", true)
            
            // 导出 customBlockCounts
            val customBlockCountsSize = prefs.getInt(prefix + "customBlockCounts_size", 0)
            val customBlockCounts = mutableListOf<String>()
            for (j in 0 until customBlockCountsSize) {
                customBlockCounts.add(prefs.getString(prefix + "customBlockCount_${j}", "") ?: "")
            }
            
            // 导出 currentReadings
            val currentReadings = mutableListOf<List<String>>()
            // 上行电流
            val size0 = prefs.getInt(prefix + "currentReadings_0_size", 0)
            val upwardReadings = mutableListOf<String>()
            for (j in 0 until size0) {
                upwardReadings.add(prefs.getString(prefix + "currentReading_0_${j}", "") ?: "")
            }
            currentReadings.add(upwardReadings)
            
            // 下行电流
            val size1 = prefs.getInt(prefix + "currentReadings_1_size", 0)
            val downwardReadings = mutableListOf<String>()
            for (j in 0 until size1) {
                downwardReadings.add(prefs.getString(prefix + "currentReading_1_${j}", "") ?: "")
            }
            currentReadings.add(downwardReadings)
            
            elevators.add(
                UnitStateExport(
                    name = name,
                    creationDate = creationDate,
                    ratedLoad = ratedLoad,
                    counterweightWeight = counterweightWeight,
                    counterweightBlockWeight = counterweightBlockWeight,
                    manualBalanceCoefficientK = manualBalanceCoefficientK,
                    useManualBalance = useManualBalance,
                    useCustomBlockInput = useCustomBlockInput,
                    customBlockCounts = customBlockCounts,
                    currentReadings = currentReadings
                )
            )
        }
        
        return CalculatorExportData(
            currentElevatorIndex = currentElevatorIndex,
            balanceCoefficientAlgorithm = balanceCoefficientAlgorithm,
            elevators = elevators
        )
    }

    /**
     * 导入磅梯数据（计算器数据）
     */
    private fun importCalculatorData(context: Context, calculatorData: CalculatorExportData) {
        val prefs = context.getSharedPreferences(PREFS_CALCULATOR, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // 清除现有数据
        val existingCount = prefs.getInt(ELEVATOR_COUNT_KEY, 0)
        for (i in 0 until existingCount) {
            val prefix = "elevator_${i}_"
            editor.remove(prefix + "name")
            editor.remove(prefix + "creationDate")
            editor.remove(prefix + "ratedLoad")
            editor.remove(prefix + "counterweightWeight")
            editor.remove(prefix + "counterweightBlockWeight")
            editor.remove(prefix + "manualBalanceCoefficientK")
            editor.remove(prefix + "useManualBalance")
            editor.remove(prefix + "useCustomBlockInput")
            
            val oldCustomBlockCountsSize = prefs.getInt(prefix + "customBlockCounts_size", 0)
            for (j in 0 until oldCustomBlockCountsSize) {
                editor.remove(prefix + "customBlockCount_${j}")
            }
            editor.remove(prefix + "customBlockCounts_size")
            
            val oldSize0 = prefs.getInt(prefix + "currentReadings_0_size", 0)
            for (j in 0 until oldSize0) {
                editor.remove(prefix + "currentReading_0_${j}")
            }
            editor.remove(prefix + "currentReadings_0_size")
            
            val oldSize1 = prefs.getInt(prefix + "currentReadings_1_size", 0)
            for (j in 0 until oldSize1) {
                editor.remove(prefix + "currentReading_1_${j}")
            }
            editor.remove(prefix + "currentReadings_1_size")
        }
        
        // 导入新数据
        editor.putInt(ELEVATOR_COUNT_KEY, calculatorData.elevators.size)
        editor.putInt("current_elevator_index", calculatorData.currentElevatorIndex)
        editor.putInt("balance_coefficient_algorithm", calculatorData.balanceCoefficientAlgorithm)
        
        calculatorData.elevators.forEachIndexed { index, unitState ->
            val prefix = "elevator_${index}_"
            editor.putString(prefix + "name", unitState.name)
            editor.putString(prefix + "creationDate", unitState.creationDate)
            editor.putString(prefix + "ratedLoad", unitState.ratedLoad)
            editor.putString(prefix + "counterweightWeight", unitState.counterweightWeight)
            editor.putString(prefix + "counterweightBlockWeight", unitState.counterweightBlockWeight)
            editor.putString(prefix + "manualBalanceCoefficientK", unitState.manualBalanceCoefficientK)
            editor.putBoolean(prefix + "useManualBalance", unitState.useManualBalance)
            editor.putBoolean(prefix + "useCustomBlockInput", unitState.useCustomBlockInput)
            
            // 导入 customBlockCounts
            editor.putInt(prefix + "customBlockCounts_size", unitState.customBlockCounts.size)
            unitState.customBlockCounts.forEachIndexed { j, value ->
                editor.putString(prefix + "customBlockCount_${j}", value)
            }
            
            // 导入 currentReadings
            if (unitState.currentReadings.isNotEmpty()) {
                val upwardReadings = unitState.currentReadings.getOrNull(0) ?: emptyList()
                editor.putInt(prefix + "currentReadings_0_size", upwardReadings.size)
                upwardReadings.forEachIndexed { j, value ->
                    editor.putString(prefix + "currentReading_0_${j}", value)
                }
                
                val downwardReadings = unitState.currentReadings.getOrNull(1) ?: emptyList()
                editor.putInt(prefix + "currentReadings_1_size", downwardReadings.size)
                downwardReadings.forEachIndexed { j, value ->
                    editor.putString(prefix + "currentReading_1_${j}", value)
                }
            } else {
                editor.putInt(prefix + "currentReadings_0_size", 0)
                editor.putInt(prefix + "currentReadings_1_size", 0)
            }
        }
        
        editor.apply()
    }

    /**
     * 导出自检数据
     */
    private fun exportSafetyData(context: Context): SafetyExportData {
        val elevatorMap = PrefsHelper.loadElevatorMap(context)
        val exportMap = elevatorMap.mapValues { (_, elevatorData) ->
            ElevatorDataExport(
                speed = elevatorData.speed,
                carBufferCompression = elevatorData.carBufferCompression,
                bufferCompression = elevatorData.bufferCompression,
                bufferDistance = elevatorData.bufferDistance,
                inputCarOvertravel = elevatorData.inputCarOvertravel,
                upperLimitDistance = elevatorData.upperLimitDistance,
                lowerLimitDistance = elevatorData.lowerLimitDistance,
                mainGuideDistance = elevatorData.mainGuideDistance,
                auxGuideDistance = elevatorData.auxGuideDistance,
                carGuideTravel = elevatorData.carGuideTravel,
                standingHeightTravel = elevatorData.standingHeightTravel,
                highestComponentTravelA = elevatorData.highestComponentTravelA,
                highestComponentTravelB = elevatorData.highestComponentTravelB,
                counterweightGuideTravel = elevatorData.counterweightGuideTravel,
                lowestHorizontalDistance = elevatorData.lowestHorizontalDistance,
                lowestVerticalDistance = elevatorData.lowestVerticalDistance,
                pitHighestDistance = elevatorData.pitHighestDistance,
                ironCounterweight = elevatorData.ironCounterweight,
                concreteCounterweight = elevatorData.concreteCounterweight,
                counterweightHeight = elevatorData.counterweightHeight
            )
        }
        
        return SafetyExportData(elevators = exportMap)
    }

    /**
     * 导入自检数据
     */
    private fun importSafetyData(context: Context, safetyData: SafetyExportData) {
        val importMap = safetyData.elevators.mapValues { (_, exportData) ->
            ElevatorData(
                speed = exportData.speed,
                carBufferCompression = exportData.carBufferCompression,
                bufferCompression = exportData.bufferCompression,
                bufferDistance = exportData.bufferDistance,
                inputCarOvertravel = exportData.inputCarOvertravel,
                upperLimitDistance = exportData.upperLimitDistance,
                lowerLimitDistance = exportData.lowerLimitDistance,
                mainGuideDistance = exportData.mainGuideDistance,
                auxGuideDistance = exportData.auxGuideDistance,
                carGuideTravel = exportData.carGuideTravel,
                standingHeightTravel = exportData.standingHeightTravel,
                highestComponentTravelA = exportData.highestComponentTravelA,
                highestComponentTravelB = exportData.highestComponentTravelB,
                counterweightGuideTravel = exportData.counterweightGuideTravel,
                lowestHorizontalDistance = exportData.lowestHorizontalDistance,
                lowestVerticalDistance = exportData.lowestVerticalDistance,
                pitHighestDistance = exportData.pitHighestDistance,
                ironCounterweight = exportData.ironCounterweight,
                concreteCounterweight = exportData.concreteCounterweight,
                counterweightHeight = exportData.counterweightHeight
            )
        }
        
        PrefsHelper.saveElevatorMap(context, importMap)
    }

    /**
     * 生成导出文件名
     */
    fun generateExportFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "elevator_data_backup_${dateFormat.format(Date())}.json"
    }
}

