package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppEntity
import com.example.data.model.ConsoleLogEntity
import com.example.data.model.DownloadEntity
import com.example.data.model.LogSeverity

class Converters {
    @TypeConverter
    fun fromLogSeverity(value: LogSeverity): String = value.name

    @TypeConverter
    fun toLogSeverity(value: String): LogSeverity = try {
        LogSeverity.valueOf(value)
    } catch (e: Exception) {
        LogSeverity.INFO
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE apps ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [AppEntity::class, DownloadEntity::class, ConsoleLogEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppHubDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun downloadDao(): DownloadDao
    abstract fun consoleLogDao(): ConsoleLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppHubDatabase? = null

        fun getInstance(context: Context): AppHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppHubDatabase::class.java,
                    "app_hub_pro.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
