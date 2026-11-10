package com.example.vehiclesensormodule.sensors

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val COMPASS_POINTS =
    listOf(
        "North",
        "North-East",
        "East",
        "South-East",
        "South",
        "South-West",
        "West",
        "North-West"
    )

/** The nearest of the eight compass directions, such as "North-West". */
fun compassPoint(degrees: Float): String {
    val normalized = ((degrees % 360f) + 360f) % 360f

    return COMPASS_POINTS[((normalized / 45f) + 0.5f).toInt() % COMPASS_POINTS.size]
}

/** How far apart two headings are, the short way round: 0 to 180 degrees. */
fun angleBetween(a: Float, b: Float): Float {
    val difference = abs(a - b) % 360f

    return if (difference > 180f) 360f - difference else difference
}

/**
 * Whether a new heading is worth sending on its own. GPS updates carry the
 * heading anyway, so this only sends a real turn, and not too often.
 */
fun shouldSendHeading(
    heading: Float,
    lastSent: Float,
    sinceLastSentMs: Long,
    minDegrees: Float,
    minIntervalMs: Long
): Boolean = sinceLastSentMs >= minIntervalMs && angleBetween(heading, lastSent) >= minDegrees

/**
 * Smooths compass readings. Each reading moves the heading [smoothing] of
 * the way towards the new value, as a direction, so 359° and 1° average
 * to 0°, not 180°.
 */
class HeadingSmoother(private val smoothing: Float) {
    private var smoothSin = 0f
    private var smoothCos = 0f
    private var started = false

    /** Takes a reading in radians and returns the heading in degrees, 0 to 360. */
    fun add(radians: Float): Float {
        if (started) {
            smoothSin += (sin(radians) - smoothSin) * smoothing
            smoothCos += (cos(radians) - smoothCos) * smoothing
        } else {
            smoothSin = sin(radians)
            smoothCos = cos(radians)
            started = true
        }

        var degrees = Math.toDegrees(atan2(smoothSin, smoothCos).toDouble()).toFloat()

        if (degrees < 0f) degrees += 360f

        return ((degrees * 10f).roundToInt() / 10f) % 360f
    }
}
