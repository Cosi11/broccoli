package com.roulette.tracker.di

import android.content.Context
import com.roulette.tracker.tracking.TrackingStateManager
import com.roulette.tracker.utils.ErrorHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {
    
    @Provides
    @Singleton
    fun provideTrackingStateManager(): TrackingStateManager {
        return TrackingStateManager()
    }
    
    @Provides
    @Singleton
    fun provideErrorHandler(
        @ApplicationContext context: Context
    ): ErrorHandler {
        return ErrorHandler(context)
    }
} 