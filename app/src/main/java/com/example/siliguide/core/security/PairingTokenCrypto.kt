package com.example.siliguide.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts the ESP32 pairing token for secure local storage.
 *
 * The pairing token is itself the AES-256 key used for ESP32 communication,
 * so it must not be stored in plaintext in SQLDelight.
 *
 * Local storage format:
 *
 *     Base64( IV[12 bytes] + ciphertext + GCM authentication tag[16 bytes] )
 *
 * The encryption key used to protect the pairing token is generated and
 * stored inside the Android Keystore. The key itself is never stored in
 * SQLDelight and never leaves the Keystore as raw key material.
 *
 * This key is separate from the ESP32 pairing token.
 */
@Singleton
class PairingTokenCrypto @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "SiliGuidePairingTokenKey"

        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BYTES = 16
    }

    /**
     * Encrypts a raw ESP32 pairing token for storage in SQLDelight.
     *
     * A new random IV is generated for every encryption operation.
     *
     * @param pairingToken The raw 64-character hexadecimal ESP32 pairing token.
     * @return Base64 encoded IV + ciphertext + authentication tag.
     */
    fun encrypt(pairingToken: String): String {
        validatePairingToken(pairingToken)

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        // Android Keystore/GCM generates a fresh random IV during init().
        val iv = cipher.iv

        require(iv.size == GCM_IV_LENGTH_BYTES) {
            "Unexpected GCM IV length: ${iv.size} bytes"
        }

        val ciphertextWithTag = cipher.doFinal(
            pairingToken.toByteArray(Charsets.UTF_8)
        )

        // Java/Android GCM appends the authentication tag to the ciphertext.
        val encryptedData = iv + ciphertextWithTag

        return Base64.encodeToString(
            encryptedData,
            Base64.NO_WRAP
        )
    }

    /**
     * Decrypts a pairing token retrieved from SQLDelight.
     *
     * Expected input format:
     *
     *     Base64( IV + ciphertext + authentication tag )
     *
     * @param encryptedPairingToken The Base64 value stored in SQLDelight.
     * @return The original raw ESP32 pairing token.
     *
     * @throws IllegalArgumentException if the stored value is malformed.
     * @throws javax.crypto.AEADBadTagException if the ciphertext has been
     *         modified or cannot be authenticated.
     */
    fun decrypt(encryptedPairingToken: String): String {
        require(encryptedPairingToken.isNotBlank()) {
            "Encrypted pairing token cannot be blank"
        }

        val encryptedData = try {
            Base64.decode(
                encryptedPairingToken,
                Base64.NO_WRAP
            )
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid Base64 encrypted pairing token",
                e
            )
        }

        val minimumLength =
            GCM_IV_LENGTH_BYTES + GCM_TAG_LENGTH_BYTES

        require(encryptedData.size > minimumLength) {
            "Encrypted pairing token is too short"
        }

        val iv = encryptedData.copyOfRange(
            0,
            GCM_IV_LENGTH_BYTES
        )

        val ciphertextWithTag = encryptedData.copyOfRange(
            GCM_IV_LENGTH_BYTES,
            encryptedData.size
        )

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(
                GCM_TAG_LENGTH_BITS,
                iv
            )
        )

        val plaintext = cipher.doFinal(ciphertextWithTag)

        val pairingToken = String(
            plaintext,
            Charsets.UTF_8
        )

        validatePairingToken(pairingToken)

        return pairingToken
    }

    /**
     * Gets the existing Android Keystore key or creates it if this is the
     * first time the app needs to protect a pairing token.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)

        keyStore.load(null)

        val existingKey = keyStore.getKey(
            KEY_ALIAS,
            null
        )

        if (existingKey != null) {
            require(existingKey is SecretKey) {
                "Stored Keystore key is not an AES SecretKey"
            }

            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE_BITS)
            .setBlockModes(
                KeyProperties.BLOCK_MODE_GCM
            )
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    /**
     * Your ESP32 pairing token is a 64-character hexadecimal string,
     * representing 32 bytes / 256 bits.
     */
    private fun validatePairingToken(pairingToken: String) {
        require(pairingToken.length == 64) {
            "Pairing token must be exactly 64 hexadecimal characters"
        }

        require(pairingToken.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "Pairing token must contain only hexadecimal characters"
        }
    }
}