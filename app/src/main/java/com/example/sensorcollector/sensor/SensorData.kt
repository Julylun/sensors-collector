package com.example.sensorcollector.sensor

data class AccelerometerValue(
    val x: Float,
    val y: Float,
    val z: Float
)

data class GyroscopeValue(
    val x: Float,
    val y: Float,
    val z: Float
)

data class SensorReading(
    val timestamp: Long,
    val accelerometer: AccelerometerValue? = null,
    val gyroscope: GyroscopeValue? = null,
    val ambientLight: Float? = null,
    val proximity: Float? = null
)

data class SensorRecording(
    val type: String,
    val duration: Int,
    val sensors: List<SensorReading>
)
