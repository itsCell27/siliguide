package com.example.siliguide.core.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * AES-256-GCM helper matching the ESP32 firmware's mbedtls_gcm implementation.
 *
 * Wire format (produced/consumed by the device):
 *   base64( IV[12 bytes] + ciphertext + tag[16 bytes] )
 *
 * Key: 32 raw bytes derived from the 64-hex-char pairing token stored on the device.
 * Java's Cipher AES/GCM/NoPadding already appends the tag to the ciphertext on
 * encrypt and expects it appended on decrypt, so no manual tag splicing is needed
 * once the IV is separated from the payload.
 */
class AesGcmCrypto @Inject constructor() {

    private val ivLengthBytes = 12
    private val tagLengthBytes = 16

    fun decrypt(base64Payload: String, pairingToken: String): String? {
        return try {
            val raw = Base64.decode(base64Payload, Base64.NO_WRAP) ?: return null
            if (raw.size <= ivLengthBytes + tagLengthBytes) {
                return null
            }

            val iv = raw.copyOfRange(0, ivLengthBytes)
            val cipherTextWithTag = raw.copyOfRange(ivLengthBytes, raw.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                keyFromToken(pairingToken),
                GCMParameterSpec(tagLengthBytes * 8, iv)
            )

            val plainBytes = cipher.doFinal(cipherTextWithTag)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun encrypt(plaintext: String, pairingToken: String): String {
        val iv = ByteArray(ivLengthBytes).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            keyFromToken(pairingToken),
            GCMParameterSpec(tagLengthBytes * 8, iv)
        )

        val cipherTextWithTag = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val packed = iv + cipherTextWithTag // IV + ciphertext + tag, same order device expects

        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun keyFromToken(pairingToken: String): SecretKeySpec {
        require(pairingToken.length == 64) { "Pairing token must be 64 hex chars (32 bytes)" }
        val keyBytes = ByteArray(32)
        for (i in 0 until 32) {
            val hi = hexDigit(pairingToken[i * 2])
            val lo = hexDigit(pairingToken[i * 2 + 1])
            keyBytes[i] = ((hi shl 4) or lo).toByte()
        }
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun hexDigit(c: Char): Int {
        val v = Character.digit(c, 16)
        require(v != -1) { "Invalid hex char in pairing token: $c" }
        return v
    }
}