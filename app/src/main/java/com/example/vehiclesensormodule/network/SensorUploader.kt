package com.example.vehiclesensormodule.network

import android.util.Log
import com.example.vehiclesensormodule.model.SensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SensorUploader {

    suspend fun checkHealth(serverIp: String): Boolean {

        return withContext(Dispatchers.IO) {

            try {

                val response =
                    ApiClient.createService(serverIp).checkHealth()

                response.isSuccessful

            } catch (e: Exception) {

                Log.e(
                    "SensorUploader",
                    "Health Check Failed: ${e.message}"
                )

                false
            }
        }
    }

    suspend fun upload(
        serverIp: String,
        sensorData: SensorData
    ): Boolean {

        return withContext(Dispatchers.IO) {

            try {

                val response =
                    ApiClient.createService(serverIp)
                        .sendSensorData(sensorData)

                if (response.isSuccessful) {

                    Log.d(
                        "SensorUploader",
                        "Sensor data uploaded successfully"
                    )

                    true

                } else {

                    Log.e(
                        "SensorUploader",
                        "Upload failed: ${response.code()}"
                    )

                    false
                }

            } catch (e: Exception) {

                Log.e(
                    "SensorUploader",
                    "Exception: ${e.message}"
                )

                false
            }
        }
    }
}