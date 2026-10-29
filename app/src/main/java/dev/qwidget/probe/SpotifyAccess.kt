package dev.qwidget.probe

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import dev.qwidget.automation.skip.AutomaticSkipTarget

object SpotifyAccess {
    fun manager(context: Context): MediaSessionManager =
        context.getSystemService(MediaSessionManager::class.java)

    fun component(context: Context) = ComponentName(context, SpotifyListener::class.java)

    fun find(context: Context): MediaController? = manager(context)
        .getActiveSessions(component(context))
        .filter { ProbePolicy.acceptsPackage(it.packageName) }
        .let { sessions -> sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull() }

    fun command(context: Context, action: String, expectedTrackId: String? = null, expectedSession: String? = null): Boolean {
        try {
            val controller = find(context)
            if (controller == null) {
                ProbeState.status = "Spotify oturumu yok. Spotify'ı açıp bir şarkı çalın; arka plandan soğuk başlatma bu probe'da yok."
                return false
            }
            if (expectedTrackId != null && controller.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_MEDIA_ID) != expectedTrackId) {
                ProbeState.status = "Şarkı değişti; yanlış şarkıya komut gönderilmedi."
                return false
            }
            if (expectedSession != null && (expectedTrackId == null || !AutomaticSkipTarget.matches(
                    expectedTrackId, expectedSession, controller.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_MEDIA_ID),
                    controller.sessionToken.hashCode().toString(), controller.playbackState?.state == PlaybackState.STATE_PLAYING))) {
                ProbeState.status = "Oturum veya çalma durumu değişti; otomatik komut gönderilmedi."
                return false
            }
            val required = when (action) {
                "play" -> PlaybackState.ACTION_PLAY
                "pause" -> PlaybackState.ACTION_PAUSE
                "next" -> PlaybackState.ACTION_SKIP_TO_NEXT
                "previous" -> PlaybackState.ACTION_SKIP_TO_PREVIOUS
                else -> return false
            }
            if (((controller.playbackState?.actions ?: 0L) and required) == 0L) {
                ProbeState.status = "Spotify bu komutu şu anda desteklemiyor: $action"
                return false
            }
            with(controller.transportControls) {
                when (action) {
                    "play" -> play()
                    "pause" -> pause()
                    "next" -> skipToNext()
                    "previous" -> skipToPrevious()
                }
            }
            ProbeState.status = "Komut gönderildi: $action. Sonucu ses ve oturum durumundan doğrulayın."
            return true
        } catch (_: SecurityException) {
            ProbeState.status = "Bildirim erişimi eksik veya kaldırılmış."
        } catch (_: IllegalStateException) {
            ProbeState.status = "Medya oturumu kullanılamıyor; Spotify'ı yeniden açın."
        }
        return false
    }
}
