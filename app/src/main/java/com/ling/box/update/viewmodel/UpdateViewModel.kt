package com.ling.box.update.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ling.box.update.data.UpdateInfo
import com.ling.box.update.utils.compareVersions
import com.ling.box.update.utils.fetchLatestReleaseInfo
import com.ling.box.update.utils.getAppVersionName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "UpdateCheck"
        private const val PREFS_NAME = "app_settings"
        private const val KEY_LAST_CHECK_TIME = "last_update_check_time"
        private val CHECK_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun dismissDialog() {
        _showDialog.value = false
    }

    fun clearMessage() {
        _message.value = null
    }

    fun tryAutoCheck() {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        val elapsed = System.currentTimeMillis() - lastCheck
        if (elapsed > CHECK_INTERVAL_MS) {
            Log.d(TAG, "Performing automatic update check on startup.")
            checkForUpdates(isAutomatic = true)
        } else {
            Log.d(TAG, "Skipping automatic check (last check ${TimeUnit.MILLISECONDS.toHours(elapsed)}h ago).")
        }
    }

    fun checkForUpdates(isAutomatic: Boolean = false) {
        if (_isChecking.value) return
        _isChecking.value = true

        viewModelScope.launch {
            val context = getApplication<Application>()
            val currentVersion = getAppVersionName(context)
            Log.d(TAG, "Current Version: $currentVersion (Automatic: $isAutomatic)")

            val result = fetchLatestReleaseInfo()

            withContext(Dispatchers.Main) {
                _isChecking.value = false
                prefs.edit { putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()) }

                result.onSuccess { info ->
                    Log.d(TAG, "Latest Version Found: ${info.latestVersion}")
                    if (compareVersions(info.latestVersion, currentVersion) > 0) {
                        _updateInfo.value = info
                        _showDialog.value = true
                    } else if (!isAutomatic) {
                        _message.value = "当前已是最新版本 ($currentVersion)"
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Update check failed (Automatic: $isAutomatic)", e)
                    if (!isAutomatic) {
                        _message.value = "检查更新失败: ${e.message}"
                    }
                }
            }
        }
    }
}
