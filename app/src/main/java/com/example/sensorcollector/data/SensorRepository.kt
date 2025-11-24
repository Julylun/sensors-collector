package com.example.sensorcollector.data

import android.content.Context
import com.example.sensorcollector.sensor.SensorRecording
import java.io.File

class SensorRepository(private val context: Context) {
    private val fileManager = FileManager(context)
    
    /**
     * Lưu recording vào file.
     * Đây là suspend function để có thể chạy trong background thread.
     * Tất cả dữ liệu đã được thu thập và lưu trong memory trước đó.
     */
    suspend fun saveRecording(recording: SensorRecording): File? {
        return fileManager.saveRecording(recording)
    }
    
    fun getTypeDirectories(): Map<String, File> {
        return fileManager.getTypeDirectories()
    }
    
    fun getRecordingFiles(): List<File> {
        return fileManager.getRecordingFiles()
    }
    
    fun deleteFiles(paths: List<String>): Boolean {
        return fileManager.deleteFiles(paths)
    }
    
    fun deleteAllRecordings(): Boolean {
        return fileManager.deleteAllRecordings()
    }
}


