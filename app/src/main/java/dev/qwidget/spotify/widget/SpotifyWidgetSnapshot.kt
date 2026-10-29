package dev.qwidget.spotify.widget

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState
import dev.qwidget.probe.SpotifyAccess

data class SpotifyWidgetSnapshot(val presentation: WidgetPresentation, val cover: Bitmap?) {
    companion object {
        fun read(context: Context): SpotifyWidgetSnapshot {
            val allowed = context.getSystemService(android.app.NotificationManager::class.java)
                .isNotificationListenerAccessGranted(SpotifyAccess.component(context))
            if (!allowed) return empty(false)
            return try {
                val controller = SpotifyAccess.find(context) ?: return empty(true)
                val metadata = controller.metadata
                val playback = controller.playbackState
                val supported = buildSet {
                    val actions = playback?.actions ?: 0L
                    if (actions and PlaybackState.ACTION_PLAY != 0L) add("play")
                    if (actions and PlaybackState.ACTION_PAUSE != 0L) add("pause")
                    if (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) add("previous")
                    if (actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) add("next")
                }
                val cover = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                // Keep binder payload bounded, even when Spotify provides a large original.
                val scaledCover = cover?.let {
                    val ratio = minOf(1f, 420f / maxOf(it.width, it.height))
                    if (ratio < 1f) Bitmap.createScaledBitmap(it,
                        maxOf(1, (it.width * ratio).toInt()), maxOf(1, (it.height * ratio).toInt()), true)
                    else it
                }
                SpotifyWidgetSnapshot(WidgetPresentation.from(true, true,
                    metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                    metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                    playback?.state == PlaybackState.STATE_PLAYING, supported), scaledCover)
            } catch (_: SecurityException) { empty(false) }
        }

        private fun empty(access: Boolean) = SpotifyWidgetSnapshot(
            WidgetPresentation.from(access, false, null, null, false, emptySet()), null)
    }
}
