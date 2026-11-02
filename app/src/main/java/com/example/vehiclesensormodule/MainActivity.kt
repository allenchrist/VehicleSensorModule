package com.example.vehiclesensormodule

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

import com.example.vehiclesensormodule.auth.LoginScreen
import com.example.vehiclesensormodule.auth.SessionManager
import com.example.vehiclesensormodule.auth.SignupScreen
import com.example.vehiclesensormodule.network.ApiClient
import com.example.vehiclesensormodule.network.CONNECTION_ERROR_MESSAGE
import com.example.vehiclesensormodule.network.DeviceAssociationRequest
import com.example.vehiclesensormodule.network.VehicleWebSocketClient
import com.example.vehiclesensormodule.network.readableError
import com.example.vehiclesensormodule.screen.DriveState
import com.example.vehiclesensormodule.screen.MainScreen
import com.example.vehiclesensormodule.screen.SensorReadings
import com.example.vehiclesensormodule.sensors.AccelerometerManager
import com.example.vehiclesensormodule.sensors.CompassManager
import com.example.vehiclesensormodule.sensors.GPSManager
import com.example.vehiclesensormodule.sensors.GyroscopeManager
import com.example.vehiclesensormodule.service.VehicleTrackingService
import com.example.vehiclesensormodule.ui.theme.VehicleSensorModuleTheme

import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"

        private const val KMH_PER_MPS = 3.6f

        // With no GPS update for this long, the screen says it is searching again
        private const val GPS_FIX_TIMEOUT_MS = 10_000L
    }

    private lateinit var gpsManager: GPSManager
    private lateinit var accelerometerManager: AccelerometerManager
    private lateinit var gyroscopeManager: GyroscopeManager
    private lateinit var compassManager: CompassManager

    private lateinit var vehicleWebSocketClient: VehicleWebSocketClient
    private lateinit var sessionManager: SessionManager

    private var locationPermissionGranted by mutableStateOf(false)

    private val locationPermissionRequest =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->

            locationPermissionGranted =
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // --------------------------------------------------
        // Initialize session manager
        // --------------------------------------------------

        sessionManager = SessionManager(this)

        locationPermissionGranted = hasLocationPermission()


        // --------------------------------------------------
        // Initialize sensors
        // --------------------------------------------------

        gpsManager = GPSManager(this)
        accelerometerManager = AccelerometerManager(this)
        gyroscopeManager = GyroscopeManager(this)
        compassManager = CompassManager(this)


        // --------------------------------------------------
        // Initialize WebSocket client
        // --------------------------------------------------

        vehicleWebSocketClient = VehicleWebSocketClient(this)


        // --------------------------------------------------
        // Compose UI
        // --------------------------------------------------

        setContent {

            var isLoggedIn by remember {
                mutableStateOf(sessionManager.isLoggedIn())
            }

            var showSignup by remember {
                mutableStateOf(false)
            }

            // Set after signing up, so the login screen can welcome them
            var newUsername by remember {
                mutableStateOf<String?>(null)
            }


            VehicleSensorModuleTheme {

                when {

                    isLoggedIn -> VehicleScreen(

                        onLogout = {

                            Log.d(
                                TAG,
                                "Logging out user: ${sessionManager.getUsername()}"
                            )

                            // Clear saved JWT and username
                            sessionManager.clearSession()

                            // Disconnect vehicle WebSocket
                            vehicleWebSocketClient.disconnect()

                            // Return to Login screen
                            isLoggedIn = false

                            // Make sure Signup is not displayed
                            showSignup = false

                            newUsername = null
                        }
                    )

                    showSignup -> SignupScreen(

                        onSignupSuccess = { username ->

                            newUsername = username

                            showSignup = false
                        },

                        onBackToLogin = {
                            showSignup = false
                        }
                    )

                    else -> key(newUsername) {

                        LoginScreen(

                            onLoginSuccess = { token, username ->

                                val error =
                                    linkDeviceAndSaveSession(token, username)

                                if (error == null) {
                                    isLoggedIn = true
                                }

                                error
                            },

                            onCreateAccount = {
                                showSignup = true
                            },

                            initialUsername = newUsername.orEmpty(),

                            successMessage = newUsername?.let {
                                "Account created. Log in to connect your vehicle."
                            }
                        )
                    }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()

        // Location may have been allowed from the settings app
        locationPermissionGranted = hasLocationPermission()
    }


    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestLocationPermission() {

        // Android 12+ ignores a request for fine location
        // unless coarse location is requested with it.
        locationPermissionRequest.launch(
            buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)

                // Android 13+ needs this to show the tracking notification
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
        )
    }


    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }


    /**
     * Links this phone to the account. The session is only saved if that
     * succeeds. Returns an error message to show, or null on success.
     */
    private suspend fun linkDeviceAndSaveSession(
        token: String,
        username: String
    ): String? {

        return try {

            val response =
                ApiClient.authService.associateDevice(
                    authorization = "Bearer $token",
                    request = DeviceAssociationRequest(
                        deviceId = vehicleWebSocketClient.getDeviceId()
                    )
                )

            if (response.isSuccessful && response.body() != null) {

                sessionManager.saveSession(
                    token = token,
                    username = username
                )

                Log.d(
                    TAG,
                    "Login successful for user: $username"
                )

                null

            } else {

                Log.e(
                    TAG,
                    "Device association failed: HTTP ${response.code()}"
                )

                response.readableError(
                    "Couldn't link this phone to your account."
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Device association error",
                e
            )

            CONNECTION_ERROR_MESSAGE
        }
    }


    // ==========================================================
    // VEHICLE SCREEN
    // ==========================================================

    @Composable
    private fun VehicleScreen(
        onLogout: () -> Unit
    ) {

        var latitude by remember {
            mutableStateOf(0.0)
        }

        var longitude by remember {
            mutableStateOf(0.0)
        }

        var speedKmh by remember {
            mutableStateOf<Float?>(null)
        }

        var accuracyMetres by remember {
            mutableStateOf<Float?>(null)
        }

        var lastFixAt by remember {
            mutableStateOf<Long?>(null)
        }

        var now by remember {
            mutableStateOf(System.currentTimeMillis())
        }

        var sensors by remember {
            mutableStateOf(SensorReadings())
        }

        var heading by remember {
            mutableStateOf(0f)
        }


        // --------------------------------------------------
        // Vehicle information
        // --------------------------------------------------

        var vehicleId by remember {
            mutableStateOf("")
        }

        var backendConnected by remember {
            mutableStateOf(false)
        }


        // Notices when GPS updates have stopped arriving
        LaunchedEffect(Unit) {

            while (true) {

                delay(1000)

                now = System.currentTimeMillis()
            }
        }


        // --------------------------------------------------
        // WebSocket callbacks
        //
        // IMPORTANT:
        // These are assigned BEFORE connect().
        // --------------------------------------------------

        DisposableEffect(Unit) {

            vehicleWebSocketClient.onConnectionChanged = { connected ->

                Log.d(
                    TAG,
                    "Backend connection: ${
                        if (connected) {
                            "CONNECTED"
                        } else {
                            "DISCONNECTED"
                        }
                    }"
                )

                backendConnected = connected
            }


            vehicleWebSocketClient.onVehicleAssigned = {
                    assignedVehicleId ->

                Log.d(
                    TAG,
                    "Vehicle assigned by backend: $assignedVehicleId"
                )

                vehicleId = assignedVehicleId
            }


            onDispose {

                vehicleWebSocketClient.onConnectionChanged = null
                vehicleWebSocketClient.onVehicleAssigned = null
            }
        }


        // --------------------------------------------------
        // Connect AFTER callbacks are ready
        // --------------------------------------------------

        LaunchedEffect(Unit) {

            vehicleWebSocketClient.connect()
        }


        // --------------------------------------------------
        // Start sensors once location is allowed,
        // and stop them when leaving this screen.
        // --------------------------------------------------

        DisposableEffect(locationPermissionGranted) {

            val trackingService = Intent(
                this@MainActivity,
                VehicleTrackingService::class.java
            )

            if (locationPermissionGranted) {

                // Keeps GPS and sensors running with the screen off
                ContextCompat.startForegroundService(
                    this@MainActivity,
                    trackingService
                )

                // --------------------------------------------------
                // GPS
                // --------------------------------------------------

                gpsManager.startLocationUpdates { location ->

                    latitude = location.latitude
                    longitude = location.longitude

                    speedKmh =
                        if (location.hasSpeed()) location.speed * KMH_PER_MPS else null

                    accuracyMetres =
                        if (location.hasAccuracy()) location.accuracy else null

                    lastFixAt = System.currentTimeMillis()


                    vehicleWebSocketClient.sendTelemetry(

                        latitude = location.latitude,

                        longitude = location.longitude,

                        heading = heading,

                        accelX = sensors.accelX,
                        accelY = sensors.accelY,
                        accelZ = sensors.accelZ,

                        gyroX = sensors.gyroX,
                        gyroY = sensors.gyroY,
                        gyroZ = sensors.gyroZ,

                        speed =
                            if (location.hasSpeed()) location.speed else null,

                        bearing =
                            if (location.hasBearing()) location.bearing else null,

                        accuracy =
                            if (location.hasAccuracy()) location.accuracy else null
                    )
                }


                // --------------------------------------------------
                // Accelerometer
                // --------------------------------------------------

                accelerometerManager.startListening {
                        x, y, z ->

                    sensors = sensors.copy(
                        accelX = x,
                        accelY = y,
                        accelZ = z
                    )
                }


                // --------------------------------------------------
                // Gyroscope
                // --------------------------------------------------

                gyroscopeManager.startListening {
                        x, y, z ->

                    sensors = sensors.copy(
                        gyroX = x,
                        gyroY = y,
                        gyroZ = z
                    )
                }


                // --------------------------------------------------
                // Compass
                // --------------------------------------------------

                compassManager.startListening {

                    heading = it
                }

            } else {

                requestLocationPermission()
            }

            onDispose {

                stopService(trackingService)

                gpsManager.stopLocationUpdates()
                accelerometerManager.stopListening()
                gyroscopeManager.stopListening()
                compassManager.stopListening()
            }
        }


        // --------------------------------------------------
        // UI
        // --------------------------------------------------

        val fixTime = lastFixAt

        MainScreen(

            state = DriveState(

                username = sessionManager.getUsername().orEmpty(),

                vehicleId = vehicleId,

                backendConnected = backendConnected,

                locationAllowed = locationPermissionGranted,

                hasRecentFix =
                    fixTime != null && now - fixTime < GPS_FIX_TIMEOUT_MS,

                latitude = latitude,
                longitude = longitude,

                speedKmh = speedKmh,
                accuracyMetres = accuracyMetres,

                heading = heading,

                sensors = sensors
            ),

            onAllowLocation = ::requestLocationPermission,

            onOpenSettings = ::openAppSettings,

            onLogout = onLogout
        )
    }


    // ------------------------------------------------------
    // Clean shutdown
    // ------------------------------------------------------

    override fun onDestroy() {

        vehicleWebSocketClient.disconnect()

        super.onDestroy()
    }
}
