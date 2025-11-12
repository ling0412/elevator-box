package com.ling.box.number.safety.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ling.box.number.safety.data.ElevatorData
import kotlinx.serialization.json.Json

object PrefsHelper {
    private const val PREFS_NAME = "ElevatorPrefs"
    private const val KEY_SAVED_ELEVATORS = "savedElevatorsMap"
    private val json = Json { ignoreUnknownKeys = true }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveElevatorMap(context: Context, map: Map<String, ElevatorData>) {
        try {
            val jsonString = json.encodeToString(map)
            getPrefs(context).edit {
                putString(KEY_SAVED_ELEVATORS, jsonString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadElevatorMap(context: Context): Map<String, ElevatorData> {
        val jsonString = getPrefs(context).getString(KEY_SAVED_ELEVATORS, null)
        return if (jsonString != null) {
            try {
                json.decodeFromString<Map<String, ElevatorData>>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }
}

