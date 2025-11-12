package com.ling.box.update.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.ling.box.update.config.UpdateConfig
import com.ling.box.update.data.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

suspend fun fetchLatestReleaseInfo(owner: String = UpdateConfig.GITHUB_OWNER, repo: String = UpdateConfig.GITHUB_REPO): Result<UpdateInfo> = withContext(Dispatchers.IO) {
    val urlString = UpdateConfig.getLatestReleaseApiUrl()
    Log.d("UpdateCheck", "Fetching URL with OkHttp: $urlString")
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(urlString)
        .header("Accept", "application/vnd.github.v3+json")
        .build()

    try {
        val response: Response = client.newCall(request).execute()
        val responseCode = response.code

        Log.d("UpdateCheck", "OkHttp Response Code: $responseCode")

        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            Log.d("UpdateCheck", "OkHttp Response Body: $responseBody")
            responseBody?.let {
                val jsonObject = JSONObject(it)
                val tagName = jsonObject.getString("tag_name").removePrefix("v")
                val body = jsonObject.optString("body", "没有提供更新说明。")
                val htmlUrl = jsonObject.getString("html_url")
                return@withContext Result.success(UpdateInfo(tagName, body, htmlUrl))
            } ?: return@withContext Result.failure(Exception("响应体为空"))
        } else {
            Log.e("UpdateCheck", "OkHttp Error Response: ${response.body?.string()}")
            return@withContext Result.failure(Exception("请求失败，响应码: $responseCode"))
        }
    } catch (e: Exception) {
        Log.e("UpdateCheck", "OkHttp Network or JSON parsing error", e)
        return@withContext Result.failure(e)
    }
}

fun compareVersions(version1: String, version2: String): Int {
    val parts1 = version1.split('.').mapNotNull { it.toIntOrNull() }
    val parts2 = version2.split('.').mapNotNull { it.toIntOrNull() }
    val maxParts = maxOf(parts1.size, parts2.size)

    for (i in 0 until maxParts) {
        val part1 = parts1.getOrElse(i) { 0 }
        val part2 = parts2.getOrElse(i) { 0 }
        if (part1 != part2) {
            return part1.compareTo(part2)
        }
    }
    return 0
}

fun getAppVersionName(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "未知版本"
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e("UpdateUtils", "Could not get package info: ${e.message}")
        "未知版本"
    }
}

