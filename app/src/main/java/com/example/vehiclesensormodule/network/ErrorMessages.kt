package com.example.vehiclesensormodule.network

import org.json.JSONObject
import retrofit2.Response

const val CONNECTION_ERROR_MESSAGE =
    "Can't reach the server. It may be waking up, so try again in a moment."

/**
 * The server's error message, without the JSON around it.
 */
fun Response<*>.readableError(fallback: String): String {
    return try {
        JSONObject(errorBody()?.string().orEmpty())
            .optString("message")
            .ifBlank { fallback }
    } catch (e: Exception) {
        fallback
    }
}
