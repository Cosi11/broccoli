package com.roulette.tracker.di

import com.roulette.tracker.ui.tracking.FrameProvider
import com.roulette.tracker.ui.tracking.CameraFrameProvider
import com.roulette.tracker.ui.tracking.TestFrameProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import dagger.Provides

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TestProvider

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CameraProviderImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class FrameModule {
    
    @Binds
    @Singleton
    @CameraProviderImpl
    abstract fun bindCameraFrameProvider(
        cameraFrameProvider: CameraFrameProvider
    ): FrameProvider

    @Binds
    @Singleton
    @TestProvider
    abstract fun bindTestFrameProvider(
        testFrameProvider: TestFrameProvider
    ): FrameProvider

    companion object {
        @Provides
        @Singleton
        fun provideDefaultFrameProvider(
            @CameraProviderImpl cameraProvider: FrameProvider
        ): FrameProvider = cameraProvider
    }
} 