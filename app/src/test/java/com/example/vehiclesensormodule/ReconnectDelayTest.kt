package com.example.vehiclesensormodule

import com.example.vehiclesensormodule.network.reconnectDelayMs
import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectDelayTest {
    @Test
    fun theWaitDoublesAfterEachFailedAttempt() {
        assertEquals(1_000L, reconnectDelayMs(0, maxMs = 10_000))
        assertEquals(2_000L, reconnectDelayMs(1, maxMs = 10_000))
        assertEquals(4_000L, reconnectDelayMs(2, maxMs = 10_000))
        assertEquals(8_000L, reconnectDelayMs(3, maxMs = 10_000))
    }

    @Test
    fun theWaitStopsGrowingAtTheLongest() {
        assertEquals(10_000L, reconnectDelayMs(4, maxMs = 10_000))
        assertEquals(10_000L, reconnectDelayMs(50, maxMs = 10_000))
    }

    @Test
    fun manyAttemptsDontOverflow() {
        assertEquals(10_000L, reconnectDelayMs(Int.MAX_VALUE, maxMs = 10_000))
    }
}
