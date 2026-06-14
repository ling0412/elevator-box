package com.ling.box.settings.components

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.ling.box.R
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (showDialog) {
        BasicAlertDialog(
            onDismissRequest = onDismiss,
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colorScheme.primaryContainer)
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.title_open_source_license),
                                style = MaterialTheme.typography.headlineSmall,
                                color = colorScheme.onPrimaryContainer
                            )
                        }

                        val libraries by produceLibraries(R.raw.aboutlibraries)

                        LibrariesContainer(
                            libraries = libraries,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            onLibraryClick = { library ->
                                val projectUrl = library.website

                                if (projectUrl != null && projectUrl.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_VIEW, projectUrl.toUri())
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("LicenseDialog", "无法打开项目链接: $e")
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.toast_cannot_open_link))
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.toast_no_link))
                                    }
                                }
                            }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = colorScheme.primary
                                )
                            ) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    }
                }
            }
        )
    }
}

