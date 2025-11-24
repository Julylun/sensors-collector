package com.example.sensorcollector.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    requireHoldToStop: Boolean = false,
    onHoldToStop: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    LaunchedEffect(isPressed, requireHoldToStop, isRecording) {
        if (requireHoldToStop && isRecording && isPressed) {
            delay(3000)
            if (isRecording) {
                onHoldToStop?.invoke()
            }
        }
    }
    
    Button(
        onClick = {
            if (requireHoldToStop && isRecording) {
                // Ignore short taps while hold-to-stop is required
                return@Button
            }
            onClick()
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        interactionSource = interactionSource
    ) {
        Text(if (isRecording) "Dừng" else "Ghi")
    }
}

