package com.example.siliguide.core.di

import com.example.siliguide.data.remote.iot.Esp32Api
import com.example.siliguide.data.remote.iot.Esp32Repository
import com.example.siliguide.domain.repository.IoTRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEsp32Api(): Esp32Api {
        return Esp32Api()
    }

    // Tell Hilt: "When a UseCase asks for an IoTRepository, give it an Esp32Repository"
    @Provides
    @Singleton
    fun provideIoTRepository(api: Esp32Api): IoTRepository {
        return Esp32Repository(api)
    }
}