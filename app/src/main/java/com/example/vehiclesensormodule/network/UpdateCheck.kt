package com.example.vehiclesensormodule.network

import com.example.vehiclesensormodule.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** A newer version of the app, published on GitHub. */
data class AppUpdate(val versionName: String, val downloadUrl: String)

/**
 * Asks GitHub for the newest published version of the app. Each version is
 * published by the Release workflow with a tag such as v12, where 12 is its
 * version code.
 */
object UpdateCheck {

    const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/allenchrist/VehicleSensorModule/releases/latest"

    const val DOWNLOAD_URL =
        "https://github.com/allenchrist/VehicleSensorModule/releases/latest/download/connected-vehicle.apk"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** The version code in a release tag, such as 12 in v12. */
    fun versionFromTag(tag: String): Int? = tag.trim().removePrefix("v").toIntOrNull()

    /**
     * The newer version, or null when this one is the newest, or GitHub
     * can't be reached. A failed check just waits until the app next opens.
     */
    suspend fun newerVersion(currentVersionCode: Int = BuildConfig.VERSION_CODE): AppUpdate? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(LATEST_RELEASE_URL)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "ConnectedVehicle/${BuildConfig.VERSION_NAME}")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null

                    val release = JSONObject(response.body?.string().orEmpty())
                    val version = versionFromTag(release.optString("tag_name"))
                        ?: return@runCatching null

                    if (version <= currentVersionCode) return@runCatching null

                    AppUpdate(
                        versionName = release.optString("name").ifBlank { "version $version" },
                        downloadUrl = DOWNLOAD_URL
                    )
                }
            }.getOrNull()
        }
}
