package com.roulette.tracker.di

import com.roulette.tracker.camera.FrameAnalyzerConfig
import com.roulette.tracker.ui.tracking.FrameProviderConfig
import com.roulette.tracker.ui.tracking.CameraLifecycleManager
import com.roulette.tracker.camera.CameraErrorHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {
    
    @Provides
    @Singleton
    fun provideFrameProviderConfig(): FrameProviderConfig {
        return FrameProviderConfig(
            frameRate = 30,
            resolution = FrameProviderConfig.Resolution.HD,
            useHardwareAcceleration = true
        )
    }

    @Provides
    @Singleton
    fun provideFrameAnalyzerConfig(): FrameAnalyzerConfig {
        return FrameAnalyzerConfig(
            targetFps = 30,
            colorConversion = FrameAnalyzerConfig.ColorConversion.YUV2BGR
        )
    }

    @Provides
    @Singleton
    fun provideCameraLifecycleManager(): CameraLifecycleManager {
        return CameraLifecycleManager()
    }

    @Provides
    @Singleton
    fun provideCameraErrorHandler(): CameraErrorHandler {
        return CameraErrorHandler()
    }
} 