package com.example.sensorcollector.sensor

import org.junit.Assert.*
import org.junit.Test

/**
 * Simple unit tests for SensorManager logic
 * Note: Full integration tests require Android device/emulator
 */
class SensorManagerSimpleTest {
    
    @Test
    fun `test SensorReading data class`() {
        val reading = SensorReading(
            timestamp = 1000L,
            accelerometer = AccelerometerValue(1.0f, 2.0f, 3.0f),
            gyroscope = GyroscopeValue(0.1f, 0.2f, 0.3f),
            ambientLight = 10.5f,
            proximity = 5.0f
        )
        
        assertEquals(1000L, reading.timestamp)
        assertNotNull(reading.accelerometer)
        assertNotNull(reading.gyroscope)
        assertNotNull(reading.ambientLight)
        assertNotNull(reading.proximity)
        assertEquals(1.0f, reading.accelerometer?.x ?: 0f, 0.001f)
        assertEquals(10.5f, reading.ambientLight ?: 0f, 0.001f)
    }
    
    @Test
    fun `test SensorReading with null sensors`() {
        val reading = SensorReading(
            timestamp = 1000L,
            accelerometer = null,
            gyroscope = null,
            ambientLight = null,
            proximity = null
        )
        
        assertEquals(1000L, reading.timestamp)
        assertNull(reading.accelerometer)
        assertNull(reading.gyroscope)
        assertNull(reading.ambientLight)
        assertNull(reading.proximity)
    }
    
    @Test
    fun `test SensorRecording structure`() {
        val readings = listOf(
            SensorReading(
                timestamp = 1000L,
                accelerometer = AccelerometerValue(1.0f, 2.0f, 3.0f),
                gyroscope = null,
                ambientLight = null,
                proximity = null
            ),
            SensorReading(
                timestamp = 2000L,
                accelerometer = null,
                gyroscope = GyroscopeValue(0.1f, 0.2f, 0.3f),
                ambientLight = null,
                proximity = null
            )
        )
        
        val recording = SensorRecording(
            type = "walk",
            duration = 2,
            sensors = readings
        )
        
        assertEquals("walk", recording.type)
        assertEquals(2, recording.duration)
        assertEquals(2, recording.sensors.size)
        assertEquals(1000L, recording.sensors[0].timestamp)
        assertEquals(2000L, recording.sensors[1].timestamp)
    }
    
    @Test
    fun `test AccelerometerValue structure`() {
        val accel = AccelerometerValue(1.5f, 2.5f, 3.5f)
        assertEquals(1.5f, accel.x, 0.001f)
        assertEquals(2.5f, accel.y, 0.001f)
        assertEquals(3.5f, accel.z, 0.001f)
    }
    
    @Test
    fun `test GyroscopeValue structure`() {
        val gyro = GyroscopeValue(0.15f, 0.25f, 0.35f)
        assertEquals(0.15f, gyro.x, 0.001f)
        assertEquals(0.25f, gyro.y, 0.001f)
        assertEquals(0.35f, gyro.z, 0.001f)
    }
}

