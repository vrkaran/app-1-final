package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converters.HBXTypeConverters
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        LeadEntity::class,
        PaymentEntity::class,
        SiteSurveyEntity::class,
        SyncOperationEntity::class,
        PipelineStageEntity::class,
        BomItemEntity::class,
        DocumentEntity::class,
        DocumentAuditEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(HBXTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun leadDao(): LeadDao
    abstract fun paymentDao(): PaymentDao
    abstract fun siteSurveyDao(): SiteSurveyDao
    abstract fun pipelineStageDao(): PipelineStageDao
    abstract fun bomItemDao(): BomItemDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun documentDao(): DocumentDao
    abstract fun documentAuditDao(): DocumentAuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hbx_solar_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
