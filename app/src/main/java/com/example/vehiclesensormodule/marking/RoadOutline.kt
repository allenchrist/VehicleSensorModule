package com.example.vehiclesensormodule.marking

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * Turns the GPS track of a drive through a narrow road into the shape the
 * server stores: an outline a few metres either side of the track, as
 * [latitude, longitude] points, closed.
 */
object RoadOutline {

    // Half the width of the outline. Close to a narrow lane, and enough to
    // cover GPS wandering a little to either side.
    const val HALF_WIDTH_M = 3.0

    // A shorter track isn't a road
    const val MIN_LENGTH_M = 15.0

    // The server takes up to 500 outline points, two for each track point
    private const val MAX_TRACK_POINTS = 120

    private const val METRES_PER_DEGREE = 111_320.0

    fun metresBetween(a: GeoPoint, b: GeoPoint): Double {
        val north = (b.latitude - a.latitude) * METRES_PER_DEGREE
        val east = (b.longitude - a.longitude) * METRES_PER_DEGREE * cos(a.latitude * PI / 180)

        return hypot(north, east)
    }

    fun lengthMetres(track: List<GeoPoint>): Double = track.zipWithNext { a, b -> metresBetween(a, b) }.sum()

    /** The outline, or null if the track is too short to be a road. */
    fun build(track: List<GeoPoint>, halfWidthM: Double = HALF_WIDTH_M): List<List<Double>>? {
        if (track.size < 2 || lengthMetres(track) < MIN_LENGTH_M) {
            return null
        }

        val origin = track.first()
        val scaleEast = METRES_PER_DEGREE * cos(origin.latitude * PI / 180)

        fun toMetres(point: GeoPoint) = Pair(
            (point.longitude - origin.longitude) * scaleEast,
            (point.latitude - origin.latitude) * METRES_PER_DEGREE
        )

        fun toPoint(x: Double, y: Double) = listOf(
            origin.latitude + y / METRES_PER_DEGREE,
            origin.longitude + x / scaleEast
        )

        // Fewer points, keeping the bends, until it fits what the server takes
        var tolerance = 1.0
        var line = simplify(track.map(::toMetres), tolerance)

        while (line.size > MAX_TRACK_POINTS) {
            tolerance *= 2
            line = simplify(track.map(::toMetres), tolerance)
        }

        val left = mutableListOf<List<Double>>()
        val right = mutableListOf<List<Double>>()

        line.forEachIndexed { i, (x, y) ->
            val (bx, by) = line[max(0, i - 1)]
            val (ax, ay) = line[min(line.size - 1, i + 1)]
            val length = hypot(ax - bx, ay - by).takeIf { it > 0 } ?: 1.0

            // To the left of the direction of travel
            val nx = -(ay - by) / length
            val ny = (ax - bx) / length

            left += toPoint(x + nx * halfWidthM, y + ny * halfWidthM)
            right += toPoint(x - nx * halfWidthM, y - ny * halfWidthM)
        }

        val ring = left + right.reversed()

        return ring + listOf(ring.first())
    }

    // Douglas-Peucker: drops points that change the line by less than the tolerance
    private fun simplify(points: List<Pair<Double, Double>>, tolerance: Double): List<Pair<Double, Double>> {
        if (points.size < 3) {
            return points
        }

        val (sx, sy) = points.first()
        val (ex, ey) = points.last()
        val length = hypot(ex - sx, ey - sy).takeIf { it > 0 } ?: 1.0

        var farthest = 0.0
        var index = 0

        for (i in 1 until points.size - 1) {
            val (px, py) = points[i]
            val offLine = abs((ex - sx) * (sy - py) - (sx - px) * (ey - sy)) / length

            if (offLine > farthest) {
                farthest = offLine
                index = i
            }
        }

        if (farthest <= tolerance) {
            return listOf(points.first(), points.last())
        }

        return simplify(points.subList(0, index + 1), tolerance).dropLast(1) +
            simplify(points.subList(index, points.size), tolerance)
    }
}
