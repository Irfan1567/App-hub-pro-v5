package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey
    val id: String, // e.g. "app_16938472918"
    val name: String,
    val description: String = "",
    val iconName: String = "code", // icon descriptor
    val entryPoint: String = "index.html", // relative entry point inside app directory
    val projectDirName: String, // sub-folder inside context.filesDir/apps/
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val isSampleApp: Boolean = false,
    val isDesktopMode: Boolean = false,
    val zoomLevelPercent: Int = 100, // 50 to 250%
    val storageSizeBytes: Long = 0L,
    val version: String = "1.0.0",
    val isPinned: Boolean = false
)

enum class RuntimeMode {
    MOBILE,
    DESKTOP
}

enum class FileCategory {
    HTML,
    CSS,
    JAVASCRIPT,
    JSON,
    IMAGE,
    FONT,
    MEDIA,
    OTHER,
    DIRECTORY
}

data class ProjectFile(
    val name: String,
    val relativePath: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: FileCategory,
    val children: List<ProjectFile> = emptyList()
)
