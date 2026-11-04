package com.example.vehiclesensormodule.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SensorReadings(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f
)

data class TrackingSnapshot(
    val running: Boolean = false,
    val backendConnected: Boolean = false,
    val vehicleId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMetres: Float? = null,
    val heading: Float = 0f,
    val sensors: SensorReadings = SensorReadings(),
    val lastFixAt: Long? = null
)

/**
 * How VehicleTrackingService tells the screen what's happening. It lives
 * outside both, because each can be closed while the other carries on.
 */
object TrackingState {

    private val snapshot = MutableStateFlow(TrackingSnapshot())
    val state: StateFlow<TrackingSnapshot> = snapshot.asStateFlow()

    private val loginRejectedFlag = MutableStateFlow(false)
    val loginRejected: StateFlow<Boolean> = loginRejectedFlag.asStateFlow()

    fun update(change: (TrackingSnapshot) -> TrackingSnapshot) {
        snapshot.update(change)
    }

    fun reset() {
        snapshot.value = TrackingSnapshot()
    }

    fun markLoginRejected() {
        loginRejectedFlag.value = true
    }

    fun clearLoginRejected() {
        loginRejectedFlag.value = false
    }
}
