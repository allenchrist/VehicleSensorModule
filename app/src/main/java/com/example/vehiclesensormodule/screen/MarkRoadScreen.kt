package com.example.vehiclesensormodule.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.vehiclesensormodule.R
import com.example.vehiclesensormodule.marking.GeoPoint
import com.example.vehiclesensormodule.marking.RoadOutline
import com.example.vehiclesensormodule.service.MarkingStage
import com.example.vehiclesensormodule.ui.components.AppTextField
import com.example.vehiclesensormodule.ui.components.CardShape
import com.example.vehiclesensormodule.ui.components.MessageBanner
import com.example.vehiclesensormodule.ui.components.MessageKind
import com.example.vehiclesensormodule.ui.components.PrimaryButton
import com.example.vehiclesensormodule.ui.components.ScreenBackground
import com.example.vehiclesensormodule.ui.map.RoadMap
import com.example.vehiclesensormodule.ui.theme.Accent
import com.example.vehiclesensormodule.ui.theme.Danger
import com.example.vehiclesensormodule.ui.theme.OutlineSubtle
import com.example.vehiclesensormodule.ui.theme.SurfaceCard
import com.example.vehiclesensormodule.ui.theme.SurfaceRaised
import com.example.vehiclesensormodule.ui.theme.TextMuted
import com.example.vehiclesensormodule.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Suggesting a narrow road by driving through it, with a live map: start,
 * drive to the end while the track is drawn, finish, then name it and send
 * it. An admin reviews it on the website before it's used.
 */
@Composable
fun MarkRoadScreen(
    state: DriveState,
    track: List<GeoPoint>,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onSend: suspend (name: String, reason: String) -> String?
) {
    var naming by rememberSaveable { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(false) }
    var satellite by rememberSaveable { mutableStateOf(false) }
    var recentre by remember { mutableIntStateOf(0) }

    // While recording, the map keeps following you. Otherwise you can move
    // it to look over the road, and come back with the location button.
    val recording = state.marking == MarkingStage.Recording

    val position =
        if (state.sharingOn && state.hasRecentFix) GeoPoint(state.latitude, state.longitude) else null

    // The shape that will be sent, once the drive is finished
    val outline = remember(track, state.marking) {
        if (state.marking == MarkingStage.Ready) RoadOutline.build(track) else null
    }

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                }

                Text("Mark a narrow road", style = MaterialTheme.typography.titleLarge)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                RoadMap(
                    position = position,
                    track = track,
                    outline = outline,
                    satellite = satellite,
                    modifier = Modifier.fillMaxSize(),
                    follow = recording,
                    recentre = recentre
                )

                if (!recording && position != null) {
                    MyLocationButton(
                        onClick = { recentre++ },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }

                MapStyleSwitch(
                    satellite = satellite,
                    onChange = { satellite = it },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )

                if (recording) {
                    RecordingBadge(
                        metres = state.markedLengthM.toInt(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                }
            }

            ControlPanel(
                state = state,
                sent = sent,
                onConnect = onConnect,
                onStart = {
                    sent = false
                    onStart()
                },
                onFinish = onFinish,
                onDiscard = onDiscard,
                onName = { naming = true }
            )
        }

        if (naming && state.marking == MarkingStage.Ready) {
            SendRoadDialog(
                onDismiss = { naming = false },
                onSend = { name, reason ->
                    val error = onSend(name, reason)

                    if (error == null) {
                        naming = false
                        sent = true
                    }

                    error
                }
            )
        }
    }
}

@Composable
private fun MapStyleSwitch(satellite: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = SurfaceRaised,
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            listOf(false to "Map", true to "Satellite").forEach { (isSatellite, label) ->
                val selected = satellite == isSatellite

                TextButton(
                    onClick = { onChange(isSatellite) },
                    shape = CircleShape,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (selected) Accent.copy(alpha = 0.18f) else Color.Transparent,
                        contentColor = if (selected) Accent else TextSecondary
                    )
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MyLocationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = SurfaceRaised,
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_my_location),
                contentDescription = "Go to my location",
                tint = Accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun RecordingBadge(metres: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = SurfaceRaised,
        border = BorderStroke(1.dp, Danger)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Danger)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Recording · $metres m", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun ControlPanel(
    state: DriveState,
    sent: Boolean,
    onConnect: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onName: () -> Unit
) {
    val metres = state.markedLengthM.toInt()
    val longEnough = state.markedLengthM >= RoadOutline.MIN_LENGTH_M

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                !state.sharingOn -> {
                    Hint("Connect first, so the app can follow your position while you drive.")
                    PrimaryButton(text = "Connect", onClick = onConnect)
                }

                state.marking == MarkingStage.Off -> {
                    if (sent) {
                        MessageBanner("Sent for review. Thank you!", MessageKind.Success)
                    }

                    Hint(
                        "Stop just before the narrow road, tap Start, then drive through it. " +
                            "Your track is drawn on the map as you go."
                    )

                    PrimaryButton(text = "Start recording", onClick = onStart, enabled = state.hasRecentFix)

                    if (!state.hasRecentFix) {
                        Text("Waiting for GPS…", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }

                state.marking == MarkingStage.Recording -> {
                    Hint(
                        if (longEnough) {
                            "Drive to the end of the narrow road, then tap Finish."
                        } else {
                            "Keep driving. Finish works after ${RoadOutline.MIN_LENGTH_M.toInt()} m."
                        }
                    )

                    PrimaryButton(text = "Finish", onClick = onFinish, enabled = longEnough)

                    TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = TextSecondary)
                    }
                }

                else -> {
                    Text("Recorded $metres m", style = MaterialTheme.typography.titleMedium)

                    Hint("The green shape is what will be sent. Name it to send it for review.")

                    PrimaryButton(text = "Name it and send", onClick = onName)

                    TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
                        Text("Discard", color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
}

@Composable
private fun SendRoadDialog(onDismiss: () -> Unit, onSend: suspend (name: String, reason: String) -> String?) {
    var name by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {
            if (!sending) onDismiss()
        },
        containerColor = SurfaceRaised,
        title = { Text("Send this road") },
        text = {
            Column {
                AppTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = "Road name",
                    icon = R.drawable.ic_road,
                    errorText = errorMessage,
                    enabled = !sending
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Why it's narrow (optional)",
                    icon = R.drawable.ic_note,
                    imeAction = ImeAction.Done,
                    enabled = !sending
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        sending = true
                        errorMessage = onSend(name, reason)
                        sending = false
                    }
                },
                enabled = name.isNotBlank() && !sending
            ) {
                Text(if (sending) "Sending…" else "Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !sending) {
                Text("Not now")
            }
        }
    )
}
