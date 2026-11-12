package com.example.vehiclesensormodule

import com.example.vehiclesensormodule.marking.GeoPoint
import com.example.vehiclesensormodule.marking.RoadOutline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadOutlineTest {
    // About 1.1 m east for every 0.00001 degrees here
    private fun eastward(points: Int, stepDegrees: Double = 0.00002) = List(points) {
        GeoPoint(
            11.0,
            77.0 + it * stepDegrees
        )
    }

    @Test
    fun aShortTrackIsNotARoad() {
        assertNull(RoadOutline.build(eastward(5)))
        assertNull(RoadOutline.build(listOf(GeoPoint(11.0, 77.0))))
    }

    @Test
    fun lengthIsMeasuredInMetres() {
        val length = RoadOutline.lengthMetres(eastward(11, stepDegrees = 0.0001))

        // 10 steps of about 10.9 m
        assertTrue("was $length", length > 105 && length < 112)
    }

    @Test
    fun theOutlineIsClosed() {
        val outline = outlineOf(eastward(30))

        assertEquals(outline.first(), outline.last())
    }

    @Test
    fun aStraightTrackNeedsOnlyItsEnds() {
        val outline = outlineOf(eastward(30))

        // Two ends, each with a left and a right point, plus the closing point
        assertEquals(5, outline.size)
    }

    @Test
    fun theOutlineIsAsWideAsSet() {
        val outline = outlineOf(eastward(30), halfWidthM = 3.0)
        val widthDegrees = outline.maxOf { it[0] } - outline.minOf { it[0] }
        val widthMetres = widthDegrees * 111_320

        assertEquals(6.0, widthMetres, 0.1)
    }

    @Test
    fun aLongWindingTrackStaysWithinTheServerLimit() {
        val winding = List(2000) { GeoPoint(11.0 + 0.0002 * kotlin.math.sin(it / 5.0), 77.0 + it * 0.00002) }
        val outline = outlineOf(winding)

        assertTrue("had ${outline.size} points", outline.size <= 500)
    }

    private fun outlineOf(track: List<GeoPoint>, halfWidthM: Double = RoadOutline.HALF_WIDTH_M) =
        requireNotNull(RoadOutline.build(track, halfWidthM)) { "expected an outline" }
}
