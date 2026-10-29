package dev.qwidget.automation.bluetooth

import org.junit.Assert.*
import org.junit.Test

class AudioConnectionGateTest {
    @Test fun `selected audio connection resumes only once across duplicate profiles`() {
        val gate = AudioConnectionGate()
        assertTrue(gate.update("a2dp", "headset", "headset", true, true))
        assertFalse(gate.update("a2dp", "headset", "headset", true, true))
        assertFalse(gate.update("le", "headset", "headset", true, true))
        assertEquals(setOf("a2dp", "le"), gate.profiles)
    }

    @Test fun `other devices and invalid profiles never start Spotify`() {
        val gate = AudioConnectionGate()
        assertFalse(gate.update("a2dp", "car", "headset", true, true))
        assertFalse(gate.update("a2dp", null, null, true, true))
        assertFalse(gate.update("acl", "headset", "headset", true, true))
        assertTrue(gate.profiles.isEmpty())
    }

    @Test fun `all profiles must disconnect before a reconnect triggers`() {
        val gate = AudioConnectionGate(setOf("a2dp", "le"))
        assertFalse(gate.update("a2dp", "headset", "headset", false, true))
        assertFalse(gate.update("a2dp", "headset", "headset", true, true))
        gate.update("a2dp", "headset", "headset", false, true)
        gate.update("le", "headset", "headset", false, true)
        assertTrue(gate.update("a2dp", "headset", "headset", true, true))
    }

    @Test fun `already known connected profiles suppress duplicate events`() {
        val gate = AudioConnectionGate(setOf("a2dp"))
        assertFalse(gate.update("a2dp", "headset", "headset", true, true))
        gate.update("a2dp", "headset", "headset", false, true)
        assertTrue(gate.update("a2dp", "headset", "headset", true, true))
    }

    @Test fun `enabling while already connected does not start music until reconnect`() {
        val gate = AudioConnectionGate()
        assertFalse(gate.update("a2dp", "headset", "headset", true, false))
        assertFalse(gate.update("a2dp", "headset", "headset", true, true))
        gate.update("a2dp", "headset", "headset", false, true)
        assertTrue(gate.update("a2dp", "headset", "headset", true, true))
    }
}
