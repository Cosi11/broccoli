package com.roulette.tracker.di

import android.content.Context
import androidx.room.Room
import com.roulette.tracker.data.AppDatabase
import com.roulette.tracker.data.dao.SimulationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "roulette_database"
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    @Singleton
    fun provideSimulationDao(database: AppDatabase): SimulationDao {
        return database.simulationDao()
    }
} 