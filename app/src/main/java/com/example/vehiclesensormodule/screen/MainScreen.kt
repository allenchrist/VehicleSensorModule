package com.example.vehiclesensormodule.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    latitude: Double,
    longitude: Double,

    accelX: Float,
    accelY: Float,
    accelZ: Float,

    gyroX: Float,
    gyroY: Float,
    gyroZ: Float,

    heading: Float,

    deviceIp: String,

    // Vehicle information
    vehicleId: String,
    pairingCode: String,

    // Backend connection status
    backendConnected: Boolean
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {

        // --------------------------------------------------
        // TITLE
        // --------------------------------------------------

        Text(
            text = "Vehicle Sensor Module",
            style = MaterialTheme.typography.headlineMedium
        )

        Divider()

        // --------------------------------------------------
        // VEHICLE CONNECTION
        // --------------------------------------------------

        Text(
            text = "☁️ Vehicle Connection",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = if (backendConnected) {
                "Backend : CONNECTED"
            } else {
                "Backend : DISCONNECTED"
            }
        )

        Text(
            text = if (vehicleId.isNotEmpty()) {
                "Vehicle ID : $vehicleId"
            } else {
                "Vehicle ID : Connecting..."
            }
        )

        Text(
            text = if (pairingCode.isNotEmpty()) {
                "Pairing Code : $pairingCode"
            } else {
                "Pairing Code : Waiting..."
            }
        )

        if (pairingCode.isNotEmpty()) {

            Text(
                text = "Enter this pairing code on the dashboard.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Divider()

        // --------------------------------------------------
        // GPS
        // --------------------------------------------------

        Text(
            text = "📍 GPS",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Latitude  : %.6f".format(latitude)
        )

        Text(
            text = "Longitude : %.6f".format(longitude)
        )

        Divider()

        // --------------------------------------------------
        // COMPASS
        // --------------------------------------------------

        Text(
            text = "🧭 Compass",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Heading : %.1f°".format(heading)
        )

        Divider()

        // --------------------------------------------------
        // ACCELEROMETER
        // --------------------------------------------------

        Text(
            text = "📈 Accelerometer",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "X : %.2f".format(accelX)
        )

        Text(
            text = "Y : %.2f".format(accelY)
        )

        Text(
            text = "Z : %.2f".format(accelZ)
        )

        Divider()

        // --------------------------------------------------
        // GYROSCOPE
        // --------------------------------------------------

        Text(
            text = "🔄 Gyroscope",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "X : %.2f".format(gyroX)
        )

        Text(
            text = "Y : %.2f".format(gyroY)
        )

        Text(
            text = "Z : %.2f".format(gyroZ)
        )

        Divider()

        // --------------------------------------------------
        // NETWORK
        // --------------------------------------------------

        Text(
            text = "🌐 Network",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Device IP : $deviceIp"
        )

        Text(
            text = "Backend : Render WebSocket"
        )
    }
}