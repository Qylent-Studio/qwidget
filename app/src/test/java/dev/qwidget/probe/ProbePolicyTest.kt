package dev.qwidget.probe

import org.junit.Assert.*
import org.junit.Test

class ProbePolicyTest {
    @Test fun `only Spotify sessions are accepted`() {
        assertTrue(ProbePolicy.acceptsPackage("com.spotify.music"))
        assertFalse(ProbePolicy.acceptsPackage("com.google.android.apps.youtube.music"))
        assertFalse(ProbePolicy.acceptsPackage(null))
    }

    @Test fun `selected bluetooth connection triggers once until disconnected`() {
        val gate = ConnectionGate()
        assertTrue(gate.connected("headphones", "headphones"))
        assertFalse(gate.connected("headphones", "headphones"))
        gate.disconnected("headphones")
        assertTrue(gate.connected("headphones", "headphones"))
    }

    @Test fun `unselected and missing devices never trigger`() {
        val gate = ConnectionGate()
        assertFalse(gate.connected("car", "headphones"))
        assertFalse(gate.connected("car", null))
        assertFalse(gate.connected(null, null))
        assertTrue(gate.connected("headphones", "headphones"))
    }
}
