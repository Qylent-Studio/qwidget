package dev.qwidget.probe

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import dev.qwidget.spotify.widget.WidgetUpdates
import dev.qwidget.automation.skip.SkipAutomation

/** Access is used only for media sessions. Notification bodies are never read. */
class SpotifyListener : NotificationListenerService() {
    private var controller: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private val sessionsChanged = android.media.session.MediaSessionManager.OnActiveSessionsChangedListener { attach() }
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() = attach()
    }

    override fun onListenerConnected() {
        ProbeState.listenerConnected = true
        try {
            SpotifyAccess.manager(this).addOnActiveSessionsChangedListener(
                sessionsChanged, SpotifyAccess.component(this), handler)
            attach()
        } catch (_: SecurityException) {
            ProbeState.status = "Medya oturumu izni kullanılamıyor."
        }
    }

    private fun attach() {
        controller?.unregisterCallback(callback)
        controller = try { SpotifyAccess.find(this) } catch (_: SecurityException) { null }
        controller?.registerCallback(callback, handler)
        refresh()
    }

    private fun refresh() {
        val current = controller
        if (current == null) ProbeState.clear()
        else ProbeState.update(current.metadata, current.playbackState)
        WidgetUpdates.request(this)
        SkipAutomation.observe(this)
    }

    private fun release() {
        SpotifyAccess.manager(this).removeOnActiveSessionsChangedListener(sessionsChanged)
        controller?.unregisterCallback(callback)
        controller = null
        ProbeState.listenerConnected = false
        ProbeState.clear()
        WidgetUpdates.requestDurable(this)
    }

    override fun onListenerDisconnected() { release(); super.onListenerDisconnected() }
    override fun onDestroy() { release(); super.onDestroy() }
}
