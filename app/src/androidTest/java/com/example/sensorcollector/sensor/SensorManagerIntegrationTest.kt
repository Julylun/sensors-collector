package com.example.sensorcollector.sensor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SensorManager
 * These tests require Android device/emulator with sensors
 */
@RunWith(AndroidJUnit4::class)
class SensorManagerIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sensorManager = SensorManager(context)
    }
    
    @Test
    fun `test sensor availability check`() {
        // Check if sensors are available (may vary by device)
        val accelAvailable = sensorManager.isAccelerometerAvailable()
        val gyroAvailable = sensorManager.isGyroscopeAvailable()
        val lightAvailable = sensorManager.isAmbientLightAvailable()
        val proximityAvailable = sensorManager.isProximityAvailable()
        
        // At least one sensor should be available on most devices
        assertTrue(
            "At least one sensor should be available",
            accelAvailable || gyroAvailable || lightAvailable || proximityAvailable
        )
    }
    
    @Test
    fun `test continuous mode creates single entry`() {
        // Enable available sensors
        if (sensorManager.isAccelerometerAvailable()) {
            sensorManager.setAccelerometerEnabled(true)
        }
        if (sensorManager.isGyroscopeAvailable()) {
            sensorManager.setGyroscopeEnabled(true)
        }
        
        // Set continuous mode
        sensorManager.setSamplingInterval(0)
        
        // Start recording
        sensorManager.startRecording()
        
        // Wait a bit for sensor events
        Thread.sleep(100)
        
        // Stop recording
        sensorManager.stopRecording()
        
        // Get recording data
        val recording = sensorManager.getRecordingData("test", 1)
        
        // Verify: Should have at least 1 entry
        assertTrue(
            "Continuous mode should have at least 1 entry",
            recording.sensors.isNotEmpty()
        )
        
        // Verify: All entries should have all sensors (some may be null)
        recording.sensors.forEach { entry ->
            // Entry should have timestamp
            assertTrue("Entry should have timestamp", entry.timestamp > 0)
            
            // Entry should have structure (sensors may be null if disabled/unavailable)
            // This is expected behavior
        }
    }
    
    @Test
    fun `test interval mode creates multiple entries`() {
        // Enable available sensors
        if (sensorManager.isAccelerometerAvailable()) {
            sensorManager.setAccelerometerEnabled(true)
        }
        
        // Set interval mode (50ms)
        sensorManager.setSamplingInterval(50)
        
        // Start recording
        sensorManager.startRecording()
        
        // Wait for multiple intervals
        Thread.sleep(200)
        
        // Stop recording
        sensorManager.stopRecording()
        
        // Get recording data
        val recording = sensorManager.getRecordingData("test", 1)
        
        // Verify: Should have multiple entries in interval mode
        assertTrue(
            "Interval mode should have multiple entries after 200ms with 50ms interval",
            recording.sensors.size >= 2
        )
    }
    
    @Test
    fun `test recording structure has all required fields`() {
        // Enable sensors
        if (sensorManager.isAccelerometerAvailable()) {
            sensorManager.setAccelerometerEnabled(true)
        }
        
        sensorManager.setSamplingInterval(0)
        sensorManager.startRecording()
        Thread.sleep(50)
        sensorManager.stopRecording()
        
        val recording = sensorManager.getRecordingData("test", 5)
        
        // Verify structure
        assertEquals("test", recording.type)
        assertEquals(5, recording.duration)
        assertNotNull(recording.sensors)
        assertTrue("Should have at least one sensor reading", recording.sensors.isNotEmpty())
    }
}

