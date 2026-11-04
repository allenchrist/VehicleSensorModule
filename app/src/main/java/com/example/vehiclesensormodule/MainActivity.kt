package com.example.vehiclesensormodule

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope

import com.example.vehiclesensormodule.auth.LoginScreen
import com.example.vehiclesensormodule.auth.SessionManager
import com.example.vehiclesensormodule.auth.SignupScreen
import com.example.vehiclesensormodule.network.ApiClient
import com.example.vehiclesensormodule.network.CONNECTION_ERROR_MESSAGE
import com.example.vehiclesensormodule.network.DeviceAssociationRequest
import com.example.vehiclesensormodule.network.DeviceIdentity
import com.example.vehiclesensormodule.network.readableError
import com.example.vehiclesensormodule.screen.DriveState
import com.example.vehiclesensormodule.screen.MainScreen
import com.example.vehiclesensormodule.service.TrackingState
import com.example.vehiclesensormodule.service.VehicleTrackingService
import com.example.vehiclesensormodule.ui.theme.VehicleSensorModuleTheme

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"

        // With no GPS update for this long, the screen says it is searching again
        private const val GPS_FIX_TIMEOUT_MS = 10_000L

        private const val LOGIN_REJECTED_MESSAGE =
            "You've been logged out. This can happen if your account was used on another phone. Please log in again."
    }

    private lateinit var sessionManager: SessionManager

    private var locationPermissionGranted by mutableStateOf(false)

    // The driver tapped Connect and location hasn't been allowed yet
    private var connectAfterPermission by mutableStateOf(false)

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

        sessionManager = SessionManager(this)

        locationPermissionGranted = hasLocationPermission()


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

            // The service refuses a login even if this screen wasn't open
            val loginRejected by TrackingState.loginRejected.collectAsState()

            val logOut = {

                Log.d(
                    TAG,
                    "Logging out user: ${sessionManager.getUsername()}"
                )

                // Clear saved JWT and username
                sessionManager.clearSession()

                // Stop sharing
                VehicleTrackingService.stop(this@MainActivity)

                // Return to Login screen
                isLoggedIn = false

                // Make sure Signup is not displayed
                showSignup = false

                newUsername = null
            }

            LaunchedEffect(loginRejected) {

                if (loginRejected) {
                    logOut()
                }
            }


            VehicleSensorModuleTheme {

                when {

                    isLoggedIn -> VehicleScreen(

                        onLogout = {

                            // Runs on after this screen closes
                            unlinkThisPhone(sessionManager.getToken())

                            logOut()
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
                                    TrackingState.clearLoginRejected()
                                    isLoggedIn = true
                                }

                                error
                            },

                            onCreateAccount = {
                                showSignup = true
                            },

                            initialUsername = newUsername.orEmpty(),

                            initialError =
                                if (loginRejected) LOGIN_REJECTED_MESSAGE else null,

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


    /**
     * Frees this phone for the next person to log in. If it fails, nothing
     * is lost: logging in on any phone moves the account to that phone.
     */
    private fun unlinkThisPhone(token: String?) {

        if (token == null) {
            return
        }

        lifecycleScope.launch {

            try {

                ApiClient.authService.unlinkDevice(
                    authorization = "Bearer $token",
                    request = DeviceAssociationRequest(
                        deviceId = DeviceIdentity.get(this@MainActivity)
                    )
                )

            } catch (e: Exception) {

                Log.w(
                    TAG,
                    "Couldn't unlink this phone on logout",
                    e
                )
            }
        }
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


    @SuppressLint("BatteryLife")
    private fun requestNoBatteryLimits() {

        openSettingsScreen(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }


    private fun openLocationSettings() {

        openSettingsScreen(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        )
    }


    private fun openBatterySaverSettings() {

        openSettingsScreen(
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        )
    }


    // Not every phone has every settings screen
    private fun openSettingsScreen(intent: Intent) {

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Couldn't open that settings screen", e)
            openAppSettings()
        }
    }


    private fun connect() {

        if (locationPermissionGranted) {
            startSharing()
        } else {
            connectAfterPermission = true
            requestLocationPermission()
        }
    }


    private fun disconnect() {

        connectAfterPermission = false

        VehicleTrackingService.stop(this)
    }


    private fun startSharing() {

        try {
            VehicleTrackingService.start(this)
        } catch (e: IllegalStateException) {

            // Android only allows this while the app is visible.
            // The screen keeps offering the Start sharing button.
            Log.e(TAG, "Couldn't start sharing", e)
        }
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
                        deviceId = DeviceIdentity.get(this)
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
    //
    // Only displays what VehicleTrackingService publishes.
    // ==========================================================

    @Composable
    private fun VehicleScreen(
        onLogout: () -> Unit
    ) {

        val tracking by TrackingState.state.collectAsState()

        var now by remember {
            mutableLongStateOf(System.currentTimeMillis())
        }

        val powerManager = remember {
            getSystemService(PowerManager::class.java)
        }

        var batteryLimited by remember {
            mutableStateOf(false)
        }

        var batterySaverOn by remember {
            mutableStateOf(false)
        }

        val locationManager = remember {
            getSystemService(LocationManager::class.java)
        }

        var phoneLocationOn by remember {
            mutableStateOf(true)
        }

        // Notices when GPS updates have stopped arriving, and
        // when the settings that can stop or delay them change
        LaunchedEffect(Unit) {

            while (true) {

                batteryLimited =
                    !powerManager.isIgnoringBatteryOptimizations(packageName)

                batterySaverOn = powerManager.isPowerSaveMode

                phoneLocationOn =
                    LocationManagerCompat.isLocationEnabled(locationManager)

                delay(1000)

                now = System.currentTimeMillis()
            }
        }


        LaunchedEffect(locationPermissionGranted, connectAfterPermission) {

            if (locationPermissionGranted && connectAfterPermission) {

                connectAfterPermission = false

                startSharing()
            }
        }


        val fixTime = tracking.lastFixAt

        MainScreen(

            state = DriveState(

                username = sessionManager.getUsername().orEmpty(),

                vehicleId = tracking.vehicleId,

                backendConnected = tracking.backendConnected,

                locationAllowed = locationPermissionGranted || !connectAfterPermission,

                sharingOn = tracking.running,

                phoneLocationOn = phoneLocationOn,

                batteryLimited = batteryLimited,

                batterySaverOn = batterySaverOn,

                hasRecentFix =
                    fixTime != null && now - fixTime < GPS_FIX_TIMEOUT_MS,

                latitude = tracking.latitude,
                longitude = tracking.longitude,

                accuracyMetres = tracking.accuracyMetres,

                heading = tracking.heading,

                sensors = tracking.sensors
            ),

            onAllowLocation = ::requestLocationPermission,

            onOpenSettings = ::openAppSettings,

            onLogout = onLogout,

            onConnect = ::connect,

            onDisconnect = ::disconnect,

            onOpenLocationSettings = ::openLocationSettings,

            onAllowNoBatteryLimits = ::requestNoBatteryLimits,

            onOpenBatterySaver = ::openBatterySaverSettings
        )
    }
}
