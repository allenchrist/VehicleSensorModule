package com.example.vehiclesensormodule

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import com.example.vehiclesensormodule.auth.LoginScreen
import com.example.vehiclesensormodule.auth.SessionManager
import com.example.vehiclesensormodule.auth.SignupScreen
import com.example.vehiclesensormodule.network.ApiClient
import com.example.vehiclesensormodule.network.DeviceAssociationRequest
import com.example.vehiclesensormodule.network.VehicleWebSocketClient
import com.example.vehiclesensormodule.screen.MainScreen
import com.example.vehiclesensormodule.sensors.AccelerometerManager
import com.example.vehiclesensormodule.sensors.CompassManager
import com.example.vehiclesensormodule.sensors.GPSManager
import com.example.vehiclesensormodule.sensors.GyroscopeManager
import com.example.vehiclesensormodule.ui.theme.VehicleSensorModuleTheme
import com.example.vehiclesensormodule.utils.NetworkUtils

import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var gpsManager: GPSManager
    private lateinit var accelerometerManager: AccelerometerManager
    private lateinit var gyroscopeManager: GyroscopeManager
    private lateinit var compassManager: CompassManager

    private lateinit var vehicleWebSocketClient: VehicleWebSocketClient
    private lateinit var sessionManager: SessionManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --------------------------------------------------
        // Initialize session manager
        // --------------------------------------------------

        sessionManager = SessionManager(this)


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


            VehicleSensorModuleTheme {

                // --------------------------------------------------
                // AUTHENTICATION
                // --------------------------------------------------

                if (!isLoggedIn) {

                    if (showSignup) {

                        // --------------------------------------------------
                        // SIGNUP SCREEN
                        // --------------------------------------------------

                        SignupScreen(

                            onSignupSuccess = {

                                Log.d(
                                    TAG,
                                    "Signup successful"
                                )

                                // Return to Login screen
                                showSignup = false
                            },

                            onBackToLogin = {

                                Log.d(
                                    TAG,
                                    "Returning to Login screen"
                                )

                                showSignup = false
                            }
                        )

                    } else {

                        // --------------------------------------------------
                        // LOGIN SCREEN
                        // --------------------------------------------------

                        LoginScreen(

                            onLoginSuccess = { token, username, _ ->

                                lifecycleScope.launch {

                                    try {

                                        // --------------------------------------------------
                                        // Get the existing persistent device UUID
                                        // --------------------------------------------------

                                        val deviceId =
                                            vehicleWebSocketClient.getDeviceId()

                                        Log.d(
                                            TAG,
                                            "Associating user $username with device $deviceId"
                                        )


                                        // --------------------------------------------------
                                        // Associate authenticated user with device
                                        // --------------------------------------------------

                                        val response =
                                            ApiClient.authService.associateDevice(
                                                authorization = "Bearer $token",
                                                request = DeviceAssociationRequest(
                                                    deviceId = deviceId
                                                )
                                            )


                                        // --------------------------------------------------
                                        // Association successful
                                        // --------------------------------------------------

                                        if (
                                            response.isSuccessful &&
                                            response.body() != null
                                        ) {

                                            val responseBody =
                                                response.body()

                                            Log.d(
                                                TAG,
                                                "Device association successful: " +
                                                        "${responseBody?.deviceId}"
                                            )


                                            // --------------------------------------------------
                                            // Save session only after association succeeds
                                            // --------------------------------------------------

                                            sessionManager.saveSession(
                                                token = token,
                                                username = username
                                            )

                                            Log.d(
                                                TAG,
                                                "Login successful for user: $username"
                                            )


                                            // --------------------------------------------------
                                            // Continue to vehicle screen
                                            // --------------------------------------------------

                                            isLoggedIn = true

                                        } else {

                                            Log.e(
                                                TAG,
                                                "Device association failed: " +
                                                        "HTTP ${response.code()} " +
                                                        "${response.errorBody()?.string()}"
                                            )

                                            Toast.makeText(
                                                this@MainActivity,
                                                "Unable to associate this device with your account.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                    } catch (e: Exception) {

                                        Log.e(
                                            TAG,
                                            "Device association error",
                                            e
                                        )

                                        Toast.makeText(
                                            this@MainActivity,
                                            "Connection error. Please try again.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },

                            onCreateAccount = {

                                Log.d(
                                    TAG,
                                    "Create Account clicked"
                                )

                                showSignup = true
                            }
                        )
                    }

                } else {

                    // --------------------------------------------------
                    // AUTHENTICATED VEHICLE SCREEN
                    // --------------------------------------------------

                    VehicleScreen(

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
                        }
                    )
                }
            }
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

        var accelX by remember {
            mutableStateOf(0f)
        }

        var accelY by remember {
            mutableStateOf(0f)
        }

        var accelZ by remember {
            mutableStateOf(0f)
        }

        var gyroX by remember {
            mutableStateOf(0f)
        }

        var gyroY by remember {
            mutableStateOf(0f)
        }

        var gyroZ by remember {
            mutableStateOf(0f)
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

        var pairingCode by remember {
            mutableStateOf("")
        }

        var backendConnected by remember {
            mutableStateOf(false)
        }


        val deviceIp = remember {
            NetworkUtils.getDeviceIpAddress()
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


            vehicleWebSocketClient.onPairingCodeReceived = {
                    code ->

                Log.d(
                    TAG,
                    "Pairing code received: $code"
                )

                pairingCode = code
            }


            onDispose {

                vehicleWebSocketClient.onConnectionChanged = null
                vehicleWebSocketClient.onVehicleAssigned = null
                vehicleWebSocketClient.onPairingCodeReceived = null
            }
        }


        // --------------------------------------------------
        // Connect AFTER callbacks are ready
        // --------------------------------------------------

        LaunchedEffect(Unit) {

            vehicleWebSocketClient.connect()
        }


        // --------------------------------------------------
        // Start sensors
        // --------------------------------------------------

        LaunchedEffect(Unit) {

            val locationPermission =
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED


            if (locationPermission) {

                // --------------------------------------------------
                // GPS
                // --------------------------------------------------

                gpsManager.startLocationUpdates { location ->

                    latitude = location.latitude
                    longitude = location.longitude


                    vehicleWebSocketClient.sendTelemetry(

                        latitude = location.latitude,

                        longitude = location.longitude,

                        heading = heading,

                        accelX = accelX,
                        accelY = accelY,
                        accelZ = accelZ,

                        gyroX = gyroX,
                        gyroY = gyroY,
                        gyroZ = gyroZ
                    )
                }


                // --------------------------------------------------
                // Accelerometer
                // --------------------------------------------------

                accelerometerManager.startListening {
                        x, y, z ->

                    accelX = x
                    accelY = y
                    accelZ = z
                }


                // --------------------------------------------------
                // Gyroscope
                // --------------------------------------------------

                gyroscopeManager.startListening {
                        x, y, z ->

                    gyroX = x
                    gyroY = y
                    gyroZ = z
                }


                // --------------------------------------------------
                // Compass
                // --------------------------------------------------

                compassManager.startListening {

                    heading = it
                }

            } else {

                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    100
                )
            }
        }


        // --------------------------------------------------
        // UI
        // --------------------------------------------------

        MainScreen(

            latitude = latitude,
            longitude = longitude,

            accelX = accelX,
            accelY = accelY,
            accelZ = accelZ,

            gyroX = gyroX,
            gyroY = gyroY,
            gyroZ = gyroZ,

            heading = heading,

            deviceIp = deviceIp,

            vehicleId = vehicleId,
            pairingCode = pairingCode,

            backendConnected = backendConnected,

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