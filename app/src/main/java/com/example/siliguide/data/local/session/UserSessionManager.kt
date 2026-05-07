package com.example.siliguide.data.local.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_session")

class UserSessionManager(private val context: Context) {

    companion object {
        private val USER_ID = stringPreferencesKey("user_id")
    }

    val userIdFlow: Flow<String?> = context.dataStore.data.map {
        it[USER_ID]
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit {
            it[USER_ID] = userId
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.clear()
        }
    }
}