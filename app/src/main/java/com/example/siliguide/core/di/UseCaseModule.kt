package com.example.siliguide.core.di

import com.example.siliguide.domain.repository.AnalysisRepository
import com.example.siliguide.domain.repository.PlantRepository
import com.example.siliguide.domain.usecase.AddPlantUseCase
import com.example.siliguide.domain.usecase.GetAnalysisSessionsByPlantUseCase
import com.example.siliguide.domain.usecase.GetPlantByIdUseCase
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideAddPlantUseCase(repository: PlantRepository): AddPlantUseCase {
        return AddPlantUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetPlantsUseCase(repository: PlantRepository): GetPlantsUseCase {
        return GetPlantsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetAnalysisSessionsByPlantUseCase(
        analysisRepository: AnalysisRepository
    ): GetAnalysisSessionsByPlantUseCase = GetAnalysisSessionsByPlantUseCase(analysisRepository)

    @Provides
    @Singleton
    fun provideGetPlantByIdUseCase(
        plantRepository: PlantRepository
    ): GetPlantByIdUseCase = GetPlantByIdUseCase(plantRepository)
}