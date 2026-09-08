package com.example.vehiclesensormodule.auth

import android.content.Context

class SessionManager(context: Context) {

    private val preferences = context.getSharedPreferences(
        "auth_session",
        Context.MODE_PRIVATE
    )

    fun saveSession(token: String, username: String) {
        preferences.edit()
            .putString("jwt_token", token)
            .putString("username", username)
            .apply()
    }

    fun getToken(): String? {
        return preferences.getString("jwt_token", null)
    }

    fun getUsername(): String? {
        return preferences.getString("username", null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun clearSession() {
        preferences.edit().clear().apply()
    }
}