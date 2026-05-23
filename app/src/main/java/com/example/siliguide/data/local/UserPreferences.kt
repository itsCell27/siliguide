package com.example.siliguide.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.siliguide.domain.model.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Creates the DataStore instance (only once)
val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val userId = stringPreferencesKey("user_id")
    private val isGuest = booleanPreferencesKey("is_guest")

    // Now this returns a Flow<UserSession?>
    val sessionFlow: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val userId = prefs[userId]
        val isGuest = prefs[isGuest] ?: false
        if (userId != null) UserSession(userId, isGuest) else null
    }

    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[userId] = session.userId
            prefs[isGuest] = session.isGuest
        }
    }
}