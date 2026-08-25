package com.example.vehiclesensormodule.network

import com.example.vehiclesensormodule.model.SensorData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, String>>

    @POST("sensor-data")
    suspend fun sendSensorData(
        @Body sensorData: SensorData
    ): Response<Unit>
}