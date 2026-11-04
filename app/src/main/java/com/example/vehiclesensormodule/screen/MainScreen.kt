package com.example.vehiclesensormodule.screen

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vehiclesensormodule.R
import com.example.vehiclesensormodule.service.SensorReadings
import com.example.vehiclesensormodule.ui.components.AppCard
import com.example.vehiclesensormodule.ui.components.CardShape
import com.example.vehiclesensormodule.ui.components.ControlShape
import com.example.vehiclesensormodule.ui.components.PrimaryButton
import com.example.vehiclesensormodule.ui.components.ScreenBackground
import com.example.vehiclesensormodule.ui.components.SectionLabel
import com.example.vehiclesensormodule.ui.theme.Accent
import com.example.vehiclesensormodule.ui.theme.Danger
import com.example.vehiclesensormodule.ui.theme.OutlineSubtle
import com.example.vehiclesensormodule.ui.theme.Success
import com.example.vehiclesensormodule.ui.theme.SurfaceCard
import com.example.vehiclesensormodule.ui.theme.SurfaceRaised
import com.example.vehiclesensormodule.ui.theme.TextMuted
import com.example.vehiclesensormodule.ui.theme.TextSecondary
import com.example.vehiclesensormodule.ui.theme.VehicleSensorModuleTheme
import com.example.vehiclesensormodule.ui.theme.Warning

data class DriveState(
    val username: String,
    val vehicleId: String,
    val backendConnected: Boolean,
    val locationAllowed: Boolean,
    val sharingOn: Boolean = true,
    val phoneLocationOn: Boolean = true,
    val batteryLimited: Boolean = false,
    val batterySaverOn: Boolean = false,
    val hasRecentFix: Boolean,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float?,
    val accuracyMetres: Float?,
    val heading: Float,
    val sensors: SensorReadings
)

private enum class DriveStatus(
    val color: Color,
    val title: String,
    val detail: String
) {
    LocationOff(
        Danger,
        "Location is off",
        "Allow location so your car screen and nearby vehicles can see you."
    ),
    PhoneLocationOff(
        Danger,
        "Your phone's location is off",
        "Turn it on so your car screen and nearby vehicles can see you."
    ),
    SharingOff(
        Warning,
        "Sharing is off",
        "Your car screen and nearby vehicles can't see you."
    ),
    Connecting(
        Warning,
        "Connecting…",
        "Trying to reach the server. This keeps retrying by itself."
    ),
    SearchingGps(
        Accent,
        "Finding your position",
        "Waiting for GPS. A clear view of the sky helps."
    ),
    Live(
        Success,
        "You're connected",
        "Sharing your position with your car screen and nearby vehicles."
    )
}

private fun DriveState.status(): DriveStatus = when {
    !locationAllowed -> DriveStatus.LocationOff
    !phoneLocationOn -> DriveStatus.PhoneLocationOff
    !sharingOn -> DriveStatus.SharingOff
    !backendConnected -> DriveStatus.Connecting
    !hasRecentFix -> DriveStatus.SearchingGps
    else -> DriveStatus.Live
}

private fun compassPoint(degrees: Float): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return points[(((degrees % 360) + 360) / 45f + 0.5f).toInt() % points.size]
}

@Composable
fun MainScreen(
    state: DriveState,
    onAllowLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onStartSharing: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    onAllowNoBatteryLimits: () -> Unit = {},
    onOpenBatterySaver: () -> Unit = {}
) {

    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    val status = state.status()

    ScreenBackground {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            TopBar(
                username = state.username,
                onLogout = { confirmLogout = true }
            )

            StatusHero(
                status = status,
                vehicleId = state.vehicleId,
                onAllowLocation = onAllowLocation,
                onOpenSettings = onOpenSettings,
                onStartSharing = onStartSharing,
                onOpenLocationSettings = onOpenLocationSettings
            )

            if (state.batterySaverOn) {
                PowerWarningCard(
                    title = "Battery Saver is on",
                    detail = "It can delay your position while the screen is locked. Turn it off while driving.",
                    buttonText = "Open Battery Saver",
                    onClick = onOpenBatterySaver
                )
            }

            if (state.batteryLimited) {
                PowerWarningCard(
                    title = "Battery limits are on",
                    detail = "Android can delay your position while the screen is locked. Remove the limits for this app.",
                    buttonText = "Remove limits",
                    onClick = onAllowNoBatteryLimits
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val live = status == DriveStatus.Live

                StatTile(
                    label = "Speed",
                    value = state.speedKmh
                        ?.takeIf { live }
                        ?.let { "%.0f".format(it) } ?: "—",
                    unit = "km/h",
                    icon = R.drawable.ic_speed,
                    modifier = Modifier.weight(1f)
                )

                StatTile(
                    label = "Accuracy",
                    value = state.accuracyMetres
                        ?.takeIf { live }
                        ?.let { "%.0f".format(it) } ?: "—",
                    unit = "m",
                    icon = R.drawable.ic_my_location,
                    modifier = Modifier.weight(1f)
                )

                StatTile(
                    label = "Facing",
                    value = "%.0f°".format(state.heading),
                    unit = compassPoint(state.heading),
                    icon = R.drawable.ic_explore,
                    modifier = Modifier.weight(1f)
                )
            }

            CarScreenCard(username = state.username)

            TipCard()

            SensorDetailsCard(state = state)

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (confirmLogout) {
            AlertDialog(
                onDismissRequest = { confirmLogout = false },
                containerColor = SurfaceRaised,
                title = { Text("Log out?") },
                text = {
                    Text(
                        "Your vehicle will stop sharing its position until you log in again.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = onLogout) {
                        Text("Log out", color = Danger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmLogout = false }) {
                        Text("Stay connected")
                    }
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    username: String,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SectionLabel("Connected Vehicle")

            Text(
                text = username,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1
            )
        }

        IconButton(onClick = onLogout) {
            Icon(
                painter = painterResource(R.drawable.ic_logout),
                contentDescription = "Log out",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun StatusHero(
    status: DriveStatus,
    vehicleId: String,
    onAllowLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartSharing: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    val color by animateColorAsState(status.color, label = "statusColor")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SurfaceCard,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.06f))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            PulsingCar(
                color = color,
                pulsing =
                    status != DriveStatus.LocationOff &&
                        status != DriveStatus.PhoneLocationOff &&
                        status != DriveStatus.SharingOff
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = status.title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = status.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (status == DriveStatus.LocationOff) {

                Spacer(modifier = Modifier.height(18.dp))

                PrimaryButton(
                    text = "Allow location",
                    onClick = onAllowLocation
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = ControlShape,
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open settings", color = TextSecondary)
                }

            } else if (status == DriveStatus.PhoneLocationOff) {

                Spacer(modifier = Modifier.height(18.dp))

                PrimaryButton(
                    text = "Open location settings",
                    onClick = onOpenLocationSettings
                )

            } else if (status == DriveStatus.SharingOff) {

                Spacer(modifier = Modifier.height(18.dp))

                PrimaryButton(
                    text = "Start sharing",
                    onClick = onStartSharing
                )

            } else if (vehicleId.isNotEmpty()) {

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = vehicleId,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SurfaceRaised)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PulsingCar(
    color: Color,
    pulsing: Boolean
) {
    val transition = rememberInfiniteTransition(label = "pulse")

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        if (pulsing) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(0.7f + 0.3f * progress)
                    .background(
                        color.copy(alpha = 0.28f * (1f - progress)),
                        CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_car),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    unit: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1
            )

            Text(
                text = "$label · $unit",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PowerWarningCard(
    title: String,
    detail: String,
    buttonText: String,
    onClick: () -> Unit
) {
    AppCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Warning
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = buttonText,
            onClick = onClick
        )
    }
}

@Composable
private fun CarScreenCard(username: String) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_monitor)

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Your car screen",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Open the dashboard website at /$username to see alerts and other vehicles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TipCard() {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_lock)

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "You can lock your phone",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Sharing carries on with the screen off. To stop it, close the app from recent apps, use the notification or log out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun IconTile(@DrawableRes icon: Int) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(Accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SensorDetailsCard(state: DriveState) {

    var expanded by rememberSaveable { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) "Hide sensor details" else "Show sensor details"
                    ) { expanded = !expanded }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sensor details",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(
                        start = 18.dp,
                        end = 18.dp,
                        bottom = 18.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ReadingRow(
                        label = "Position",
                        values = listOf(
                            "Lat" to "%.6f".format(state.latitude),
                            "Lon" to "%.6f".format(state.longitude)
                        )
                    )

                    ReadingRow(
                        label = "Accelerometer",
                        values = listOf(
                            "X" to "%.2f".format(state.sensors.accelX),
                            "Y" to "%.2f".format(state.sensors.accelY),
                            "Z" to "%.2f".format(state.sensors.accelZ)
                        )
                    )

                    ReadingRow(
                        label = "Gyroscope",
                        values = listOf(
                            "X" to "%.2f".format(state.sensors.gyroX),
                            "Y" to "%.2f".format(state.sensors.gyroY),
                            "Z" to "%.2f".format(state.sensors.gyroZ)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingRow(
    label: String,
    values: List<Pair<String, String>>
) {
    Column {
        SectionLabel(label)

        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { (name, value) ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceRaised)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private val PreviewState = DriveState(
    username = "allenchrist",
    vehicleId = "VEHICLE_001",
    backendConnected = true,
    locationAllowed = true,
    hasRecentFix = true,
    latitude = 10.958063,
    longitude = 76.956192,
    speedKmh = 24f,
    accuracyMetres = 6f,
    heading = 92f,
    sensors = SensorReadings(0.12f, -0.08f, 9.78f, 0.01f, 0.02f, -0.01f)
)

@Preview(showSystemUi = true)
@Composable
private fun LivePreview() {
    VehicleSensorModuleTheme {
        MainScreen(PreviewState, {}, {}, {})
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ConnectingPreview() {
    VehicleSensorModuleTheme {
        MainScreen(PreviewState.copy(backendConnected = false), {}, {}, {})
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LocationOffPreview() {
    VehicleSensorModuleTheme {
        MainScreen(PreviewState.copy(locationAllowed = false), {}, {}, {})
    }
}
