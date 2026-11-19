package com.example.vehiclesensormodule

import com.example.vehiclesensormodule.network.UpdateCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckTest {

    @Test
    fun readsTheVersionFromAReleaseTag() {
        assertEquals(12, UpdateCheck.versionFromTag("v12"))
        assertEquals(3, UpdateCheck.versionFromTag(" v3 "))
        assertEquals(7, UpdateCheck.versionFromTag("7"))
    }

    @Test
    fun ignoresTagsThatArentAVersion() {
        assertNull(UpdateCheck.versionFromTag(""))
        assertNull(UpdateCheck.versionFromTag("latest"))
        assertNull(UpdateCheck.versionFromTag("v1.2"))
    }
}
