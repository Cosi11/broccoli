package com.roulette.tracker.di

import android.content.Context
import com.roulette.tracker.data.BallTracker
import com.roulette.tracker.data.WheelTracker
import com.roulette.tracker.camera.CameraManager
import com.roulette.tracker.camera.FrameAnalyzer
import com.roulette.tracker.camera.OpenCVLoader
import com.roulette.tracker.PredictionEngine
import com.roulette.tracker.ocr.OCRService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.work.WorkManager
import javax.inject.Provider
import androidx.hilt.work.HiltWorkerFactory
import androidx.hilt.work.WorkerFactory
import com.roulette.tracker.SettingsManager

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideBallTracker(@ApplicationContext context: Context): BallTracker {
        return BallTracker(context)
    }
    
    @Provides
    @Singleton
    fun provideWheelTracker(@ApplicationContext context: Context): WheelTracker {
        return WheelTracker(context)
    }
    
    @Provides
    @Singleton
    fun providePredictionEngine(@ApplicationContext context: Context): PredictionEngine {
        return PredictionEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideOpenCVLoader(): OpenCVLoader {
        return OpenCVLoader()
    }
    
    @Provides
    @Singleton
    fun provideCameraManager(
        @ApplicationContext context: Context,
        openCVLoader: OpenCVLoader
    ): CameraManager {
        return CameraManager(context, openCVLoader)
    }
    
    @Provides
    @Singleton
    fun provideFrameAnalyzer(): FrameAnalyzer {
        return FrameAnalyzer()
    }
    
    @Provides
    @Singleton
    fun provideOCRService(@ApplicationContext context: Context): OCRService {
        return OCRService(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideHiltWorkerFactory(
        workerFactoryProvider: Provider<HiltWorkerFactory>
    ): WorkerFactory {
        return workerFactoryProvider.get()
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context
    ): SettingsManager {
        return SettingsManager(context)
    }
} 