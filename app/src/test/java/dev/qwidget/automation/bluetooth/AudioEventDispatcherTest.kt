package dev.qwidget.automation.bluetooth

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AudioEventDispatcherTest {
    @Test fun `disconnect work finishes before a following reconnect can evaluate the gate`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val gate = AudioConnectionGate(setOf("a2dp"))
            val release = CompletableDeferred<Unit>()
            val result = CompletableDeferred<Boolean>()
            val dispatcher = AudioEventDispatcher(scope)
            dispatcher.submit {
                release.await()
                gate.update("a2dp", "headset", "headset", false, true)
            }
            dispatcher.submit { result.complete(gate.update("a2dp", "headset", "headset", true, true)) }
            assertFalse("Reconnect must not run while disconnect is suspended", result.isCompleted)
            release.complete(Unit)
            assertTrue(withTimeout(1000) { result.await() })
        } finally { scope.cancel() }
    }
}
