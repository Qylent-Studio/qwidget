package dev.qwidget.spotify.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WidgetUpdates {
    val revisionKey = longPreferencesKey("revision")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private val mutex = Mutex()
    private var pending: Runnable? = null

    fun ids(context: Context): IntArray {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(SpotifyCompactReceiver::class.java, SpotifyLargeReceiver::class.java)
            .flatMap { manager.getAppWidgetIds(ComponentName(context, it)).toList() }.toIntArray()
    }

    /** Collapse metadata/playback bursts; no periodic polling or new foreground service. */
    fun request(context: Context) {
        val app = context.applicationContext
        handler.post {
            pending?.let(handler::removeCallbacks)
            pending = Runnable { scope.launch { refresh(app) } }
            handler.postDelayed(pending!!, 150)
        }
    }

    /** Queue durable disconnect cleanup; force-stop can still delay host refresh. */
    fun requestDurable(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "spotify_disconnect_refresh", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<SpotifyWidgetRefreshWorker>().build())
    }

    suspend fun refresh(context: Context) = mutex.withLock {
        ids(context).forEach { widgetId ->
            try {
                val id = GlanceAppWidgetManager(context).getGlanceIdBy(widgetId)
                updateAppWidgetState(context, id) { it[revisionKey] = android.os.SystemClock.elapsedRealtimeNanos() }
                SpotifyWidget().update(context, id)
            } catch (_: IllegalArgumentException) {
                // The host may delete a widget while an update is in flight.
            }
        }
    }
}

class SpotifyWidgetRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        WidgetUpdates.refresh(applicationContext)
        return Result.success()
    }
}
