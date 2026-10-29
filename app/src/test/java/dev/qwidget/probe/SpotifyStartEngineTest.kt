package dev.qwidget.probe

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SpotifyStartEngineTest {
    private class Device(var state: StartSession = StartSession.MISSING) {
        val events = mutableListOf<String>()
        var opens = true
        var sends = true
        var waits = 0
        var onWait: () -> Unit = {}
        var allowed = true
        fun engine() = SpotifyStartEngine(
            session = { state },
            start = { events += "start"; opens },
            play = { events += "play"; sends },
            waitForSession = { waits++; onWait() },
            canResume = { allowed },
        )
    }

    @Test fun `closed Spotify starts once then plays when ready`() = runBlocking {
        val device = Device()
        device.onWait = { device.state = StartSession.READY }
        assertEquals(StartResult.COMMAND_SENT, device.engine().resume())
        assertEquals(listOf("start", "play"), device.events)
    }

    @Test fun `already playing never starts or sends another play`() = runBlocking {
        val device = Device(StartSession.PLAYING)
        assertEquals(StartResult.ALREADY_PLAYING, device.engine().resume())
        assertTrue(device.events.isEmpty())
    }

    @Test fun `paused session plays without launching Spotify`() = runBlocking {
        val device = Device(StartSession.READY)
        assertEquals(StartResult.COMMAND_SENT, device.engine().resume())
        assertEquals(listOf("play"), device.events)
    }

    @Test fun `startup that auto plays never receives a redundant command`() = runBlocking {
        val device = Device()
        device.onWait = { device.state = StartSession.PLAYING }
        assertEquals(StartResult.ALREADY_PLAYING, device.engine().resume())
        assertEquals(listOf("start"), device.events)
    }

    @Test fun `missing session times out with no repeated launch or blind play`() = runBlocking {
        val device = Device()
        assertEquals(StartResult.UNAVAILABLE, device.engine().resume())
        assertEquals(listOf("start"), device.events)
        assertEquals(16, device.waits)
    }

    @Test fun `blocked launch stops immediately`() = runBlocking {
        val device = Device().apply { opens = false }
        assertEquals(StartResult.UNAVAILABLE, device.engine().resume())
        assertEquals(0, device.waits)
        assertEquals(listOf("start"), device.events)
    }

    @Test fun `unsupported session is not forced or relaunched`() = runBlocking {
        val device = Device(StartSession.WAITING)
        assertEquals(StartResult.UNAVAILABLE, device.engine().resume())
        assertTrue(device.events.isEmpty())
    }

    @Test fun `existing initializing session is allowed to become ready without relaunch`() = runBlocking {
        val device = Device(StartSession.WAITING)
        device.onWait = { device.state = StartSession.READY }
        assertEquals(StartResult.COMMAND_SENT, device.engine().resume())
        assertEquals(listOf("play"), device.events)
    }

    @Test fun `disconnect during startup prevents playing through phone speaker`() = runBlocking {
        val device = Device()
        device.onWait = { device.allowed = false; device.state = StartSession.READY }
        assertEquals(StartResult.UNAVAILABLE, device.engine().resume())
        assertEquals(listOf("start"), device.events)
        assertEquals(1, device.waits)
    }

    @Test fun `cancelled request cannot launch Spotify`() = runBlocking {
        val device = Device().apply { allowed = false }
        assertEquals(StartResult.UNAVAILABLE, device.engine().resume())
        assertTrue(device.events.isEmpty())
    }

    @Test fun `failed command is not retried`() = runBlocking {
        val device = Device(StartSession.READY).apply { sends = false }
        assertEquals(StartResult.UNAVAILABLE, device.engine().resume())
        assertEquals(listOf("play"), device.events)
    }

    @Test fun `new session can become ready after appearing without actions`() = runBlocking {
        val device = Device()
        device.onWait = { device.state = if (device.waits == 1) StartSession.WAITING else StartSession.READY }
        assertEquals(StartResult.COMMAND_SENT, device.engine().resume())
        assertEquals(listOf("start", "play"), device.events)
        assertEquals(2, device.waits)
    }

    @Test fun `cold Spotify taking six seconds can still start playback`() = runBlocking {
        val device = Device()
        device.onWait = { if (device.waits == 12) device.state = StartSession.READY }
        assertEquals(StartResult.COMMAND_SENT, device.engine().resume())
        assertEquals(listOf("start", "play"), device.events)
    }
}
