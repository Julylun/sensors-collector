package com.example.sensorcollector.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorManager(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    private val handler = Handler(Looper.getMainLooper())
    private var intervalRunnable: Runnable? = null
    
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val ambientLightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    
    private var accelerometerEnabled = false
    private var gyroscopeEnabled = false
    private var ambientLightEnabled = false
    private var proximityEnabled = false
    
    // List of sensor readings - mỗi entry chứa tất cả sensors tại một thời điểm
    private val sensorReadings = mutableListOf<SensorReading>()
    private var samplingIntervalMs: Long = 0 // 0 = continuous (merge vào entry cuối cùng), >0 = tạo entry mới mỗi X ms
    private var lastSamplingTimestamp: Long = 0
    private var isRecording = false // Flag để biết đang recording hay không
    
    // Lưu giá trị cuối cùng của mỗi sensor để dùng khi tạo entry mới
    private var lastAccelerometerValue: AccelerometerValue? = null
    private var lastGyroscopeValue: GyroscopeValue? = null
    private var lastAmbientLightValue: Float? = null
    private var lastProximityValue: Float? = null
    
    private val _accelerometerValue = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    val accelerometerValue: StateFlow<Triple<Float, Float, Float>?> = _accelerometerValue.asStateFlow()
    
    private val _gyroscopeValue = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    val gyroscopeValue: StateFlow<Triple<Float, Float, Float>?> = _gyroscopeValue.asStateFlow()
    
    private val _ambientLightValue = MutableStateFlow<Float?>(null)
    val ambientLightValue: StateFlow<Float?> = _ambientLightValue.asStateFlow()
    
    private val _proximityValue = MutableStateFlow<Float?>(null)
    val proximityValue: StateFlow<Float?> = _proximityValue.asStateFlow()
    
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val timestamp = System.currentTimeMillis()
            
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    if (accelerometerEnabled) {
                        val value = AccelerometerValue(event.values[0], event.values[1], event.values[2])
                        // LUÔN LUÔN lưu last value, kể cả khi không recording
                        lastAccelerometerValue = value
                        // Chỉ update UI khi không recording để giảm overhead
                        if (!isRecording) {
                            _accelerometerValue.value = Triple(event.values[0], event.values[1], event.values[2])
                        }
                        // Chỉ update reading khi đang recording (tối ưu: không update StateFlow)
                        if (isRecording && sensorReadings.isNotEmpty()) {
                            val lastIndex = sensorReadings.size - 1
                            val existing = sensorReadings[lastIndex]
                            sensorReadings[lastIndex] = existing.copy(accelerometer = value)
                        }
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    if (gyroscopeEnabled) {
                        val value = GyroscopeValue(event.values[0], event.values[1], event.values[2])
                        // LUÔN LUÔN lưu last value, kể cả khi không recording
                        lastGyroscopeValue = value
                        // Chỉ update UI khi không recording để giảm overhead
                        if (!isRecording) {
                            _gyroscopeValue.value = Triple(event.values[0], event.values[1], event.values[2])
                        }
                        // Chỉ update reading khi đang recording (tối ưu: không update StateFlow)
                        if (isRecording && sensorReadings.isNotEmpty()) {
                            val lastIndex = sensorReadings.size - 1
                            val existing = sensorReadings[lastIndex]
                            sensorReadings[lastIndex] = existing.copy(gyroscope = value)
                        }
                    }
                }
                Sensor.TYPE_LIGHT -> {
                    if (ambientLightEnabled) {
                        // LUÔN LUÔN lưu last value, kể cả khi không recording
                        lastAmbientLightValue = event.values[0]
                        // Chỉ update UI khi không recording để giảm overhead
                        if (!isRecording) {
                            _ambientLightValue.value = event.values[0]
                        }
                        // Chỉ update reading khi đang recording (tối ưu: không update StateFlow)
                        if (isRecording && sensorReadings.isNotEmpty()) {
                            val lastIndex = sensorReadings.size - 1
                            val existing = sensorReadings[lastIndex]
                            sensorReadings[lastIndex] = existing.copy(ambientLight = event.values[0])
                        }
                    }
                }
                Sensor.TYPE_PROXIMITY -> {
                    if (proximityEnabled) {
                        // LUÔN LUÔN lưu last value, kể cả khi không recording
                        lastProximityValue = event.values[0]
                        // Chỉ update UI khi không recording để giảm overhead
                        if (!isRecording) {
                            _proximityValue.value = event.values[0]
                        }
                        // Chỉ update reading khi đang recording (tối ưu: không update StateFlow)
                        if (isRecording && sensorReadings.isNotEmpty()) {
                            val lastIndex = sensorReadings.size - 1
                            val existing = sensorReadings[lastIndex]
                            sensorReadings[lastIndex] = existing.copy(proximity = event.values[0])
                        }
                    }
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }
    
    fun setSamplingInterval(intervalMs: Long) {
        samplingIntervalMs = intervalMs
    }
    
    private fun updateOrCreateReading(timestamp: Long, update: (SensorReading) -> SensorReading) {
        if (samplingIntervalMs == 0L) {
            // Continuous mode: luôn merge vào entry cuối cùng
            // Nếu chưa có entry nào, tạo entry mới
            if (sensorReadings.isEmpty()) {
                val newReading = createReadingWithAllSensors(timestamp, update)
                sensorReadings.add(newReading)
                lastSamplingTimestamp = timestamp
            } else {
                // Merge vào entry cuối cùng
                val lastIndex = sensorReadings.size - 1
                val existing = sensorReadings[lastIndex]
                sensorReadings[lastIndex] = update(existing)
            }
        } else {
            // Interval mode: luôn merge vào entry cuối cùng (entry mới được tạo bởi timer)
            if (sensorReadings.isNotEmpty()) {
                val lastIndex = sensorReadings.size - 1
                val existing = sensorReadings[lastIndex]
                sensorReadings[lastIndex] = update(existing)
            } else {
                // Fallback: nếu chưa có entry nào, tạo entry mới
                val newReading = createReadingWithAllSensors(timestamp, update)
                sensorReadings.add(newReading)
                lastSamplingTimestamp = timestamp
            }
        }
    }
    
    private fun createReadingWithAllSensors(timestamp: Long, update: (SensorReading) -> SensorReading): SensorReading {
        // Tạo reading với tất cả sensors, dùng giá trị cuối cùng nếu chưa có data mới
        val baseReading = SensorReading(
            timestamp = timestamp,
            accelerometer = if (isAccelerometerAvailable() && accelerometerEnabled) lastAccelerometerValue else null,
            gyroscope = if (isGyroscopeAvailable() && gyroscopeEnabled) lastGyroscopeValue else null,
            ambientLight = if (isAmbientLightAvailable() && ambientLightEnabled) lastAmbientLightValue else null,
            proximity = if (isProximityAvailable() && proximityEnabled) lastProximityValue else null
        )
        return update(baseReading)
    }
    
    fun isAccelerometerAvailable(): Boolean = accelerometerSensor != null
    fun isGyroscopeAvailable(): Boolean = gyroscopeSensor != null
    fun isAmbientLightAvailable(): Boolean = ambientLightSensor != null
    fun isProximityAvailable(): Boolean = proximitySensor != null
    
    fun setAccelerometerEnabled(enabled: Boolean) {
        accelerometerEnabled = enabled && isAccelerometerAvailable()
        if (accelerometerSensor != null) {
            if (accelerometerEnabled) {
                sensorManager.registerListener(
                    sensorEventListener,
                    accelerometerSensor,
                    AndroidSensorManager.SENSOR_DELAY_NORMAL
                )
            } else {
                sensorManager.unregisterListener(sensorEventListener, accelerometerSensor)
                _accelerometerValue.value = null
                lastAccelerometerValue = null
            }
        }
    }
    
    fun setGyroscopeEnabled(enabled: Boolean) {
        gyroscopeEnabled = enabled && isGyroscopeAvailable()
        if (gyroscopeSensor != null) {
            if (gyroscopeEnabled) {
                sensorManager.registerListener(
                    sensorEventListener,
                    gyroscopeSensor,
                    AndroidSensorManager.SENSOR_DELAY_NORMAL
                )
            } else {
                sensorManager.unregisterListener(sensorEventListener, gyroscopeSensor)
                _gyroscopeValue.value = null
                lastGyroscopeValue = null
            }
        }
    }
    
    fun setAmbientLightEnabled(enabled: Boolean) {
        ambientLightEnabled = enabled && isAmbientLightAvailable()
        if (ambientLightSensor != null) {
            if (ambientLightEnabled) {
                sensorManager.registerListener(
                    sensorEventListener,
                    ambientLightSensor,
                    AndroidSensorManager.SENSOR_DELAY_NORMAL
                )
            } else {
                sensorManager.unregisterListener(sensorEventListener, ambientLightSensor)
                _ambientLightValue.value = null
                lastAmbientLightValue = null
            }
        }
    }
    
    fun setProximityEnabled(enabled: Boolean) {
        proximityEnabled = enabled && isProximityAvailable()
        if (proximitySensor != null) {
            if (proximityEnabled) {
                sensorManager.registerListener(
                    sensorEventListener,
                    proximitySensor,
                    AndroidSensorManager.SENSOR_DELAY_NORMAL
                )
            } else {
                sensorManager.unregisterListener(sensorEventListener, proximitySensor)
                _proximityValue.value = null
                lastProximityValue = null
            }
        }
    }
    
    fun startRecording() {
        isRecording = true
        sensorReadings.clear()
        lastSamplingTimestamp = 0
        // KHÔNG reset last values - giữ lại giá trị đã lưu từ trước khi ghi
        // Last values đã được lưu liên tục khi sensors được bật, không chỉ khi recording
        
        if (samplingIntervalMs == 0L) {
            // Continuous mode: tạo entry đầu tiên ngay lập tức với tất cả last values hiện có
            val initialTimestamp = System.currentTimeMillis()
            val initialReading = SensorReading(
                timestamp = initialTimestamp,
                accelerometer = if (isAccelerometerAvailable() && accelerometerEnabled) lastAccelerometerValue else null,
                gyroscope = if (isGyroscopeAvailable() && gyroscopeEnabled) lastGyroscopeValue else null,
                ambientLight = if (isAmbientLightAvailable() && ambientLightEnabled) lastAmbientLightValue else null,
                proximity = if (isProximityAvailable() && proximityEnabled) lastProximityValue else null
            )
            sensorReadings.add(initialReading)
            lastSamplingTimestamp = initialTimestamp
        } else {
            // Interval mode: tạo entry đầu tiên ngay lập tức và bắt đầu timer để tạo entry định kỳ
            val initialTimestamp = System.currentTimeMillis()
            val initialReading = SensorReading(
                timestamp = initialTimestamp,
                accelerometer = if (isAccelerometerAvailable() && accelerometerEnabled) lastAccelerometerValue else null,
                gyroscope = if (isGyroscopeAvailable() && gyroscopeEnabled) lastGyroscopeValue else null,
                ambientLight = if (isAmbientLightAvailable() && ambientLightEnabled) lastAmbientLightValue else null,
                proximity = if (isProximityAvailable() && proximityEnabled) lastProximityValue else null
            )
            sensorReadings.add(initialReading)
            lastSamplingTimestamp = initialTimestamp
            
            // Bắt đầu timer để tạo entry mới mỗi X ms (dùng Handler cho chính xác hơn)
            intervalRunnable = object : Runnable {
                override fun run() {
                    if (isRecording) {
                        val timestamp = System.currentTimeMillis()
                        createNewReadingEntry(timestamp)
                        handler.postDelayed(this, samplingIntervalMs)
                    }
                }
            }
            handler.postDelayed(intervalRunnable!!, samplingIntervalMs)
        }
    }
    
    private fun createNewReadingEntry(timestamp: Long) {
        // Tạo entry mới với tất cả last values hiện có (tối ưu: tạo trực tiếp, không qua function)
        sensorReadings.add(
            SensorReading(
                timestamp = timestamp,
                accelerometer = if (isAccelerometerAvailable() && accelerometerEnabled) lastAccelerometerValue else null,
                gyroscope = if (isGyroscopeAvailable() && gyroscopeEnabled) lastGyroscopeValue else null,
                ambientLight = if (isAmbientLightAvailable() && ambientLightEnabled) lastAmbientLightValue else null,
                proximity = if (isProximityAvailable() && proximityEnabled) lastProximityValue else null
            )
        )
        lastSamplingTimestamp = timestamp
    }
    
    fun stopRecording() {
        isRecording = false
        intervalRunnable?.let { handler.removeCallbacks(it) }
        intervalRunnable = null
        
        // Update UI với giá trị cuối cùng sau khi dừng recording
        lastAccelerometerValue?.let {
            _accelerometerValue.value = Triple(it.x, it.y, it.z)
        }
        lastGyroscopeValue?.let {
            _gyroscopeValue.value = Triple(it.x, it.y, it.z)
        }
        lastAmbientLightValue?.let {
            _ambientLightValue.value = it
        }
        lastProximityValue?.let {
            _proximityValue.value = it
        }
        
        // KHÔNG unregister listener vì sensors vẫn cần hoạt động để lưu last values
        // Chỉ clear sensorReadings khi bắt đầu recording mới
    }
    
    fun getRecordingData(type: String, duration: Int): SensorRecording {
        // Sort by timestamp và đảm bảo mỗi entry có đầy đủ tất cả sensors
        // Nếu sensor chưa có data, dùng giá trị từ entry trước đó
        val readings = mutableListOf<SensorReading>()
        var prevAccelerometer: AccelerometerValue? = null
        var prevGyroscope: GyroscopeValue? = null
        var prevAmbientLight: Float? = null
        var prevProximity: Float? = null
        
        sensorReadings.sortedBy { it.timestamp }.forEach { reading ->
            // Đảm bảo mỗi entry có đầy đủ sensors, dùng giá trị trước đó nếu chưa có
            val completeReading = SensorReading(
                timestamp = reading.timestamp,
                accelerometer = if (isAccelerometerAvailable() && accelerometerEnabled) {
                    reading.accelerometer ?: prevAccelerometer
                } else null,
                gyroscope = if (isGyroscopeAvailable() && gyroscopeEnabled) {
                    reading.gyroscope ?: prevGyroscope
                } else null,
                ambientLight = if (isAmbientLightAvailable() && ambientLightEnabled) {
                    reading.ambientLight ?: prevAmbientLight
                } else null,
                proximity = if (isProximityAvailable() && proximityEnabled) {
                    reading.proximity ?: prevProximity
                } else null
            )
            
            // Cập nhật giá trị trước đó nếu có data mới
            if (reading.accelerometer != null) prevAccelerometer = reading.accelerometer
            if (reading.gyroscope != null) prevGyroscope = reading.gyroscope
            if (reading.ambientLight != null) prevAmbientLight = reading.ambientLight
            if (reading.proximity != null) prevProximity = reading.proximity
            
            readings.add(completeReading)
        }
        
        return SensorRecording(
            type = type,
            duration = duration,
            sensors = readings
        )
    }
    
    fun clearData() {
        sensorReadings.clear()
    }
}
