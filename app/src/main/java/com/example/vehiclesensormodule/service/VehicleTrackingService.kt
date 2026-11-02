package com.example.vehiclesensormodule.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.example.vehiclesensormodule.R

/**
 * Keeps the app in the foreground state while a drive is in progress.
 *
 * The phone is only the vehicle's connectivity module, so it may sit with
 * its screen off. Without this service Android limits location updates as
 * soon as the app is no longer visible. MainActivity still owns the GPS,
 * sensors and WebSocket, so tracking ends if the app is closed.
 */
class VehicleTrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "vehicle_tracking"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }

        // Tracking cannot resume without MainActivity
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {

        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_ID,
                            "Vehicle tracking",
                            NotificationManager.IMPORTANCE_LOW
                        )
                    )

                Notification.Builder(this, CHANNEL_ID)

            } else {

                @Suppress("DEPRECATION")
                Notification.Builder(this)
            }

        return builder
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Sharing your location with nearby vehicles")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }
}
