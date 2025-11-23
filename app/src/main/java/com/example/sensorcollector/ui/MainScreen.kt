package com.example.sensorcollector.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensorcollector.ui.components.RecordButton
import com.example.sensorcollector.ui.components.SensorDisplay
import com.example.sensorcollector.ui.components.SensorToggle
import com.google.gson.GsonBuilder


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: SensorViewModel = viewModel()
) {
    val context = LocalContext.current
    
    val isRecording by viewModel.isRecording.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val delayCountdown by viewModel.delayCountdown.collectAsState()
    val recordingTime by viewModel.recordingTime.collectAsState()
    val delayBeforeRecording by viewModel.delayBeforeRecording.collectAsState()
    val beepEnabled by viewModel.beepEnabled.collectAsState()
    val samplingInterval by viewModel.samplingInterval.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val accelerometerEnabled by viewModel.accelerometerEnabled.collectAsState()
    val gyroscopeEnabled by viewModel.gyroscopeEnabled.collectAsState()
    val ambientLightEnabled by viewModel.ambientLightEnabled.collectAsState()
    val proximityEnabled by viewModel.proximityEnabled.collectAsState()
    
    val accelerometerValue by viewModel.accelerometerValue.collectAsState()
    val gyroscopeValue by viewModel.gyroscopeValue.collectAsState()
    val ambientLightValue by viewModel.ambientLightValue.collectAsState()
    val proximityValue by viewModel.proximityValue.collectAsState()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var recordingTimeInput by remember { mutableStateOf(recordingTime.toString()) }
    var delayBeforeRecordingInput by remember { mutableStateOf(delayBeforeRecording.toString()) }
    var samplingIntervalInput by remember { mutableStateOf(samplingInterval.toString()) }
    
    val lastRecording by viewModel.lastRecording.collectAsState()
    
    val types = listOf("fall", "jump", "walk", "running", "idle", "shake", "other")
    val samplingIntervals = listOf(0L, 20L, 50L, 100L, 200L, 500L) // 0 = continuous
    
    LaunchedEffect(recordingTime) {
        recordingTimeInput = recordingTime.toString()
    }
    
    LaunchedEffect(delayBeforeRecording) {
        delayBeforeRecordingInput = delayBeforeRecording.toString()
    }
    
    LaunchedEffect(samplingInterval) {
        samplingIntervalInput = samplingInterval.toString()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor Collector") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Record/Stop Button
            RecordButton(
                isRecording = isRecording || delayCountdown != null,
                onClick = {
                    if (isRecording || delayCountdown != null) {
                        viewModel.stopRecording()
                    } else {
                        val time = recordingTimeInput.toIntOrNull() ?: 30
                        viewModel.setRecordingTime(time)
                        viewModel.startRecording()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Delay Countdown Display
            if (delayCountdown != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Chuẩn bị: $delayCountdown giây",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Recording Countdown Display
            if (countdown != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Đang ghi: $countdown giây",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Delay Before Recording Input
            OutlinedTextField(
                value = delayBeforeRecordingInput,
                onValueChange = { 
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        delayBeforeRecordingInput = it
                        val delay = it.toIntOrNull() ?: 0
                        if (delay >= 0) {
                            viewModel.setDelayBeforeRecording(delay)
                        }
                    }
                },
                label = { Text("Delay trước khi ghi (giây)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecording && delayCountdown == null
            )
            
            // Beep Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = beepEnabled,
                    onCheckedChange = { viewModel.setBeepEnabled(it) },
                    enabled = !isRecording && delayCountdown == null
                )
                Text(
                    text = "Bật tiếng beep sau delay",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Recording Time Input
            OutlinedTextField(
                value = recordingTimeInput,
                onValueChange = { 
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        recordingTimeInput = it
                    }
                },
                label = { Text("Thời gian ghi (giây)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecording && delayCountdown == null
            )
            
            // Sampling Interval Selection
            var samplingExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = samplingExpanded,
                onExpandedChange = { samplingExpanded = !samplingExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (samplingInterval == 0L) "Liên tục" else "${samplingInterval}ms",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Khoảng thời gian lấy mẫu") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = samplingExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    enabled = !isRecording
                )
                ExposedDropdownMenu(
                    expanded = samplingExpanded,
                    onDismissRequest = { samplingExpanded = false }
                ) {
                    samplingIntervals.forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(if (interval == 0L) "Liên tục" else "${interval}ms") },
                            onClick = {
                                viewModel.setSamplingInterval(interval)
                                samplingExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Custom Sampling Interval Input (optional)
            OutlinedTextField(
                value = samplingIntervalInput,
                onValueChange = { 
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        samplingIntervalInput = it
                        val interval = it.toLongOrNull() ?: 0L
                        if (interval >= 0) {
                            viewModel.setSamplingInterval(interval)
                        }
                    }
                },
                label = { Text("Tùy chỉnh (ms, 0 = liên tục)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecording,
                placeholder = { Text("Nhập số ms hoặc 0 cho liên tục") }
            )
            
            // Type Selection
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Loại") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    types.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                viewModel.setSelectedType(type)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Divider()
            
            // Sensor Toggles
            Text(
                "Bật/Tắt cảm biến",
                style = MaterialTheme.typography.titleMedium
            )
            
            SensorToggle(
                label = "Accelerometer",
                enabled = accelerometerEnabled,
                available = viewModel.isAccelerometerAvailable,
                onCheckedChange = { viewModel.setAccelerometerEnabled(it) }
            )
            
            SensorToggle(
                label = "Gyroscope",
                enabled = gyroscopeEnabled,
                available = viewModel.isGyroscopeAvailable,
                onCheckedChange = { viewModel.setGyroscopeEnabled(it) }
            )
            
            SensorToggle(
                label = "Ambient Light",
                enabled = ambientLightEnabled,
                available = viewModel.isAmbientLightAvailable,
                onCheckedChange = { viewModel.setAmbientLightEnabled(it) }
            )
            
            SensorToggle(
                label = "Proximity",
                enabled = proximityEnabled,
                available = viewModel.isProximityAvailable,
                onCheckedChange = { viewModel.setProximityEnabled(it) }
            )
            
            Divider()
            
            // Sensor Values Display
            Text(
                "Giá trị cảm biến",
                style = MaterialTheme.typography.titleMedium
            )
            
            val accelText = accelerometerValue?.let { 
                "X: ${String.format("%.2f", it.first)}, Y: ${String.format("%.2f", it.second)}, Z: ${String.format("%.2f", it.third)}"
            }
            SensorDisplay(
                label = "Accelerometer",
                value = accelText
            )
            
            val gyroText = gyroscopeValue?.let {
                "X: ${String.format("%.2f", it.first)}, Y: ${String.format("%.2f", it.second)}, Z: ${String.format("%.2f", it.third)}"
            }
            SensorDisplay(
                label = "Gyroscope",
                value = gyroText
            )
            
            val lightText = ambientLightValue?.let { 
                String.format("%.2f", it)
            }
            SensorDisplay(
                label = "Ambient Light",
                value = lightText
            )
            
            val proxText = proximityValue?.let {
                String.format("%.2f", it)
            }
            SensorDisplay(
                label = "Proximity",
                value = proxText
            )
            
            Divider()
            
            // Save, Delete, and View JSON Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Lưu")
                }
                
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Xóa")
                }
            }
            
            // View Last Recording JSON Button
            Button(
                onClick = { showJsonDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = lastRecording != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    if (lastRecording != null) {
                        "Xem JSON (${lastRecording!!.sensors.size} phần tử)"
                    } else {
                        "Xem JSON (chưa có dữ liệu)"
                    }
                )
            }
        }
    }
    
    // Save Dialog
    if (showSaveDialog) {
        SaveDialog(
            types = viewModel.getTypeDirectories(),
            onDismiss = { showSaveDialog = false },
            onSave = { showSaveDialog = false }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa tất cả dữ liệu đã lưu?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllRecordings()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Có")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Không")
                }
            }
        )
    }
    
    // JSON View Dialog
    if (showJsonDialog && lastRecording != null) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonString = gson.toJson(lastRecording)
        
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = {
                Column {
                    Text("JSON vừa ghi")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Số phần tử: ${lastRecording!!.sensors.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = jsonString,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJsonDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}


