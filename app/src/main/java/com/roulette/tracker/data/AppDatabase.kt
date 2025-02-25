package com.roulette.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.roulette.tracker.data.dao.SimulationDao
import com.roulette.tracker.data.entities.SimulationEntity
import com.roulette.tracker.data.entities.SimulationResult

@Database(
    entities = [
        SimulationEntity::class,
        SimulationResult::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun simulationDao(): SimulationDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roulette_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
            }
        }
    }
} 