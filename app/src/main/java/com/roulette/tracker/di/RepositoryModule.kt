package com.roulette.tracker.di

import com.roulette.tracker.data.repository.Repository
import com.roulette.tracker.data.repository.SimulationRepository
import com.roulette.tracker.data.entities.SimulationResult
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSimulationRepository(
        repository: SimulationRepository
    ): Repository<SimulationResult>
} 