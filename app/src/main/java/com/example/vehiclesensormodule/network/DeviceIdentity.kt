package com.example.vehiclesensormodule.network

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/**
 * This phone's permanent ID. The login screen and the
 * background service both need it, so it has one source.
 */
object DeviceIdentity {

    private const val PREFS_NAME = "vehicle_sensor_preferences"
    private const val DEVICE_ID_KEY = "device_id"

    fun get(context: Context): String {

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return preferences.getString(DEVICE_ID_KEY, null)
            ?: UUID.randomUUID().toString().also { newId ->
                preferences.edit {
                    putString(DEVICE_ID_KEY, newId)
                }
            }
    }
}
