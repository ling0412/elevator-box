package com.ling.box.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ling.box.update.utils.getAppVersionName

/**
 * 版本迁移辅助类
 * 用于处理应用版本升级时的数据迁移
 * 
 * 使用说明：
 * 1. 每次发布新版本时，如果数据结构有变化，需要添加相应的迁移方法
 * 2. 在 migrateFromVersion() 中添加新的版本迁移条件
 * 3. 迁移方法应该幂等（可以安全地多次执行）
 * 4. 迁移失败不应该阻止应用启动
 */
object VersionMigrationHelper {
    private const val TAG = "VersionMigration"
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LAST_VERSION = "last_app_version"
    
    /**
     * 执行版本迁移检查
     * 在应用启动时调用，检查是否需要执行数据迁移
     */
    fun performMigrationIfNeeded(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentVersion = getAppVersionName(context)
            val lastVersion = prefs.getString(KEY_LAST_VERSION, null)
            
            Log.d(TAG, "当前版本: $currentVersion, 上次版本: $lastVersion")
            
            // 如果是首次安装或版本相同，不需要迁移
            if (lastVersion == null || lastVersion == currentVersion) {
                // 更新版本号
                prefs.edit().putString(KEY_LAST_VERSION, currentVersion).apply()
                return
            }
            
            // 需要迁移
            Log.i(TAG, "检测到版本升级: $lastVersion -> $currentVersion")
            
            // 执行迁移逻辑
            migrateFromVersion(context, lastVersion, currentVersion)
            
            // 更新版本号
            prefs.edit().putString(KEY_LAST_VERSION, currentVersion).apply()
            
            Log.i(TAG, "版本迁移完成")
        } catch (e: Exception) {
            Log.e(TAG, "版本迁移失败", e)
            // 即使迁移失败，也不应该阻止应用启动
            // 可以选择清除数据或使用默认值
        }
    }
    
    /**
     * 根据版本号执行相应的迁移逻辑
     */
    private fun migrateFromVersion(context: Context, fromVersion: String, toVersion: String) {
        try {
            // 比较版本号
            val fromVersionCode = parseVersionCode(fromVersion)
            val toVersionCode = parseVersionCode(toVersion)
            
            // 按版本顺序执行迁移（从旧到新）
            // 注意：迁移应该按顺序执行，因为可能跨多个版本升级
            
            // v1.4.0 -> v1.5.0 迁移
            if (fromVersionCode < 150 && toVersionCode >= 150) {
                Log.i(TAG, "执行 v1.4.0 -> v1.5.0 迁移")
                migrateFromV1_4_0ToV1_5_0(context)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "迁移过程出错", e)
            throw e
        }
    }
    
    /**
     * 从v1.4.0迁移到v1.5.0
     * v1.5.0新增了customBlockPercentages字段，需要确保数据兼容性
     */
    private fun migrateFromV1_4_0ToV1_5_0(context: Context) {
        try {
            val calculatorPrefs = context.getSharedPreferences("elevator_calculator_prefs", Context.MODE_PRIVATE)
            val elevatorCount = calculatorPrefs.getInt("elevator_count", 0)
            
            Log.d(TAG, "开始迁移 $elevatorCount 个电梯的数据")
            
            // v1.5.0新增了customBlockPercentages字段
            // 由于这个字段在加载时会自动初始化为空列表，所以不需要特殊处理
            // 但我们需要确保数据加载时不会因为格式不兼容而崩溃
            
            // 验证并修复可能损坏的数据
            for (i in 0 until elevatorCount) {
                val prefix = "elevator_${i}_"
                
                // 检查必要字段是否存在
                val name = calculatorPrefs.getString(prefix + "name", null)
                if (name == null) {
                    Log.w(TAG, "电梯 $i 的数据不完整，跳过")
                    continue
                }
                
                // 确保customBlockCounts_size存在
                val customBlockCountsSize = calculatorPrefs.getInt(prefix + "customBlockCounts_size", 0)
                if (customBlockCountsSize < 0) {
                    // 修复负数大小
                    calculatorPrefs.edit().putInt(prefix + "customBlockCounts_size", 0).apply()
                    Log.w(TAG, "修复电梯 $i 的customBlockCounts_size")
                }
                
                // 确保currentReadings大小有效
                val size0 = calculatorPrefs.getInt(prefix + "currentReadings_0_size", 0)
                val size1 = calculatorPrefs.getInt(prefix + "currentReadings_1_size", 0)
                if (size0 < 0) {
                    calculatorPrefs.edit().putInt(prefix + "currentReadings_0_size", 0).apply()
                    Log.w(TAG, "修复电梯 $i 的currentReadings_0_size")
                }
                if (size1 < 0) {
                    calculatorPrefs.edit().putInt(prefix + "currentReadings_1_size", 0).apply()
                    Log.w(TAG, "修复电梯 $i 的currentReadings_1_size")
                }
            }
            
            Log.i(TAG, "v1.4.0 -> v1.5.0 迁移完成")
        } catch (e: Exception) {
            Log.e(TAG, "v1.4.0 -> v1.5.0 迁移失败", e)
            // 如果迁移失败，可以选择清除数据或保留原数据
            // 这里我们选择保留原数据，让应用尝试加载
        }
    }
    
    /**
     * 解析版本号为数字代码
     * 例如: "1.5.0" -> 150, "1.4.0" -> 140
     */
    private fun parseVersionCode(version: String): Int {
        return try {
            val parts = version.split(".")
            if (parts.size >= 2) {
                val major = parts[0].toIntOrNull() ?: 0
                val minor = parts[1].toIntOrNull() ?: 0
                val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
                major * 100 + minor * 10 + patch
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析版本号失败: $version", e)
            0
        }
    }
    
    /**
     * 清除所有数据（仅在严重错误时使用）
     */
    fun clearAllData(context: Context) {
        try {
            Log.w(TAG, "清除所有应用数据")
            val calculatorPrefs = context.getSharedPreferences("elevator_calculator_prefs", Context.MODE_PRIVATE)
            val safetyPrefs = context.getSharedPreferences("ElevatorPrefs", Context.MODE_PRIVATE)
            val appSettingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            
            calculatorPrefs.edit().clear().apply()
            safetyPrefs.edit().clear().apply()
            // 不清除app_settings，保留用户设置
            
            Log.i(TAG, "数据清除完成")
        } catch (e: Exception) {
            Log.e(TAG, "清除数据失败", e)
        }
    }
}
