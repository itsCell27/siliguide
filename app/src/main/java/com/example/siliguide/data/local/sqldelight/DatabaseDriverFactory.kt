package com.example.siliguide.data.local.sqldelight

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.siliguide.data.local.SiliGuideDatabase

class DatabaseDriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = SiliGuideDatabase.Companion.Schema,
            context = context,
            name = "siliguide.db"
        )
    }
}