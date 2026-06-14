package com.ling.box.settings.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ling.box.R
import com.ling.box.update.config.UpdateConfig
import com.ling.box.update.data.UpdateInfo
import com.ling.box.update.utils.getAppVersionName
import kotlinx.coroutines.launch

@Composable
fun SettingsUpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_new_version_found)) },
        text = {
            Column {
                Text(stringResource(R.string.update_new_version_label, updateInfo.latestVersion), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.update_current_version_label, getAppVersionName(context)), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.update_release_notes), fontWeight = FontWeight.Medium)
                Text(updateInfo.releaseNotes ?: stringResource(R.string.none), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, UpdateConfig.DOWNLOAD_URL.toUri())
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.toast_cannot_open_browser))
                    }
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.update_go_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        }
    )
}

