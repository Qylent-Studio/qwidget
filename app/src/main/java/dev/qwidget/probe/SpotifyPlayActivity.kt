package dev.qwidget.probe

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import dev.qwidget.spotify.widget.WidgetUpdates
import kotlinx.coroutines.*

/** Widget play uses an Activity PendingIntent, not a background activity trampoline. */
class SpotifyPlayActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope.launch {
            try {
                if (!SpotifyStarter.resume(this@SpotifyPlayActivity)) {
                    Toast.makeText(this@SpotifyPlayActivity, ProbeState.status, Toast.LENGTH_LONG).show()
                }
                WidgetUpdates.refresh(this@SpotifyPlayActivity)
            } finally {
                finish()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
