package com.example.sensorcollector.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensorcollector.data.SensorRepository
import com.example.sensorcollector.sensor.SensorManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    }
    
    fun setRecordingTime(time: Int) {
        _recordingTime.value = time
        prefs.edit().putInt("recording_time", time).apply()
    }
    
    fun setDelayBeforeRecording(delay: Int) {
        _delayBeforeRecording.value = delay
        prefs.edit().putInt("delay_before_recording", delay).apply()
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
            
            // Sau khi hết delay, bắt đầu recording
            _delayCountdown.value = null
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
    
    fun stopRecording() {
        if (!_isRecording.value && _delayCountdown.value == null) return
        
        _isRecording.value = false
        _delayCountdown.value = null
        countdownJob?.cancel()
        countdownJob = null
        sensorManager.stopRecording()
        
        // Lấy dữ liệu từ memory (chỉ là list, chưa serialize JSON)
        val recording = sensorManager.getRecordingData(_selectedType.value, _recordingTime.value)
        
        // Lưu recording để hiển thị
        _lastRecording.value = recording
        
        // Ghi file trong background thread để không block UI/sensor collection
        viewModelScope.launch {
            // Serialize và ghi file chỉ một lần duy nhất sau khi recording dừng
            repository.saveRecording(recording)
            sensorManager.clearData()
        }
        
        _countdown.value = null
    }
    
    fun getTypeDirectories() = repository.getTypeDirectories()
    
    fun deleteAllRecordings(): Boolean {
        return repository.deleteAllRecordings()
    }
}


