package dev.qwidget.probe

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState

/** Volatile probe snapshot only: no listening history or notification text. */
object ProbeState {
    var listenerConnected = false
    var title = "Spotify oturumu yok"
    var artist = ""
    var trackId = ""
    var cover: Bitmap? = null
    var playing = false
    var changes = 0
    var lastChange = "—"
    var status = "Hazır; önce bildirim erişimi verin ve Spotify'da müzik açın."
    private var previousTrack: String? = null

    fun update(metadata: MediaMetadata?, playback: PlaybackState?) {
        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Şarkı bilgisi yok"
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        trackId = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID).orEmpty()
        cover = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        playing = playback?.state == PlaybackState.STATE_PLAYING
        // A diagnostic counter, not an identity suitable for permanent skip rules.
        val key = metadata?.let { trackId.ifBlank { "$title\u0000$artist" } }
        if (key != null && key != previousTrack) {
            changes++
            lastChange = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())
        }
        previousTrack = key
    }

    fun clear() {
        update(null, null)
        title = "Spotify oturumu yok"
    }
}
