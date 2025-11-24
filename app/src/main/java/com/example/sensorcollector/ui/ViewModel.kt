package com.example.sensorcollector.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensorcollector.data.SensorRepository
import com.example.sensorcollector.sensor.SensorManager
import com.example.sensorcollector.utils.BeepHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecordingFileInfo(
    val path: String,
    val name: String,
    val type: String,
    val sizeBytes: Long,
    val lastModified: Long
)

class SensorViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    private val sensorManager = SensorManager(context)
    private val repository = SensorRepository(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("sensor_prefs", Context.MODE_PRIVATE)
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()
    
    private val _recordingTime = MutableStateFlow(prefs.getInt("recording_time", 30))
    val recordingTime: StateFlow<Int> = _recordingTime.asStateFlow()
    
    private val _delayBeforeRecording = MutableStateFlow(prefs.getInt("delay_before_recording", 3))
    val delayBeforeRecording: StateFlow<Int> = _delayBeforeRecording.asStateFlow()
    
    private val _samplingInterval = MutableStateFlow(prefs.getLong("sampling_interval", 0))
    val samplingInterval: StateFlow<Long> = _samplingInterval.asStateFlow()
    
    private val _delayCountdown = MutableStateFlow<Int?>(null)
    val delayCountdown: StateFlow<Int?> = _delayCountdown.asStateFlow()
    
    private val _beepEnabled = MutableStateFlow(prefs.getBoolean("beep_enabled", true))
    val beepEnabled: StateFlow<Boolean> = _beepEnabled.asStateFlow()
    
    private val _loopRecordingEnabled = MutableStateFlow(prefs.getBoolean("loop_enabled", false))
    val loopRecordingEnabled: StateFlow<Boolean> = _loopRecordingEnabled.asStateFlow()
    
    private val _selectedType = MutableStateFlow("walk")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()
    
    private val _accelerometerEnabled = MutableStateFlow(false)
    val accelerometerEnabled: StateFlow<Boolean> = _accelerometerEnabled.asStateFlow()
    
    private val _gyroscopeEnabled = MutableStateFlow(false)
    val gyroscopeEnabled: StateFlow<Boolean> = _gyroscopeEnabled.asStateFlow()
    
    private val _ambientLightEnabled = MutableStateFlow(false)
    val ambientLightEnabled: StateFlow<Boolean> = _ambientLightEnabled.asStateFlow()
    
    private val _proximityEnabled = MutableStateFlow(false)
    val proximityEnabled: StateFlow<Boolean> = _proximityEnabled.asStateFlow()
    
    private val _recordingFiles = MutableStateFlow<List<RecordingFileInfo>>(emptyList())
    val recordingFiles: StateFlow<List<RecordingFileInfo>> = _recordingFiles.asStateFlow()
    
    val accelerometerValue = sensorManager.accelerometerValue
    val gyroscopeValue = sensorManager.gyroscopeValue
    val ambientLightValue = sensorManager.ambientLightValue
    val proximityValue = sensorManager.proximityValue
    
    val isAccelerometerAvailable = sensorManager.isAccelerometerAvailable()
    val isGyroscopeAvailable = sensorManager.isGyroscopeAvailable()
    val isAmbientLightAvailable = sensorManager.isAmbientLightAvailable()
    val isProximityAvailable = sensorManager.isProximityAvailable()
    
    private var countdownJob: Job? = null
    
    init {
        // Set initial sensor states
        sensorManager.setAccelerometerEnabled(_accelerometerEnabled.value)
        sensorManager.setGyroscopeEnabled(_gyroscopeEnabled.value)
        sensorManager.setAmbientLightEnabled(_ambientLightEnabled.value)
        sensorManager.setProximityEnabled(_proximityEnabled.value)
        
        refreshRecordingFiles()
    }
    
    fun setRecordingTime(time: Int) {
        _recordingTime.value = time
        prefs.edit().putInt("recording_time", time).apply()
    }
    
    fun setDelayBeforeRecording(delay: Int) {
        _delayBeforeRecording.value = delay
        prefs.edit().putInt("delay_before_recording", delay).apply()
    }
    
    fun setBeepEnabled(enabled: Boolean) {
        _beepEnabled.value = enabled
        prefs.edit().putBoolean("beep_enabled", enabled).apply()
    }
    
    fun setLoopRecordingEnabled(enabled: Boolean) {
        _loopRecordingEnabled.value = enabled
        prefs.edit().putBoolean("loop_enabled", enabled).apply()
    }
    
    fun setSamplingInterval(intervalMs: Long) {
        _samplingInterval.value = intervalMs
        sensorManager.setSamplingInterval(intervalMs)
        prefs.edit().putLong("sampling_interval", intervalMs).apply()
    }
    
    fun setSelectedType(type: String) {
        _selectedType.value = type
    }
    
    fun setAccelerometerEnabled(enabled: Boolean) {
        _accelerometerEnabled.value = enabled
        sensorManager.setAccelerometerEnabled(enabled)
    }
    
    fun setGyroscopeEnabled(enabled: Boolean) {
        _gyroscopeEnabled.value = enabled
        sensorManager.setGyroscopeEnabled(enabled)
    }
    
    fun setAmbientLightEnabled(enabled: Boolean) {
        _ambientLightEnabled.value = enabled
        sensorManager.setAmbientLightEnabled(enabled)
    }
    
    fun setProximityEnabled(enabled: Boolean) {
        _proximityEnabled.value = enabled
        sensorManager.setProximityEnabled(enabled)
    }
    
    fun startRecording() {
        if (_isRecording.value) return
        
        val delaySeconds = _delayBeforeRecording.value
        
        // Bắt đầu delay countdown
        _delayCountdown.value = delaySeconds
        
        viewModelScope.launch {
            // Đếm ngược delay
            var delayRemaining = delaySeconds
            while (delayRemaining > 0) {
                delay(1000)
                delayRemaining--
                _delayCountdown.value = delayRemaining
            }
            
            // Sau khi hết delay, phát beep nếu được bật
            _delayCountdown.value = null
            if (_beepEnabled.value) {
                BeepHelper.playBeep()
            }
            
            // Bắt đầu recording
            _isRecording.value = true
            sensorManager.setSamplingInterval(_samplingInterval.value)
            sensorManager.startRecording()
            
            val duration = _recordingTime.value
            _countdown.value = duration
            
            countdownJob = viewModelScope.launch {
                var remaining = duration
                while (remaining > 0 && _isRecording.value) {
                    delay(1000)
                    remaining--
                    _countdown.value = remaining
                }
                
                if (_isRecording.value) {
                    stopRecording()
                }
            }
        }
    }
    
    private val _lastRecording = MutableStateFlow<com.example.sensorcollector.sensor.SensorRecording?>(null)
    val lastRecording: StateFlow<com.example.sensorcollector.sensor.SensorRecording?> = _lastRecording.asStateFlow()
    
    fun stopRecording(manualStop: Boolean = false) {
        if (!_isRecording.value && _delayCountdown.value == null) return
        
        val wasRecording = _isRecording.value
        val remainingCountdown = _countdown.value ?: 0
        val shouldSkipSaving = manualStop && _loopRecordingEnabled.value && wasRecording && remainingCountdown > 0
        val shouldRestartLoop = _loopRecordingEnabled.value && !manualStop
        
        _isRecording.value = false
        _delayCountdown.value = null
        countdownJob?.cancel()
        countdownJob = null
        sensorManager.stopRecording()
        
        if (wasRecording && _beepEnabled.value) {
            BeepHelper.playBeep()
        }
        
        val recording = if (wasRecording && !shouldSkipSaving) {
            val data = sensorManager.getRecordingData(_selectedType.value, _recordingTime.value)
            _lastRecording.value = data
            data
        } else {
            null
        }
        
        if (recording != null) {
            viewModelScope.launch {
                repository.saveRecording(recording)
                sensorManager.clearData()
                refreshRecordingFiles()
            }
        } else {
            sensorManager.clearData()
        }
        
        _countdown.value = null
        
        if (shouldRestartLoop && wasRecording) {
            startRecording()
        }
    }
    
    private suspend fun loadRecordingFiles(): List<RecordingFileInfo> {
        return repository.getRecordingFiles().map { file ->
            RecordingFileInfo(
                path = file.absolutePath,
                name = file.name,
                type = file.parentFile?.name ?: "unknown",
                sizeBytes = file.length(),
                lastModified = file.lastModified()
            )
        }.sortedByDescending { it.lastModified }
    }
    
    fun refreshRecordingFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = loadRecordingFiles()
            withContext(Dispatchers.Main) {
                _recordingFiles.value = files
            }
        }
    }
    
    fun deleteSelectedFiles(paths: List<String>, onResult: (Boolean) -> Unit = {}) {
        if (paths.isEmpty()) {
            onResult(true)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.deleteFiles(paths)
            val files = loadRecordingFiles()
            withContext(Dispatchers.Main) {
                _recordingFiles.value = files
                onResult(result)
            }
        }
    }
    
    fun getTypeDirectories() = repository.getTypeDirectories()
    
    fun deleteAllRecordings(): Boolean {
        val result = repository.deleteAllRecordings()
        refreshRecordingFiles()
        return result
    }
}


