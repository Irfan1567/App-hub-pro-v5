package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String, // UUID
    val appId: String,
    val appName: String,
    val fileName: String,
    val mimeType: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED", // COMPLETED, FAILED
    val sourceUrl: String = ""
)

enum class LogSeverity {
    INFO,
    WARN,
    ERROR,
    NETWORK
}

@Entity(tableName = "console_logs")
data class ConsoleLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val appId: String,
    val severity: LogSeverity,
    val message: String,
    val sourceId: String = "",
    val lineNumber: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActiveAppSession(
    val appId: String,
    val appName: String,
    val entryUrl: String,
    val currentUrl: String,
    val title: String,
    val isDesktopMode: Boolean,
    val zoomLevelPercent: Int,
    val memoryEstimateMb: Double,
    val lastActiveTimestamp: Long,
    val errorCount: Int = 0
)
