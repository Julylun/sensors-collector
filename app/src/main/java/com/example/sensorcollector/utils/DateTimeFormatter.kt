package com.example.sensorcollector.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeFormatter {
    private val dateFormat = SimpleDateFormat("mm-HH_dd-MM-yyyy", Locale.getDefault())
    
    fun formatForFileName(date: Date = Date()): String {
        return dateFormat.format(date)
    }
    
    fun generateFileName(baseName: String, directory: java.io.File): String {
        val baseFileName = "sensors_${formatForFileName()}"
        var fileName = "$baseFileName.json"
        var counter = 1
        
        while (directory.resolve(fileName).exists()) {
            fileName = "${baseFileName}-${counter}.json"
            counter++
        }
        
        return fileName
    }
}

