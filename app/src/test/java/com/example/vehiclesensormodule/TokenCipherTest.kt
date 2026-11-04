package com.example.vehiclesensormodule

import com.example.vehiclesensormodule.auth.TokenCipher
import javax.crypto.KeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenCipherTest {

    private fun newKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val key = newKey()
    private val cipher = TokenCipher { key }

    private val token = "header.payload.signature-of-a-login-token"

    @Test
    fun decryptsWhatItEncrypted() {
        assertEquals(token, cipher.decrypt(cipher.encrypt(token)))
    }

    @Test
    fun theStoredValueDoesNotContainTheToken() {
        val stored = cipher.encrypt(token)

        assertFalse(stored.contains("payload"))
        assertFalse(stored.contains(token))
    }

    @Test
    fun encryptingTwiceGivesDifferentResults() {
        assertNotEquals(cipher.encrypt(token), cipher.encrypt(token))
    }

    @Test
    fun aDifferentKeyCannotDecrypt() {
        assertNull(TokenCipher { newKey() }.decrypt(cipher.encrypt(token)))
    }

    @Test
    fun changedDataIsRefused() {
        val stored = cipher.encrypt(token)
        val lastChar = if (stored.last() == '0') '1' else '0'

        assertNull(cipher.decrypt(stored.dropLast(1) + lastChar))
    }

    @Test
    fun rubbishIsRefused() {
        assertNull(cipher.decrypt("not encrypted at all"))
        assertNull(cipher.decrypt(""))
    }
}
