package com.ling.box.number.safety.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun NumberInputField(
    label: String,
    initialValue: Float,
    onValueChange: (Float) -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var text by remember(initialValue) {
        mutableStateOf(if (initialValue != 0f) initialValue.toString() else "")
    }

    fun parseAndCallback() {
        val parsedValue = text.toFloatOrNull()
        if (parsedValue != null) {
            onValueChange(parsedValue)
        } else if (text.isBlank()) {
            onValueChange(0.0f)
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                parseAndCallback()
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        ),
        singleLine = true,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) {
                    parseAndCallback()
                }
            }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun IntegerInputField(
    label: String,
    initialValue: Int,
    onValueChange: (Int) -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var text by remember(initialValue) {
        mutableStateOf(if (initialValue != 0) initialValue.toString() else "")
    }

    fun parseAndCallback() {
        val filteredText = text.filter { it.isDigit() }
        val intValue = filteredText.toIntOrNull() ?: 0
        onValueChange(intValue)
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText.filter { it.isDigit() }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                parseAndCallback()
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        ),
        singleLine = true,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) {
                    parseAndCallback()
                }
            }
    )
}

