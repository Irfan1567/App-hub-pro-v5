package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppEntity
import com.example.data.model.ConsoleLogEntity
import com.example.data.model.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY isPinned DESC, lastUsedAt DESC")
    fun getAllAppsFlow(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE id = :appId LIMIT 1")
    suspend fun getAppById(appId: String): AppEntity?

    @Query("SELECT * FROM apps WHERE id = :appId LIMIT 1")
    fun getAppFlow(appId: String): Flow<AppEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("UPDATE apps SET name = :name WHERE id = :appId")
    suspend fun updateAppName(appId: String, name: String)

    @Query("UPDATE apps SET name = :name, description = :description WHERE id = :appId")
    suspend fun updateAppDetails(appId: String, name: String, description: String)

    @Query("UPDATE apps SET isPinned = :isPinned WHERE id = :appId")
    suspend fun updatePinStatus(appId: String, isPinned: Boolean)

    @Query("UPDATE apps SET lastUsedAt = :timestamp WHERE id = :appId")
    suspend fun updateLastUsed(appId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE apps SET isDesktopMode = :isDesktop WHERE id = :appId")
    suspend fun updateDesktopMode(appId: String, isDesktop: Boolean)

    @Query("UPDATE apps SET zoomLevelPercent = :zoom WHERE id = :appId")
    suspend fun updateZoomLevel(appId: String, zoom: Int)

    @Query("UPDATE apps SET entryPoint = :entryPoint WHERE id = :appId")
    suspend fun updateEntryPoint(appId: String, entryPoint: String)

    @Query("UPDATE apps SET storageSizeBytes = :sizeBytes WHERE id = :appId")
    suspend fun updateStorageSize(appId: String, sizeBytes: Long)

    @Delete
    suspend fun deleteApp(app: AppEntity)

    @Query("DELETE FROM apps WHERE id = :appId")
    suspend fun deleteAppById(appId: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloadsFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE appId = :appId ORDER BY timestamp DESC")
    fun getDownloadsForApp(appId: String): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("DELETE FROM downloads WHERE appId = :appId")
    suspend fun deleteDownloadsForApp(appId: String)
}

@Dao
interface ConsoleLogDao {
    @Query("SELECT * FROM console_logs WHERE appId = :appId ORDER BY id ASC")
    fun getLogsForApp(appId: String): Flow<List<ConsoleLogEntity>>

    @Query("SELECT COUNT(*) FROM console_logs WHERE appId = :appId AND severity = 'ERROR'")
    fun getErrorCountForApp(appId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConsoleLogEntity)

    @Query("DELETE FROM console_logs WHERE appId = :appId")
    suspend fun clearLogsForApp(appId: String)

    @Query("DELETE FROM console_logs")
    suspend fun clearAllLogs()
}
