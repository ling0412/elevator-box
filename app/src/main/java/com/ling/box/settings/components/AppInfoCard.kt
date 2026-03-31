package com.ling.box.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ling.box.R
import com.ling.box.update.utils.getAppVersionName

@Composable
fun AppInfoCard(
    isCheckingForUpdate: Boolean,
    onCheckUpdateClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onSourceCodeClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            VersionItem(currentVersion = getAppVersionName(context))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            SettingItem(
                icon = Icons.Filled.Update,
                title = stringResource(R.string.check_update),
                subtitle = if (isCheckingForUpdate) stringResource(R.string.checking_update) else stringResource(R.string.current_version_format, getAppVersionName(context)),
                onClick = onCheckUpdateClick,
                trailingContent = {
                    if (isCheckingForUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            SettingItem(
                icon = Icons.Filled.Code,
                title = stringResource(R.string.source_code),
                subtitle = stringResource(R.string.source_code_subtitle),
                onClick = onSourceCodeClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            SettingItem(
                icon = Icons.Filled.Description,
                title = stringResource(R.string.open_source_license),
                subtitle = stringResource(R.string.open_source_license_subtitle),
                onClick = onLicenseClick
            )
        }
    }
}

