package com.example.siliguide.core.di

import com.example.siliguide.data.repository.AnalysisRepositoryImpl
import com.example.siliguide.data.repository.AuthRepositoryImpl
import com.example.siliguide.data.repository.Esp32Repository
import com.example.siliguide.data.repository.PlantRepositoryImpl
import com.example.siliguide.domain.repository.AnalysisRepository
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.PlantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindPlantRepository(
        impl: PlantRepositoryImpl
    ): PlantRepository

    @Binds
    abstract fun bindAnalysisRepository(
        impl: AnalysisRepositoryImpl
    ): AnalysisRepository

    @Binds
    @Singleton
    abstract fun bindIoTRepository(
        esp32Repository: Esp32Repository
    ): IoTRepository
}