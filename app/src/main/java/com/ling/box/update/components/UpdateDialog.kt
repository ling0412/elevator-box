package com.ling.box.update.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ling.box.update.config.UpdateConfig
import com.ling.box.update.data.UpdateInfo
import com.ling.box.update.utils.getAppVersionName

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本") },
        text = {
            Column {
                Text("新版本: ${updateInfo.latestVersion}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("当前版本: ${getAppVersionName(context)}")
                Spacer(modifier = Modifier.height(16.dp))
                Text("更新说明:")
                Text(updateInfo.releaseNotes ?: "无")
            }
        },
        confirmButton = {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, UpdateConfig.DOWNLOAD_URL.toUri())
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }) {
                Text("前往下载")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}

