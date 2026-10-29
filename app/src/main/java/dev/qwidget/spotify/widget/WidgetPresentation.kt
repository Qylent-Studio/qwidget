package dev.qwidget.spotify.widget

data class WidgetPresentation(val title: String, val subtitle: String, val toggleCommand: String, val commands: Set<String>) {
    companion object {
        fun from(access: Boolean, session: Boolean, title: String?, artist: String?, playing: Boolean, supported: Set<String>): WidgetPresentation {
            val toggle = if (playing) "pause" else "play"
            if (!access) return WidgetPresentation("Bildirim erişimi gerekli", "İzin vermek için ayarlara dokun", toggle, emptySet())
            if (!session) return WidgetPresentation("Spotify'ı aç", "Müziğe kaldığın yerden devam et", "play", setOf("play"))
            return WidgetPresentation(
                title?.takeIf { it.isNotBlank() } ?: "Şarkı bilgisi yok",
                artist?.takeIf { it.isNotBlank() } ?: "Spotify",
                toggle, supported.intersect(setOf(toggle, "previous", "next")) + if (!playing) setOf("play") else emptySet(),
            )
        }
    }
}
