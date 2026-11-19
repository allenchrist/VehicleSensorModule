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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vehiclesensormodule.R
import com.example.vehiclesensormodule.model.VehicleType
import com.example.vehiclesensormodule.network.AppUpdate
import com.example.vehiclesensormodule.sensors.compassPoint
import com.example.vehiclesensormodule.service.MarkingStage
import com.example.vehiclesensormodule.service.SensorReadings
import com.example.vehiclesensormodule.ui.components.AppCard
import com.example.vehiclesensormodule.ui.components.AppTextField
import com.example.vehiclesensormodule.ui.components.BrandMark
import com.example.vehiclesensormodule.ui.components.CardShape
import com.example.vehiclesensormodule.ui.components.ControlShape
import com.example.vehiclesensormodule.ui.components.PrimaryButton
import com.example.vehiclesensormodule.ui.components.ScreenBackground
import com.example.vehiclesensormodule.ui.components.SectionLabel
import com.example.vehiclesensormodule.ui.components.StatusDot
import com.example.vehiclesensormodule.ui.components.VehiclePicker
import com.example.vehiclesensormodule.ui.theme.Accent
import com.example.vehiclesensormodule.ui.theme.AppTheme
import com.example.vehiclesensormodule.ui.theme.Danger
import com.example.vehiclesensormodule.ui.theme.OutlineSubtle
import com.example.vehiclesensormodule.ui.theme.Success
import com.example.vehiclesensormodule.ui.theme.SurfaceCard
import com.example.vehiclesensormodule.ui.theme.SurfaceRaised
import com.example.vehiclesensormodule.ui.theme.TextMuted
import com.example.vehiclesensormodule.ui.theme.TextSecondary
import com.example.vehiclesensormodule.ui.theme.VehicleSensorModuleTheme
import com.example.vehiclesensormodule.ui.theme.Warning
import kotlinx.coroutines.launch

data class DriveState(
    val username: String,
    val vehicleId: String,
    val backendConnected: Boolean,
    val locationAllowed: Boolean,
    val sharingOn: Boolean = true,
    val vehicleType: VehicleType? = null,
    val vehicleTypeError: String? = null,
    val phoneLocationOn: Boolean = true,
    val batteryLimited: Boolean = false,
    val batterySaverOn: Boolean = false,
    val hasRecentFix: Boolean,
    val latitude: Double,
    val longitude: Double,
    val accuracyMetres: Float?,
    val heading: Float,
    val sensors: SensorReadings,
    val marking: MarkingStage = MarkingStage.Off,
    val markedLengthM: Double = 0.0
)

private enum class DriveStatus(val color: Color, val title: String, val detail: String) {
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
        "Not connected",
        "Connect to share your position with your car screen and nearby vehicles."
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
    !sharingOn -> DriveStatus.SharingOff
    !phoneLocationOn -> DriveStatus.PhoneLocationOff
    !backendConnected -> DriveStatus.Connecting
    !hasRecentFix -> DriveStatus.SearchingGps
    else -> DriveStatus.Live
}

@Composable
fun MainScreen(
    state: DriveState,
    onAllowLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onConnect: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    onChangeVehicleType: (VehicleType) -> Unit = {},
    onDeleteAccount: suspend (String) -> String? = { null },
    onOpenLocationSettings: () -> Unit = {},
    onAllowNoBatteryLimits: () -> Unit = {},
    onOpenBatterySaver: () -> Unit = {},
    onOpenMarkRoad: () -> Unit = {},
    update: AppUpdate? = null,
    onUpdate: (AppUpdate) -> Unit = {}
) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var showAccount by rememberSaveable { mutableStateOf(false) }

    val status = state.status()

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopBar(
                username = state.username,
                onLogout = { confirmLogout = true },
                onOpenAccount = { showAccount = true }
            )

            if (update != null) {
                UpdateCard(update = update, onUpdate = { onUpdate(update) })
            }

            StatusHero(
                status = status,
                onAllowLocation = onAllowLocation,
                onOpenSettings = onOpenSettings,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onOpenLocationSettings = onOpenLocationSettings
            )

            if (state.batterySaverOn) {
                PowerWarningCard(
                    title = "Battery Saver is on",
                    detail = "It can delay your position while the screen is locked.",
                    buttonText = "Open Battery Saver",
                    onClick = onOpenBatterySaver
                )
            }

            if (state.batteryLimited) {
                PowerWarningCard(
                    title = "Battery limits are on",
                    detail = "Android can delay your position while the screen is locked.",
                    buttonText = "Remove limits",
                    onClick = onAllowNoBatteryLimits
                )
            }

            VehicleCard(
                state = state,
                onChangeVehicleType = onChangeVehicleType
            )

            ConnectionCard(state = state)

            if (state.marking != MarkingStage.Off) {
                MarkingBanner(state = state, onOpen = onOpenMarkRoad)
            }

            GuideCard(username = state.username)

            SensorDetailsCard(state = state)

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showAccount) {
            AccountDialogs(
                state = state,
                onDismiss = { showAccount = false },
                onDeleteAccount = onDeleteAccount,
                onOpenMarkRoad = {
                    showAccount = false
                    onOpenMarkRoad()
                }
            )
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
private fun TopBar(username: String, onLogout: () -> Unit, onOpenAccount: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandMark(
            size = 40.dp,
            iconSize = 22.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Open your account",
                    onClick = onOpenAccount
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Open your account",
                    onClick = onOpenAccount
                )
        ) {
            Text(
                text = "Connected Vehicle",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1
            )

            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
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
    onAllowLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    val color by animateColorAsState(status.color, label = "statusColor")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SurfaceCard,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.07f))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingCar(
                    color = color,
                    pulsing =
                    status != DriveStatus.LocationOff &&
                        status != DriveStatus.PhoneLocationOff &&
                        status != DriveStatus.SharingOff
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.title,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = status.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            when (status) {
                DriveStatus.LocationOff -> {
                    Spacer(modifier = Modifier.height(14.dp))

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
                }

                DriveStatus.PhoneLocationOff -> {
                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryButton(
                        text = "Open location settings",
                        onClick = onOpenLocationSettings
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DisconnectButton(onDisconnect)
                }

                DriveStatus.SharingOff -> {
                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryButton(
                        text = "Connect",
                        onClick = onConnect
                    )
                }

                else -> {
                    Spacer(modifier = Modifier.height(14.dp))

                    DisconnectButton(onDisconnect)
                }
            }
        }
    }
}

@Composable
private fun DisconnectButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = ControlShape,
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Disconnect", color = TextSecondary)
    }
}

@Composable
private fun PulsingCar(color: Color, pulsing: Boolean) {
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
        modifier = Modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        if (pulsing) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(0.75f + 0.25f * progress)
                    .background(
                        color.copy(alpha = 0.28f * (1f - progress)),
                        CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .background(color.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_car),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * A short notice with one action. It stays small, so it doesn't push
 * the driver's main information off the screen.
 */
@Composable
private fun PowerWarningCard(title: String, detail: String, buttonText: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SurfaceCard,
        border = BorderStroke(1.dp, Warning.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            TextButton(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(buttonText, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

// Reads the theme colours, so it runs as part of drawing the screen
@Composable
private fun gpsQuality(state: DriveState): Pair<String, Color> {
    val accuracy = state.accuracyMetres

    return when {
        !state.sharingOn -> "Off" to TextMuted
        !state.locationAllowed || !state.phoneLocationOn -> "Off" to Danger
        !state.hasRecentFix -> "Searching…" to Warning
        accuracy == null || accuracy <= 15f -> "Good" to Success
        accuracy <= 50f -> "Fair" to Warning
        else -> "Weak" to Warning
    }
}

/**
 * Opens from the icon next to the driver's name. Deleting is a second
 * step, with the password, so it can't happen by accident.
 */
@Composable
private fun AccountDialogs(
    state: DriveState,
    onDismiss: () -> Unit,
    onDeleteAccount: suspend (String) -> String?,
    onOpenMarkRoad: () -> Unit
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    if (!confirming) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = SurfaceRaised,
            title = { Text("Your account") },
            text = {
                Column {
                    Text(
                        text = state.username,
                        style = MaterialTheme.typography.titleMedium
                    )

                    state.vehicleType?.let {
                        Text(
                            text = it.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AccountOption(
                        icon = R.drawable.ic_road,
                        title = "Mark a narrow road",
                        detail = "Drive through a missing road to suggest it",
                        onClick = onOpenMarkRoad
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val context = LocalContext.current

                    AccountOption(
                        icon = if (AppTheme.isLight) R.drawable.ic_dark_mode else R.drawable.ic_light_mode,
                        title = if (AppTheme.isLight) "Dark mode" else "Light mode",
                        detail = if (AppTheme.isLight) {
                            "Dark screens and a darkened map"
                        } else {
                            "Light screens and the map in its normal colours"
                        },
                        onClick = { AppTheme.setLight(context, !AppTheme.isLight) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = true }) {
                    Text("Delete account", color = Danger)
                }
            }
        )

        return
    }

    AlertDialog(
        onDismissRequest = {
            if (!deleting) onDismiss()
        },
        containerColor = SurfaceRaised,
        title = { Text("Delete your account?") },
        text = {
            Column {
                Text(
                    "This permanently deletes your account and logs you out on every device. It can't be undone.",
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = "Your password",
                    icon = R.drawable.ic_lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    errorText = errorMessage,
                    enabled = !deleting
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        deleting = true
                        errorMessage = onDeleteAccount(password)
                        deleting = false
                    }
                },
                enabled = password.isNotEmpty() && !deleting
            ) {
                Text(
                    if (deleting) "Deleting…" else "Delete",
                    color = if (password.isNotEmpty() && !deleting) Danger else TextMuted
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !deleting
            ) {
                Text("Keep account")
            }
        }
    )
}

@Composable
private fun AccountOption(@DrawableRes icon: Int, title: String, detail: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .clickable(onClick = onClick),
        shape = ControlShape,
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun VehicleCard(state: DriveState, onChangeVehicleType: (VehicleType) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }

    val type = state.vehicleType

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(type?.icon ?: R.drawable.ic_car),
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your vehicle",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Text(
                    // Just the type. The server's vehicle number is only
                    // for telling phones apart, so it isn't shown.
                    text = type?.label ?: "Loading…",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }

            TextButton(
                onClick = { choosing = true },
                enabled = type != null
            ) {
                Text("Change", style = MaterialTheme.typography.titleSmall)
            }
        }

        if (state.vehicleTypeError != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = state.vehicleTypeError,
                style = MaterialTheme.typography.bodySmall,
                color = Danger
            )
        }
    }

    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            containerColor = SurfaceRaised,
            title = { Text("What do you drive?") },
            text = {
                VehiclePicker(
                    selected = type,
                    onSelected = {
                        choosing = false
                        onChangeVehicleType(it)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { choosing = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ConnectionCard(state: DriveState) {
    val (gpsText, gpsColor) = gpsQuality(state)

    AppCard {
        SectionLabel("Connection")

        Spacer(modifier = Modifier.height(6.dp))

        ConnectionRow(
            label = "Server",
            value = when {
                !state.sharingOn -> "Not connected"
                state.backendConnected -> "Connected"
                else -> "Connecting…"
            },
            dot = when {
                !state.sharingOn -> TextMuted
                state.backendConnected -> Success
                else -> Warning
            }
        )

        RowDivider()

        ConnectionRow(
            label = "GPS",
            value = gpsText,
            dot = gpsColor
        )

        RowDivider()

        ConnectionRow(
            label = "Sharing",
            value = if (state.sharingOn) "On" else "Off",
            dot = if (state.sharingOn) Success else TextMuted
        )

        RowDivider()

        ConnectionRow(
            label = "Heading",
            value = if (state.sharingOn) compassPoint(state.heading) else "—"
        )
    }
}

/** A newer version of the app has been published. */
@Composable
private fun UpdateCard(update: AppUpdate, onUpdate: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update available",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "${update.versionName}. Download it, then open it to install.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            TextButton(onClick = onUpdate) {
                Text("Update")
            }
        }
    }
}

/** While a road is being marked, a way back to its page. */
@Composable
private fun MarkingBanner(state: DriveState, onOpen: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.marking == MarkingStage.Recording) {
                        "Recording a narrow road · ${state.markedLengthM.toInt()} m"
                    } else {
                        "A recorded road is ready to send"
                    },
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "Open the map to follow it, finish or send it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            TextButton(onClick = onOpen) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun ConnectionRow(label: String, value: String, dot: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )

        if (dot != null) {
            StatusDot(dot)

            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = OutlineSubtle)
}

@Composable
private fun GuideCard(username: String) {
    AppCard {
        GuideRow(
            icon = R.drawable.ic_monitor,
            title = "Your car screen",
            detail = "Open the dashboard website and log in as $username to see alerts and other vehicles."
        )

        Spacer(modifier = Modifier.height(12.dp))

        RowDivider()

        Spacer(modifier = Modifier.height(12.dp))

        GuideRow(
            icon = R.drawable.ic_lock,
            title = "You can lock your phone",
            detail =
            "Sharing carries on with the screen off. To stop it, tap Disconnect, " +
                "use the notification, close the app from recent apps or log out."
        )
    }
}

@Composable
private fun GuideRow(@DrawableRes icon: Int, title: String, detail: String) {
    Row {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
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
                        label = "Heading",
                        values = listOf(
                            "Degrees" to
                                if (state.sharingOn) "%.0f°".format(state.heading) else "—",
                            "Direction" to
                                if (state.sharingOn) compassPoint(state.heading) else "—"
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
private fun ReadingRow(label: String, values: List<Pair<String, String>>) {
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
    vehicleType = VehicleType.Car,
    backendConnected = true,
    locationAllowed = true,
    hasRecentFix = true,
    latitude = 10.958063,
    longitude = 76.956192,
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
