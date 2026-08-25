package com.example.siliguide.core.di

import com.example.siliguide.data.repository.AnalysisRepositoryImpl
import com.example.siliguide.data.repository.AuthRepositoryImpl
import com.example.siliguide.data.repository.IoTRepositoryImpl
import com.example.siliguide.data.repository.PlantRepositoryImpl
import com.example.siliguide.data.repository.WifiScannerImpl
import com.example.siliguide.domain.repository.AnalysisRepository
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.PlantRepository
import com.example.siliguide.domain.repository.WifiScanRepository
import com.example.siliguide.data.repository.SyncRepositoryImpl
import com.example.siliguide.domain.repository.SyncRepository
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
        ioTRepositoryImpl: IoTRepositoryImpl
    ): IoTRepository

    @Binds
    @Singleton
    abstract fun bindWifiScanRepository(
       impl: WifiScannerImpl
    ): WifiScanRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl
    ): SyncRepository
}