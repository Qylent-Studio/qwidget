package dev.qwidget.spotify.widget

object SpotifyLayoutSpec {
    fun isCompact(widthDp: Float, heightDp: Float): Boolean = widthDp < 250f || heightDp < 190f
}
