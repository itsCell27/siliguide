package com.example.siliguide.core.di

import com.example.siliguide.data.local.AuthRepositoryImpl
import com.example.siliguide.data.repository.PlantRepositoryImpl
import com.example.siliguide.domain.repository.AuthRepository
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
    @Singleton
    abstract fun bindPlantRepository(
        plantRepositoryImpl: PlantRepositoryImpl
    ): PlantRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}