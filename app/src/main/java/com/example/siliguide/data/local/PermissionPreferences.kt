package com.example.siliguide.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.siliguide.core.permission.PermissionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionPreferences @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {

    private fun requestedKey(permission: PermissionType) =
        booleanPreferencesKey("${permission.key}_requested")

    fun wasRequested(
        permission: PermissionType
    ): Flow<Boolean> {

        return context.dataStore.data.map { prefs ->

            prefs[requestedKey(permission)] ?: false
        }
    }

    suspend fun setRequested(
        permission: PermissionType
    ) {

        context.dataStore.edit { prefs ->

            prefs[requestedKey(permission)] = true
        }
    }

    suspend fun clearRequested(
        permission: PermissionType
    ) {

        context.dataStore.edit { prefs ->

            prefs[requestedKey(permission)] = false
        }
    }
}