package com.roulette.tracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RouletteApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)  // Optional für besseres Debugging
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // WorkManager.initialize(this, workManagerConfiguration)  // Nicht mehr nötig, wird automatisch gemacht
        // Initialisierung hier...
    }
} 