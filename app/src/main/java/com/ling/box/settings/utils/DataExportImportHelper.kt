package com.ling.box.settings.utils

import android.content.Context
import android.net.Uri
import com.ling.box.calculator.repository.ElevatorRepository
import com.ling.box.number.safety.data.ElevatorData
import com.ling.box.number.safety.utils.PrefsHelper
import com.ling.box.settings.data.CalculatorExportData
import com.ling.box.settings.data.ElevatorDataExport
import com.ling.box.settings.data.ExportData
import com.ling.box.settings.data.SafetyExportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DataExportImportHelper {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportDataToJson(context: Context): String = withContext(Dispatchers.IO) {
        val repository = ElevatorRepository(context)

        val calculatorData = CalculatorExportData(
            currentElevatorIndex = repository.getCurrentElevatorIndex(),
            balanceCoefficientAlgorithm = repository.getAlgorithmSelection(0),
            elevators = repository.getAllElevatorsForExport()
        )

        val exportData = ExportData(
            calculatorData = calculatorData,
            safetyData = exportSafetyData(context)
        )

        json.encodeToString(ExportData.serializer(), exportData)
    }

    suspend fun importDataFromJson(context: Context, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val exportData = json.decodeFromString<ExportData>(jsonString)

            val repository = ElevatorRepository(context)
            repository.importElevators(
                elevators = exportData.calculatorData.elevators,
                currentIndex = exportData.calculatorData.currentElevatorIndex,
                algorithm = exportData.calculatorData.balanceCoefficientAlgorithm
            )

            importSafetyData(context, exportData.safetyData)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

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

    private fun exportSafetyData(context: Context): SafetyExportData {
        val elevatorMap = PrefsHelper.loadElevatorMap(context)
        val exportMap = elevatorMap.mapValues { (_, data) ->
            ElevatorDataExport(
                speed = data.speed,
                carBufferCompression = data.carBufferCompression,
                bufferCompression = data.bufferCompression,
                bufferDistance = data.bufferDistance,
                inputCarOvertravel = data.inputCarOvertravel,
                upperLimitDistance = data.upperLimitDistance,
                lowerLimitDistance = data.lowerLimitDistance,
                mainGuideDistance = data.mainGuideDistance,
                auxGuideDistance = data.auxGuideDistance,
                carGuideTravel = data.carGuideTravel,
                standingHeightTravel = data.standingHeightTravel,
                highestComponentTravelA = data.highestComponentTravelA,
                highestComponentTravelB = data.highestComponentTravelB,
                counterweightGuideTravel = data.counterweightGuideTravel,
                lowestHorizontalDistance = data.lowestHorizontalDistance,
                lowestVerticalDistance = data.lowestVerticalDistance,
                pitHighestDistance = data.pitHighestDistance,
                ironCounterweight = data.ironCounterweight,
                concreteCounterweight = data.concreteCounterweight,
                counterweightHeight = data.counterweightHeight
            )
        }
        return SafetyExportData(elevators = exportMap)
    }

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

    fun generateExportFileName(): String {
        val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.getDefault())
        return "elevator_data_backup_${LocalDateTime.now().format(dateFormat)}.json"
    }
}
