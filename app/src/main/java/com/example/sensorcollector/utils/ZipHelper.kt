package com.example.sensorcollector.utils

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipHelper {
    fun createZipFromDirectories(
        context: Context,
        directories: Map<String, File>
    ): File? {
        if (directories.isEmpty()) return null
        
        val zipFile = File(context.cacheDir, "sensor_data_${System.currentTimeMillis()}.zip")
        
        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                directories.forEach { (type, directory) ->
                    if (directory.exists() && directory.isDirectory) {
                        addDirectoryToZip(directory, type, zipOut)
                    }
                }
            }
            return zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun addDirectoryToZip(
        directory: File,
        basePath: String,
        zipOut: ZipOutputStream
    ) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".json")) {
                val entryName = "$basePath/${file.name}"
                zipOut.putNextEntry(ZipEntry(entryName))
                
                FileInputStream(file).use { fis ->
                    fis.copyTo(zipOut)
                }
                
                zipOut.closeEntry()
            }
        }
    }
}


