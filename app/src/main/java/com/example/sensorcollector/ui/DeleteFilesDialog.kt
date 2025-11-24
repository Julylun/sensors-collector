package com.example.sensorcollector.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeleteFilesDialog(
    files: List<RecordingFileInfo>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(files) {
        selectedPaths = emptySet()
    }
    val grouped = remember(files) { files.groupBy { it.type }.toSortedMap() }
    val formatter = remember {
        SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xóa file đã chọn") },
        text = {
            if (files.isEmpty()) {
                Text("Không có file nào để xóa.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    grouped.forEach { (type, items) ->
                        Text(
                            text = type,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        items.forEach { file ->
                            val isSelected = selectedPaths.contains(file.path)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedPaths = if (checked) {
                                            selectedPaths + file.path
                                        } else {
                                            selectedPaths - file.path
                                        }
                                    }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${formatSize(file.sizeBytes)} • ${formatter.format(Date(file.lastModified))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedPaths.toList()) },
                enabled = selectedPaths.isNotEmpty()
            ) {
                Text("Xóa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

private fun formatSize(bytes: Long): String {
    val kilo = bytes / 1024.0
    if (kilo < 1024) {
        return String.format(Locale.getDefault(), "%.1f KB", kilo)
    }
    val mega = kilo / 1024.0
    return String.format(Locale.getDefault(), "%.1f MB", mega)
}

