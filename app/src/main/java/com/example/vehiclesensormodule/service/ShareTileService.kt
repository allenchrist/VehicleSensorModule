package com.example.vehiclesensormodule.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.vehiclesensormodule.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A "Share position" button in the phone's Quick Settings, so the driver
 * can connect or disconnect without finding the app.
 *
 * Android only lets an app start location tracking while it's on screen, so
 * turning sharing on opens the app, which then connects by itself. Turning
 * it off works straight from the tile.
 */
class ShareTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)

    private var watching: Job? = null

    // The tile is only shown while the Quick Settings panel is open
    override fun onStartListening() {
        super.onStartListening()

        watching = scope.launch {
            TrackingState.state.collect { showSharing(it.running) }
        }
    }

    override fun onStopListening() {
        watching?.cancel()
        watching = null

        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        if (TrackingState.state.value.running) {
            VehicleTrackingService.stop(this)
        } else {
            openAppAndConnect()
        }
    }

    private fun showSharing(on: Boolean) {
        val tile = qsTile ?: return

        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (on) "On" else "Off"
        }

        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openAppAndConnect() {
        val intent =
            Intent(this, MainActivity::class.java)
                .setAction(MainActivity.ACTION_CONNECT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
