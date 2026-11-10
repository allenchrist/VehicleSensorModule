package com.example.vehiclesensormodule.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class GPSManager(private val context: Context) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L
    ).build()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationReceived: (Location) -> Unit) {
        stopLocationUpdates()

        val callback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    onLocationReceived(it)
                }
            }
        }

        locationCallback = callback

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            null
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        locationCallback = null
    }
}
