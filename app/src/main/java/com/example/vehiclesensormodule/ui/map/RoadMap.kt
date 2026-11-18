package com.example.vehiclesensormodule.ui.map

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.vehiclesensormodule.BuildConfig
import com.example.vehiclesensormodule.marking.GeoPoint
import com.example.vehiclesensormodule.ui.theme.Accent
import com.example.vehiclesensormodule.ui.theme.LocalPalette
import com.example.vehiclesensormodule.ui.theme.Success
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as MapPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

// Close enough to see a lane a few metres wide
private const val DRIVING_ZOOM = 19.0

// Shown until the first GPS position arrives
private val COIMBATORE = MapPoint(11.0, 76.96)

// The dot for your position, in metres
private const val POSITION_RADIUS_M = 4.0

// Satellite photos from Esri, as on the website. They show the buildings
// either side of a lane, which helps when marking one. Esri numbers tiles
// zoom, row, column, unlike OpenStreetMap.
private val ESRI_SATELLITE =
    object : OnlineTileSourceBase(
        "EsriWorldImagery",
        0,
        19,
        256,
        "",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
        "Imagery © Esri"
    ) {
        override fun getTileURLString(index: Long): String =
            baseUrl + MapTileIndex.getZoom(index) + "/" + MapTileIndex.getY(index) + "/" + MapTileIndex.getX(index)
    }

/**
 * A map following your position, with the track recorded so far and, once
 * finished, the road shape that will be sent.
 */
@Composable
fun RoadMap(
    position: GeoPoint?,
    track: List<GeoPoint>,
    outline: List<List<Double>>?,
    satellite: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }
    val darkMode = LocalPalette.current.isDark

    DisposableEffect(mapView) {
        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // A map view draws past its edges unless it's clipped
    AndroidView(
        factory = { mapView },
        modifier = modifier.clipToBounds(),
        update = { view ->
            showMapStyle(view, satellite, darkMode)
            showOnMap(view, position, track, outline)
        }
    )
}

private fun createMapView(context: Context): MapView {
    // OpenStreetMap refuses apps that don't say who they are and how to reach
    // them, so this names the app and its website. Tiles are cached in the
    // app's own folder, as OpenStreetMap asks, so no storage permission is needed.
    Configuration.getInstance().apply {
        userAgentValue = "ConnectedVehicle/${BuildConfig.VERSION_NAME} (+https://narrow-road-navigation.vercel.app)"
        // A new folder, so blocked tiles saved by an earlier version aren't shown
        osmdroidBasePath = File(context.cacheDir, "map")
        osmdroidTileCache = File(context.cacheDir, "map/tiles")
    }

    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)

        // Otherwise the map looks tiny on a sharp phone screen
        isTilesScaledToDpi = true

        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        controller.setZoom(DRIVING_ZOOM)
        controller.setCenter(COIMBATORE)
    }
}

private fun showMapStyle(view: MapView, satellite: Boolean, darkMode: Boolean) {
    val source = if (satellite) ESRI_SATELLITE else TileSourceFactory.MAPNIK

    if (view.tileProvider.tileSource != source) {
        view.setTileSource(source)
    }

    // Photos stay as they are. The street map is darkened in dark mode, and
    // shown in its normal colours in light mode.
    view.overlayManager.tilesOverlay.setColorFilter(if (!satellite && darkMode) darkMapFilter() else null)
}

// Turns the standard light map into a dark navy one, as the website does
private fun darkMapFilter(): ColorMatrixColorFilter {
    val (red, green, blue) = Triple(0.45f, 0.55f, 0.72f)

    // Each colour is a tinted copy of the inverted brightness
    fun row(scale: Float, offset: Float) =
        floatArrayOf(-0.299f * scale, -0.587f * scale, -0.114f * scale, 0f, 255f * scale + offset)

    return ColorMatrixColorFilter(
        ColorMatrix(row(red, 8f) + row(green, 14f) + row(blue, 24f) + floatArrayOf(0f, 0f, 0f, 1f, 0f))
    )
}

private fun showOnMap(view: MapView, position: GeoPoint?, track: List<GeoPoint>, outline: List<List<Double>>?) {
    view.overlays.clear()

    // Line widths in screen pixels, for this phone's screen
    val dp = view.resources.displayMetrics.density

    // The shape that will be sent, softly filled
    if (outline != null) {
        view.overlays += Polygon(view).apply {
            points = outline.map { MapPoint(it[0], it[1]) }
            fillPaint.color = Success.copy(alpha = 0.22f).toArgb()
            outlinePaint.color = Success.toArgb()
            outlinePaint.strokeWidth = 2 * dp
            setOnClickListener { _, _, _ -> true }
        }
    }

    // The drive recorded so far
    if (track.size >= 2) {
        view.overlays += Polyline(view).apply {
            setPoints(track.map { MapPoint(it.latitude, it.longitude) })
            outlinePaint.color = Accent.toArgb()
            outlinePaint.strokeWidth = 5 * dp
            setOnClickListener { _, _, _ -> true }
        }
    }

    if (position != null) {
        val here = MapPoint(position.latitude, position.longitude)

        view.overlays += Polygon(view).apply {
            points = Polygon.pointsAsCircle(here, POSITION_RADIUS_M)
            fillPaint.color = Accent.toArgb()
            outlinePaint.color = android.graphics.Color.WHITE
            outlinePaint.strokeWidth = 2 * dp
            setOnClickListener { _, _, _ -> true }
        }

        view.controller.animateTo(here)
    }

    // The map's credits, which its licence requires
    view.overlays += CopyrightOverlay(view.context).apply {
        setTextColor(android.graphics.Color.LTGRAY)
        setAlignRight(true)
    }

    view.invalidate()
}
