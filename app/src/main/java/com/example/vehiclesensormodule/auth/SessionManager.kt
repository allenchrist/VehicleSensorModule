package com.example.vehiclesensormodule.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SessionManager(context: Context) {

    private companion object {
        const val TAG = "SessionManager"

        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "session_token_key"

        const val ENCRYPTED_TOKEN = "jwt_token_encrypted"

        // Plain text, from before tokens were encrypted
        const val PLAIN_TOKEN = "jwt_token"

        const val USERNAME = "username"
    }

    private val preferences = context.getSharedPreferences(
        "auth_session",
        Context.MODE_PRIVATE
    )

    private val cipher = TokenCipher(::getOrCreateKey)

    // The key stays inside Android's secure storage and can't be copied out
    private fun getOrCreateKey(): SecretKey {

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)

        keyStore.load(null)

        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let {
            return it
        }

        return KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
            }
            .generateKey()
    }

    private fun storeToken(editor: android.content.SharedPreferences.Editor, token: String) {

        try {

            editor
                .putString(ENCRYPTED_TOKEN, cipher.encrypt(token))
                .remove(PLAIN_TOKEN)

        } catch (e: Exception) {

            // Secure storage is faulty on a few phones. Staying logged
            // in matters more here than encrypting this one value.
            Log.w(TAG, "Couldn't encrypt the login token", e)

            editor.putString(PLAIN_TOKEN, token)
        }
    }

    fun saveSession(token: String, username: String) {

        val editor = preferences.edit()

        storeToken(editor, token)

        editor
            .putString(USERNAME, username)
            .apply()
    }

    fun getToken(): String? {

        preferences.getString(ENCRYPTED_TOKEN, null)?.let {
            return cipher.decrypt(it)
        }

        val plainToken = preferences.getString(PLAIN_TOKEN, null)
            ?: return null

        // A login from an older version of the app. Encrypt it now.
        val editor = preferences.edit()

        storeToken(editor, plainToken)

        editor.apply()

        return plainToken
    }

    fun getUsername(): String? {
        return preferences.getString(USERNAME, null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun clearSession() {
        preferences.edit().clear().apply()
    }
}
