package com.example.sensorcollector.data

import android.content.Context
import com.example.sensorcollector.sensor.SensorRecording
import com.example.sensorcollector.utils.DateTimeFormatter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class FileManager(private val context: Context) {
    // Tạo Gson instance một lần để tái sử dụng (tránh tạo mới mỗi lần)
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    private val baseDir: File = File(context.getExternalFilesDir(null), "sensor_data")
    
    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }
    
    /**
     * Lưu recording vào file JSON.
     * Chỉ được gọi một lần duy nhất sau khi recording dừng.
     * Tất cả dữ liệu đã được thu thập và lưu trong memory trước đó.
     */
    suspend fun saveRecording(recording: SensorRecording): File? {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            val typeDir = File(baseDir, recording.type)
            if (!typeDir.exists()) {
                typeDir.mkdirs()
            }
            
            val fileName = DateTimeFormatter.generateFileName("sensors", typeDir)
            val file = File(typeDir, fileName)
            
            try {
                // Serialize JSON và ghi file trong background thread
                FileWriter(file).use { writer ->
                    gson.toJson(recording, writer)
                }
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    fun getTypeDirectories(): Map<String, File> {
        val types = mutableMapOf<String, File>()
        if (baseDir.exists() && baseDir.isDirectory) {
            baseDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.listFiles()?.any { it.name.endsWith(".json") } == true) {
                    types[file.name] = file
                }
            }
        }
        return types
    }
    
    fun getRecordingFiles(): List<File> {
        if (!baseDir.exists() || !baseDir.isDirectory) return emptyList()
        val files = mutableListOf<File>()
        baseDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                dir.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }?.let { files.addAll(it) }
            }
        }
        return files.sortedByDescending { it.lastModified() }
    }
    
    fun deleteFiles(paths: List<String>): Boolean {
        if (paths.isEmpty()) return true
        var success = true
        val baseCanonical = try {
            baseDir.canonicalPath
        } catch (e: Exception) {
            baseDir.absolutePath
        }
        
        paths.forEach { path ->
            try {
                val file = File(path)
                if (!file.exists()) return@forEach
                val canonical = try {
                    file.canonicalPath
                } catch (e: Exception) {
                    success = false
                    return@forEach
                }
                if (!canonical.startsWith(baseCanonical)) {
                    return@forEach
                }
                if (!file.delete()) {
                    success = false
                }
                val parent = file.parentFile
                if (parent != null && parent.isDirectory) {
                    val remaining = parent.listFiles()
                    if (remaining == null || remaining.isEmpty()) {
                        parent.delete()
                    }
                }
            } catch (e: Exception) {
                success = false
                e.printStackTrace()
            }
        }
        return success
    }
    
    fun deleteAllRecordings(): Boolean {
        return try {
            if (baseDir.exists()) {
                baseDir.deleteRecursively()
                baseDir.mkdirs()
                true
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}


