package com.example.vehiclesensormodule.auth

import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts text with AES-GCM, which also detects tampering.
 * Takes the key as a parameter, so this can be tested off a phone.
 */
class TokenCipher(
    private val key: () -> SecretKey
) {

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }

    fun encrypt(text: String): String {

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(Cipher.ENCRYPT_MODE, key())

        val iv = cipher.iv
        val encrypted = cipher.doFinal(text.toByteArray())

        val packed = ByteBuffer
            .allocate(1 + iv.size + encrypted.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()

        return packed.joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns null if the text can't be decrypted, for
     * example if the key was lost or the data was changed.
     */
    fun decrypt(encoded: String): String? {

        return try {

            val packed = ByteBuffer.wrap(
                encoded
                    .chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray()
            )

            val iv = ByteArray(packed.get().toInt())
            packed.get(iv)

            val encrypted = ByteArray(packed.remaining())
            packed.get(encrypted)

            val cipher = Cipher.getInstance(TRANSFORMATION)

            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(TAG_BITS, iv)
            )

            String(cipher.doFinal(encrypted))

        } catch (e: Exception) {

            null
        }
    }
}
