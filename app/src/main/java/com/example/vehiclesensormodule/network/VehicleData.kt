package com.example.vehiclesensormodule.network

data class VehicleData(
    val vehicleId: String,
    val lat: Double?,
    val lon: Double?,
    val heading: Double?,
    val accelX: Double?,
    val accelY: Double?,
    val accelZ: Double?,
    val gyroX: Double?,
    val gyroY: Double?,
    val gyroZ: Double?,
    val connected: Boolean,
    val lastReceivedAt: Long?
)