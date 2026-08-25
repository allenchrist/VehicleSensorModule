package com.example.vehiclesensormodule.model

data class SensorData(

    val latitude: Double,

    val longitude: Double,

    val heading: Float,

    val accelerometerX: Float,
    val accelerometerY: Float,
    val accelerometerZ: Float,

    val gyroscopeX: Float,
    val gyroscopeY: Float,
    val gyroscopeZ: Float,

    val timestamp: Long

)