package com.example.siliguide.data.sync

import android.content.Context
import android.util.Log
import com.example.siliguide.core.security.PairingTokenCrypto
import com.example.siliguide.data.local.sqldelight.SiliGuideDatabaseManager
import com.example.siliguide.data.remote.supabase.datasource.SupabaseAnalysisRemoteSource
import com.example.siliguide.data.remote.supabase.datasource.SupabaseIoTRemoteSource
import com.example.siliguide.data.remote.supabase.datasource.SupabasePlantRemoteSource
import com.example.siliguide.data.remote.supabase.datasource.SupabaseStorageSource
import com.example.siliguide.data.remote.supabase.datasource.SupabaseUserRemoteSource
import com.example.siliguide.data.remote.supabase.dto.toDomain
import com.example.siliguide.data.remote.supabase.dto.toDto
import com.example.siliguide.domain.repository.AnalysisRepository
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.PlantRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngine @Inject constructor(
    private val plantRepositoryProvider: dagger.Lazy<PlantRepository>,
    private val ioTRepositoryProvider: dagger.Lazy<IoTRepository>,
    private val analysisRepositoryProvider: dagger.Lazy<AnalysisRepository>,
    private val pairingTokenCryptoProvider: dagger.Lazy<PairingTokenCrypto>,
    private val storageSourceProvider: dagger.Lazy<SupabaseStorageSource>,
    private val plantRemoteSourceProvider: dagger.Lazy<SupabasePlantRemoteSource>,
    private val ioTRemoteSourceProvider: dagger.Lazy<SupabaseIoTRemoteSource>,
    private val analysisRemoteSourceProvider: dagger.Lazy<SupabaseAnalysisRemoteSource>,
    private val userRemoteSourceProvider: dagger.Lazy<SupabaseUserRemoteSource>,
    private val dbManagerProvider: dagger.Lazy<SiliGuideDatabaseManager>,
    private val supabaseClientProvider: dagger.Lazy<SupabaseClient>,
    @param:ApplicationContext private val context: Context
) {
    private val plantRepository get() = plantRepositoryProvider.get()
    private val ioTRepository get() = ioTRepositoryProvider.get()
    private val analysisRepository get() = analysisRepositoryProvider.get()
    private val pairingTokenCrypto get() = pairingTokenCryptoProvider.get()
    private val storageSource get() = storageSourceProvider.get()
    private val plantRemoteSource get() = plantRemoteSourceProvider.get()
    private val ioTRemoteSource get() = ioTRemoteSourceProvider.get()
    private val analysisRemoteSource get() = analysisRemoteSourceProvider.get()
    private val userRemoteSource get() = userRemoteSourceProvider.get()
    private val dbManager get() = dbManagerProvider.get()
    private val supabaseClient get() = supabaseClientProvider.get()

    companion object {
        private const val TAG = "SyncEngine"
    }

    suspend fun sync(userId: String): SyncResult = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext SyncResult.Error("Cannot sync with blank userId")
        }

        try {
            Log.d(TAG, "Starting bidirectional sync for user: $userId")

            // Ensure Supabase Auth session is initialized and valid before making PostgREST calls
            supabaseClient.auth.awaitInitialization()
            val currentAuthUser = supabaseClient.auth.currentUserOrNull()
            if (currentAuthUser == null) {
                val msg = "Supabase Auth session is not authenticated (currentUser is null). Aborting sync for $userId"
                Log.e(TAG, msg)
                return@withContext SyncResult.Error(msg)
            }
            if (currentAuthUser.id != userId) {
                val msg = "Supabase Auth user ID (${currentAuthUser.id}) does not match sync user ID ($userId). Aborting sync."
                Log.e(TAG, msg)
                return@withContext SyncResult.Error(msg)
            }

            // =========================================================================
            // 1. USER PROFILE SYNC (Bi-directional timestamp conflict resolution)
            // =========================================================================
            syncUser(userId)

            // =========================================================================
            // 2. PUSH PHASE (Local -> Supabase)
            // Foreign-Key Enforced Order: IoT Devices -> Plants -> Analysis Sessions
            // =========================================================================
            pushDevices(userId)
            pushPlants(userId)
            pushAnalysisSessions(userId)

            // =========================================================================
            // 3. PULL PHASE (Supabase -> Local)
            // Foreign-Key Enforced Order: IoT Devices -> Plants -> Analysis Sessions
            // =========================================================================
            pullDevices(userId)
            pullPlants(userId)
            pullAnalysisSessions(userId)

            Log.d(TAG, "Sync completed successfully for user: $userId")
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for user: $userId", e)
            SyncResult.Error(e.message ?: "Unknown sync error", e)
        }
    }

    // --- USER SYNC IMPLEMENTATION ---

    private suspend fun syncUser(userId: String) {
        val remoteUser = try {
            userRemoteSource.fetchUserById(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch remote user profile for $userId: ${e.message}")
            null
        } ?: return

        val localUser = dbManager.queries.getUserById(userId).executeAsOneOrNull()

        val remoteUpdatedAt = try {
            remoteUser.updatedAt?.let { java.time.Instant.parse(it).toEpochMilli() } ?: 0L
        } catch (e: Exception) {
            0L
        }

        if (localUser == null) {
            val remoteCreatedAt = try {
                remoteUser.createdAt?.let { java.time.Instant.parse(it).toEpochMilli() } ?: remoteUpdatedAt
            } catch (e: Exception) {
                remoteUpdatedAt
            }
            dbManager.queries.insertUser(
                user_id = remoteUser.id,
                display_name = remoteUser.displayName,
                email = remoteUser.email,
                is_guest = 0L,
                created_at = remoteCreatedAt,
                updated_at = remoteUpdatedAt
            )
            Log.d(TAG, "Inserted user profile from Supabase: display_name=${remoteUser.displayName}")
        } else if (localUser.is_guest == 0L) {
            if (localUser.display_name != remoteUser.displayName) {
                if (localUser.updated_at > remoteUpdatedAt) {
                    Log.d(TAG, "Local user is newer (${localUser.updated_at} > $remoteUpdatedAt). Pushing to Supabase: display_name=${localUser.display_name}")
                    try {
                        userRemoteSource.updateDisplayName(
                            userId = userId,
                            displayName = localUser.display_name,
                            updatedAt = java.time.Instant.ofEpochMilli(localUser.updated_at).toString()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to push updated user profile to Supabase", e)
                    }
                } else {
                    Log.d(TAG, "Remote user is newer ($remoteUpdatedAt >= ${localUser.updated_at}). Pulling from Supabase: display_name=${remoteUser.displayName}")
                    dbManager.queries.updateDisplayName(
                        display_name = remoteUser.displayName,
                        updated_at = remoteUpdatedAt,
                        user_id = userId
                    )
                }
            }
        }
    }

    // --- PUSH IMPLEMENTATIONS ---

    private suspend fun pushDevices(userId: String) {
        val unsyncedDevices = ioTRepository.getUnsyncedDevices(userId)
        if (unsyncedDevices.isEmpty()) return

        val (deletedDevices, activeDevices) = unsyncedDevices.partition { it.isDeleted }

        // 1. Delete removed devices from Supabase & SQLite
        for (device in deletedDevices) {
            try {
                ioTRemoteSource.deleteDevice(device.deviceId)
                ioTRepository.deleteDevice(device.deviceId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete device ${device.deviceId} from Supabase", e)
            }
        }

        // 2. Upsert active devices
        if (activeDevices.isNotEmpty()) {
            Log.d(TAG, "Pushing ${activeDevices.size} unsynced devices to Supabase")
            val deviceDtos = activeDevices.map { device ->
                val rawToken = try {
                    pairingTokenCrypto.decrypt(device.pairingToken)
                } catch (e: Exception) {
                    Log.w(TAG, "Pairing token was already plaintext or decryption failed for ${device.deviceId}", e)
                    device.pairingToken
                }
                device.toDto(rawPairingToken = rawToken)
            }

            ioTRemoteSource.upsertDevices(deviceDtos)

            activeDevices.forEach { device ->
                ioTRepository.markDeviceSynced(device.deviceId)
            }
        }
    }

    private suspend fun pushPlants(userId: String) {
        val unsyncedPlants = plantRepository.getUnsyncedPlants(userId)
        if (unsyncedPlants.isEmpty()) return

        val (deletedPlants, activePlants) = unsyncedPlants.partition { it.isDeleted }

        // 1. Delete removed plants from Supabase & SQLite
        for (plant in deletedPlants) {
            try {
                plantRemoteSource.deletePlant(plant.plantId)
                plantRepository.deletePlant(plant.plantId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete plant ${plant.plantId} from Supabase", e)
            }
        }

        // 2. Upsert active plants
        if (activePlants.isNotEmpty()) {
            Log.d(TAG, "Pushing ${activePlants.size} unsynced plants to Supabase")
            val plantDtos = activePlants.map { it.toDto() }

            plantRemoteSource.upsertPlants(plantDtos)

            activePlants.forEach { plant ->
                plantRepository.markPlantSynced(plant.plantId)
            }
        }
    }

    private suspend fun pushAnalysisSessions(userId: String) {
        val unsyncedSessions = analysisRepository.getUnsyncedSessions(userId)
        if (unsyncedSessions.isEmpty()) return

        val (deletedSessions, activeSessions) = unsyncedSessions.partition { it.isDeleted }

        // 1. Delete removed sessions from Supabase & SQLite
        for (session in deletedSessions) {
            try {
                analysisRemoteSource.deleteSession(session.sessionId)
                storageSource.deleteAnalysisImage(userId, session.sessionId)
                analysisRepository.deleteSession(session.sessionId)
                val localFile = File(session.imagePath)
                if (localFile.exists()) localFile.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete analysis session ${session.sessionId} from Supabase", e)
            }
        }

        // 2. Upsert active sessions
        if (activeSessions.isNotEmpty()) {
            Log.d(TAG, "Pushing ${activeSessions.size} unsynced analysis sessions to Supabase")
            val sessionDtos = activeSessions.map { session ->
                var storagePath = "$userId/${session.sessionId}.jpg"

                val localImageFile = File(session.imagePath)
                if (localImageFile.exists() && localImageFile.isFile) {
                    try {
                        val imageBytes = localImageFile.readBytes()
                        storagePath = storageSource.uploadAnalysisImage(
                            userId = userId,
                            sessionId = session.sessionId,
                            imageBytes = imageBytes
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to upload analysis image for session ${session.sessionId}", e)
                    }
                }

                session.toDto(storageImagePath = storagePath)
            }

            analysisRemoteSource.upsertSessions(sessionDtos)

            activeSessions.forEach { session ->
                analysisRepository.markSessionSynced(session.sessionId)
            }
        }
    }

    // --- PULL IMPLEMENTATIONS ---

    private suspend fun pullDevices(userId: String) {
        val remoteDeviceDtos = ioTRemoteSource.fetchDevicesByUser(userId)
        Log.d(TAG, "Pulled ${remoteDeviceDtos.size} devices from Supabase")

        for (dto in remoteDeviceDtos) {
            val localEncryptedToken = try {
                pairingTokenCrypto.encrypt(dto.pairingToken)
            } catch (e: Exception) {
                Log.w(TAG, "Pairing token was already encrypted or encryption failed for ${dto.deviceId}", e)
                dto.pairingToken
            }

            val domainDevice = dto.toDomain(localEncryptedPairingToken = localEncryptedToken)
            ioTRepository.saveDevice(domainDevice)
            // Explicitly mark synced to prevent re-pushing pulled data
            ioTRepository.markDeviceSynced(domainDevice.deviceId)
        }
    }

    private suspend fun pullPlants(userId: String) {
        val remotePlantDtos = plantRemoteSource.fetchPlantsByUser(userId)
        Log.d(TAG, "Pulled ${remotePlantDtos.size} plants from Supabase")

        for (dto in remotePlantDtos) {
            val domainPlant = dto.toDomain()
            plantRepository.insertPlant(domainPlant)
            // Explicitly mark synced to prevent re-pushing pulled data
            plantRepository.markPlantSynced(domainPlant.plantId)
        }
    }

    private suspend fun pullAnalysisSessions(userId: String) {
        val remoteSessionDtos = analysisRemoteSource.fetchSessionsByUser(userId)
        Log.d(TAG, "Pulled ${remoteSessionDtos.size} analysis sessions from Supabase")

        val directory = File(context.filesDir, "analysis_images")
        if (!directory.exists()) directory.mkdirs()

        for (dto in remoteSessionDtos) {
            val localFile = File(directory, "analysis_${dto.sessionId}.jpg")

            if (!localFile.exists() || localFile.length() == 0L) {
                try {
                    val downloadedBytes = storageSource.downloadAnalysisImage(dto.imagePath)
                    FileOutputStream(localFile).use { output ->
                        output.write(downloadedBytes)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download image for remote session ${dto.sessionId}", e)
                }
            }

            val domainSession = dto.toDomain(localDeviceImagePath = localFile.absolutePath)
            analysisRepository.saveSession(domainSession)
            // Explicitly mark synced to prevent re-pushing pulled data
            analysisRepository.markSessionSynced(domainSession.sessionId)
        }
    }
}
