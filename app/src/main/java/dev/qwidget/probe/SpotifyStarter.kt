package dev.qwidget.probe

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState
import android.view.KeyEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SpotifyStarter {
    private const val PACKAGE = "com.spotify.music"
    private val mutex = Mutex()

    suspend fun resume(context: Context, canResume: () -> Boolean = { true }): Boolean = mutex.withLock {
        try {
            val result = SpotifyStartEngine(
                session = {
                    val controller = SpotifyAccess.find(context)
                    when {
                        controller == null -> StartSession.MISSING
                        controller.playbackState?.state == PlaybackState.STATE_PLAYING -> StartSession.PLAYING
                        ((controller.playbackState?.actions ?: 0L) and PlaybackState.ACTION_PLAY) != 0L -> StartSession.READY
                        else -> StartSession.WAITING
                    }
                },
                start = { wake(context) },
                play = { SpotifyAccess.command(context, "play") },
                waitForSession = { delay(500) },
                canResume = canResume,
            ).resume()
            ProbeState.status = when (result) {
                StartResult.ALREADY_PLAYING -> "Spotify zaten çalıyor."
                StartResult.COMMAND_SENT -> "Spotify'a devam ettirme komutu gönderildi."
                StartResult.UNAVAILABLE -> "Spotify başlatılamadı veya çalmaya hazır değil. Spotify'ı açın; Android arka plan başlatmasını engellemiş olabilir."
            }
            result != StartResult.UNAVAILABLE
        } catch (_: SecurityException) {
            ProbeState.status = "Bildirim erişimini ve Spotify izinlerini kontrol edin."
            false
        } catch (_: IllegalStateException) {
            ProbeState.status = "Spotify oturumu kullanılamıyor."
            false
        }
    }

    private fun wake(context: Context): Boolean {
        if (context !is Activity) {
            // Target Spotify only: a global media key could start another music app.
            val mediaIntent = Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(PACKAGE)
            @Suppress("DEPRECATION")
            val receiver = context.packageManager.queryBroadcastReceivers(mediaIntent, 0)
                .firstOrNull { it.activityInfo?.exported == true && it.activityInfo?.enabled == true }
                ?.activityInfo
            if (receiver != null) {
                val target = ComponentName(receiver.packageName, receiver.name)
                for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
                    context.sendBroadcast(Intent(mediaIntent).setComponent(target)
                        .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(action, KeyEvent.KEYCODE_MEDIA_PLAY)))
                }
                return true
            }
        }
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true // Background launch can be silently denied; session polling detects that.
        } catch (_: android.content.ActivityNotFoundException) {
            false
        }
    }
}
