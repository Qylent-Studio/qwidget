package dev.qwidget.automation.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel

/** Preserves the connection broadcast arrival order, including suspending work. */
class AudioEventDispatcher(private val scope: CoroutineScope) {
    private val events = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init {
        scope.launch {
            for (event in events) runCatching { event() }.onFailure { if (it is CancellationException) throw it }
        }
    }
    fun submit(event: suspend () -> Unit) { check(events.trySend(event).isSuccess) }
}
