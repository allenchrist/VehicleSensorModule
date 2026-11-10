package com.example.vehiclesensormodule

import com.example.vehiclesensormodule.sensors.HeadingSmoother
import com.example.vehiclesensormodule.sensors.angleBetween
import com.example.vehiclesensormodule.sensors.compassPoint
import com.example.vehiclesensormodule.sensors.shouldSendHeading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadingTest {
    @Test
    fun eachDirectionHasItsName() {
        assertEquals("North", compassPoint(0f))
        assertEquals("North-East", compassPoint(45f))
        assertEquals("East", compassPoint(90f))
        assertEquals("South-East", compassPoint(135f))
        assertEquals("South", compassPoint(180f))
        assertEquals("South-West", compassPoint(225f))
        assertEquals("West", compassPoint(270f))
        assertEquals("North-West", compassPoint(315f))
    }

    @Test
    fun aHeadingTakesTheNearestName() {
        assertEquals("North", compassPoint(22f))
        assertEquals("North-East", compassPoint(23f))
        assertEquals("North", compassPoint(359f))
    }

    @Test
    fun headingsOutsideZeroTo360StillHaveNames() {
        assertEquals("North", compassPoint(360f))
        assertEquals("West", compassPoint(-90f))
        assertEquals("East", compassPoint(450f))
    }

    @Test
    fun anglesAreMeasuredTheShortWayRound() {
        assertEquals(20f, angleBetween(350f, 10f), 0.001f)
        assertEquals(20f, angleBetween(10f, 350f), 0.001f)
        assertEquals(180f, angleBetween(0f, 180f), 0.001f)
        assertEquals(0f, angleBetween(90f, 90f), 0.001f)
    }

    @Test
    fun aRealTurnIsSent() {
        assertTrue(shouldSendHeading(100f, 90f, 500, minDegrees = 3f, minIntervalMs = 150))
    }

    @Test
    fun aTinyChangeIsNotSent() {
        assertFalse(shouldSendHeading(91f, 90f, 500, minDegrees = 3f, minIntervalMs = 150))
    }

    @Test
    fun turnsAreNotSentTooOften() {
        assertFalse(shouldSendHeading(120f, 90f, 100, minDegrees = 3f, minIntervalMs = 150))
    }

    @Test
    fun aTurnPastNorthCountsAsSmall() {
        assertFalse(shouldSendHeading(1f, 359f, 500, minDegrees = 3f, minIntervalMs = 150))
    }

    @Test
    fun theFirstReadingIsUsedAsItIs() {
        val smoother = HeadingSmoother(0.5f)

        assertEquals(90f, smoother.add(Math.toRadians(90.0).toFloat()), 0.1f)
    }

    @Test
    fun smoothingMovesPartWayToTheNewReading() {
        val smoother = HeadingSmoother(0.5f)
        smoother.add(Math.toRadians(0.0).toFloat())

        val heading = smoother.add(Math.toRadians(90.0).toFloat())

        assertTrue("was $heading", heading > 30f && heading < 60f)
    }

    @Test
    fun smoothingAcrossNorthStaysNearNorth() {
        val smoother = HeadingSmoother(0.5f)
        smoother.add(Math.toRadians(350.0).toFloat())

        val heading = smoother.add(Math.toRadians(10.0).toFloat())

        // Averaging the numbers would give 180, due south
        assertTrue("was $heading", heading < 5f || heading > 355f)
    }
}
