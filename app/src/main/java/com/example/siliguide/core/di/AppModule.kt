package com.example.siliguide.core.di

import android.app.Application
import com.example.siliguide.data.remote.iot.Esp32Api
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.siliguide.data.local.SiliGuideDatabase
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

    @Provides
    @Singleton
    fun provideSqlDriver(app: Application): SqlDriver {
        return AndroidSqliteDriver(
            schema = SiliGuideDatabase.Schema,
            context = app,
            name = "siliguide.db"
        )
    }
}