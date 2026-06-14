package com.ling.box.update.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ling.box.R
import com.ling.box.update.config.UpdateConfig
import com.ling.box.update.data.UpdateInfo
import com.ling.box.update.utils.getAppVersionName

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_new_version_found)) },
        text = {
            Column {
                Text(stringResource(R.string.update_new_version_label, updateInfo.latestVersion))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.update_current_version_label, getAppVersionName(context)))
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.update_release_notes))
                Text(updateInfo.releaseNotes ?: stringResource(R.string.none))
            }
        },
        confirmButton = {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, UpdateConfig.DOWNLOAD_URL.toUri())
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Error handled by caller
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.update_go_download))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        }
    )
}

