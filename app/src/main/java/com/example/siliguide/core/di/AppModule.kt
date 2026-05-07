package com.example.siliguide.core.di

import com.example.siliguide.data.remote.iot.Esp32Api
import com.example.siliguide.data.repository.Esp32Repository
import com.example.siliguide.domain.repository.IoTRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.example.siliguide.data.local.sqldelight.DatabaseDriverFactory
import com.example.siliguide.data.remote.iot.WifiProvisioningManager
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEsp32Api(): Esp32Api {
        return Esp32Api()
    }

    @Provides
    fun provideWifiProvisioningManager(): WifiProvisioningManager {
        return WifiProvisioningManager()
    }

    // Tell Hilt: "When a UseCase asks for an IoTRepository, give it an Esp32Repository"
    @Provides
    fun provideIoTRepository(
        api: Esp32Api,
        wifiProvisioningManager: WifiProvisioningManager,
        @ApplicationContext context: Context
    ): IoTRepository {
        return Esp32Repository(
            api,
            wifiProvisioningManager,
            context
        )
    }

    @Provides
    @Singleton
    fun provideDatabaseDriver(@ApplicationContext context: Context): SqlDriver {
        return DatabaseDriverFactory(context).createDriver()
    }
}