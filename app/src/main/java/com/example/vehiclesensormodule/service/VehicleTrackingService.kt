package com.example.vehiclesensormodule.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.vehiclesensormodule.MainActivity
import com.example.vehiclesensormodule.R
import com.example.vehiclesensormodule.auth.SessionManager
import com.example.vehiclesensormodule.network.VehicleWebSocketClient
import com.example.vehiclesensormodule.sensors.AccelerometerManager
import com.example.vehiclesensormodule.sensors.CompassManager
import com.example.vehiclesensormodule.sensors.GPSManager
import com.example.vehiclesensormodule.sensors.GyroscopeManager

/**
 * Owns the GPS, sensors and connection to the backend during a drive.
 *
 * The phone is only the vehicle's connectivity module, so it may sit with its
 * screen off. Everything lives here, not in MainActivity, so sharing carries
 * on when Android closes the screen. It ends when the driver closes the app
 * from recent apps, logs out or taps Stop on the notification.
 *
 * MainActivity only displays what this publishes through TrackingState.
 */
class VehicleTrackingService : Service() {

    companion object {
        private const val TAG = "VehicleTracking"

        private const val CHANNEL_ID = "vehicle_tracking"
        private const val NOTIFICATION_ID = 1

        private const val ACTION_STOP = "com.example.vehiclesensormodule.STOP_SHARING"

        // Often enough for the heading on screen to follow a turn
        private const val SENSOR_PUBLISH_INTERVAL_MS = 300L

        // GPS only arrives about once a second, so a turn is also sent on
        // its own, as soon as the compass moves this far
        private const val HEADING_SEND_DEGREES = 3f
        private const val HEADING_SEND_INTERVAL_MS = 150L

        // GPS is requested every second, so a longer wait than this is a delay
        private const val GPS_GAP_LOG_MS = 3000L

        // A safety limit, in case the service is never stopped
        private const val WAKE_LOCK_TIMEOUT_MS = 12 * 60 * 60 * 1000L

        // Must be called while the app is visible, as Android only
        // allows location tracking to start from the foreground
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, VehicleTrackingService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(
                Intent(context, VehicleTrackingService::class.java)
            )
        }
    }

    private lateinit var gpsManager: GPSManager
    private lateinit var accelerometerManager: AccelerometerManager
    private lateinit var gyroscopeManager: GyroscopeManager
    private lateinit var compassManager: CompassManager
    private lateinit var vehicleClient: VehicleWebSocketClient

    private var tracking = false

    private var wakeLock: PowerManager.WakeLock? = null

    // Latest readings, sent along with each GPS update
    private var sensors = SensorReadings()
    private var heading = 0f

    private var lastLocationAt = 0L

    private var sentHeading = 0f
    private var headingSentAt = 0L

    private val publishHandler = Handler(Looper.getMainLooper())

    private var lastNetwork: Network? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {

            val changed = lastNetwork != null && lastNetwork != network

            lastNetwork = network

            vehicleClient.onNetworkAvailable(networkChanged = changed)
        }
    }

    private val publishSensors = object : Runnable {
        override fun run() {
            TrackingState.update {
                it.copy(sensors = sensors, heading = heading)
            }

            publishHandler.postDelayed(this, SENSOR_PUBLISH_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            startInForeground()
        } catch (e: RuntimeException) {

            // Android refused, because location isn't allowed or the
            // app isn't visible. The screen offers to start it again.
            Log.e(TAG, "Couldn't start sharing", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!tracking) {
            startTracking()
        }

        // Android doesn't allow location tracking to restart without
        // the app being visible, so it's restarted by opening the app
        return START_NOT_STICKY
    }

    // The driver swiped the app away from recent apps
    override fun onTaskRemoved(rootIntent: Intent?) {

        stopSelf()

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {

        publishHandler.removeCallbacks(publishSensors)

        if (tracking) {
            getSystemService(ConnectivityManager::class.java)
                .unregisterNetworkCallback(networkCallback)

            gpsManager.stopLocationUpdates()
            accelerometerManager.stopListening()
            gyroscopeManager.stopListening()
            compassManager.stopListening()
            vehicleClient.disconnect()
        }

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

        TrackingState.reset()

        super.onDestroy()
    }

    private fun startTracking() {

        val sessionManager = SessionManager(this)
        val token = sessionManager.getToken()

        if (token == null) {
            stopSelf()
            return
        }

        tracking = true

        // Keeps the phone processing updates while its screen is off
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "VehicleSensorModule:tracking"
            )
            .apply { acquire(WAKE_LOCK_TIMEOUT_MS) }

        gpsManager = GPSManager(this)
        accelerometerManager = AccelerometerManager(this)
        gyroscopeManager = GyroscopeManager(this)
        compassManager = CompassManager(this)
        vehicleClient = VehicleWebSocketClient(this)

        TrackingState.update { it.copy(running = true) }

        vehicleClient.authToken = token

        vehicleClient.onConnectionChanged = { connected ->
            TrackingState.update { it.copy(backendConnected = connected) }
        }

        vehicleClient.onVehicleAssigned = { vehicleId ->
            TrackingState.update { it.copy(vehicleId = vehicleId) }
        }

        vehicleClient.onAuthorizationRejected = {
            sessionManager.clearSession()
            TrackingState.markLoginRejected()
            stopSelf()
        }

        vehicleClient.connect()

        getSystemService(ConnectivityManager::class.java)
            .registerDefaultNetworkCallback(networkCallback)

        gpsManager.startLocationUpdates(::onLocation)

        accelerometerManager.startListening { x, y, z ->
            sensors = sensors.copy(accelX = x, accelY = y, accelZ = z)

            compassManager.onGravityChanged(x, y, z)
        }

        gyroscopeManager.startListening { x, y, z ->
            sensors = sensors.copy(gyroX = x, gyroY = y, gyroZ = z)
        }

        compassManager.startListening(::onHeading)

        publishHandler.post(publishSensors)
    }

    private fun onHeading(newHeading: Float) {

        heading = newHeading

        val now = SystemClock.elapsedRealtime()

        // The backend only takes a heading once it knows where the car is
        if (
            lastLocationAt == 0L ||
            now - headingSentAt < HEADING_SEND_INTERVAL_MS ||
            angleBetween(newHeading, sentHeading) < HEADING_SEND_DEGREES
        ) {
            return
        }

        if (vehicleClient.sendHeading(newHeading)) {
            sentHeading = newHeading
            headingSentAt = now
        }
    }

    private fun angleBetween(a: Float, b: Float): Float {
        val difference = Math.abs(a - b) % 360f

        return if (difference > 180f) 360f - difference else difference
    }

    private fun onLocation(location: Location) {

        val receivedAt = SystemClock.elapsedRealtime()

        if (lastLocationAt != 0L && receivedAt - lastLocationAt > GPS_GAP_LOG_MS) {
            Log.w(TAG, "No GPS update for ${receivedAt - lastLocationAt} ms")
        }

        lastLocationAt = receivedAt

        val speed = if (location.hasSpeed()) location.speed else null
        val accuracy = if (location.hasAccuracy()) location.accuracy else null

        TrackingState.update {
            it.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMetres = accuracy,
                lastFixAt = System.currentTimeMillis()
            )
        }

        sentHeading = heading
        headingSentAt = receivedAt

        vehicleClient.sendTelemetry(
            latitude = location.latitude,
            longitude = location.longitude,
            heading = heading,
            accelX = sensors.accelX,
            accelY = sensors.accelY,
            accelZ = sensors.accelZ,
            gyroX = sensors.gyroX,
            gyroY = sensors.gyroY,
            gyroZ = sensors.gyroZ,
            speed = speed,
            bearing = if (location.hasBearing()) location.bearing else null,
            accuracy = accuracy
        )
    }

    private fun startInForeground() {

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

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopSharing = PendingIntent.getService(
            this,
            1,
            Intent(this, VehicleTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return builder
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Sharing your location with nearby vehicles")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop sharing",
                    stopSharing
                ).build()
            )
            .build()
    }
}
