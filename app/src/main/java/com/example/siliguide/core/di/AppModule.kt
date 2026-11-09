package com.example.siliguide.core.di

import com.example.siliguide.data.remote.iot.Esp32Api
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.siliguide.data.local.SiliGuideDatabase
import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.data.remote.iot.WifiProvisioningManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager as AndroidBluetoothManager

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Bluetooth
    @Provides
    @Singleton
    fun provideAndroidBluetoothManager(
        @ApplicationContext context: Context
    ): AndroidBluetoothManager {
        return context.getSystemService(AndroidBluetoothManager::class.java)
    }

    @Provides
    @Singleton
    fun provideBluetoothAdapter(
        bluetoothManager: AndroidBluetoothManager
    ): BluetoothAdapter {

        return bluetoothManager.adapter
    }

    @Provides
    fun provideWifiProvisioningManager(): WifiProvisioningManager {
        return WifiProvisioningManager()
    }

    @Provides
    @Singleton
    fun provideSqlDriver(@ApplicationContext context: Context): SqlDriver {
        return AndroidSqliteDriver(
            schema = SiliGuideDatabase.Schema,
            context = context,
            name = "siliguide.db",
            callback = object : AndroidSqliteDriver.Callback(SiliGuideDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    // CRITICAL: Tells the SQLite instance to actively monitor foreign key relationships
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }
}