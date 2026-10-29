package dev.qwidget.spotify.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetPresentationTest {
    @Test fun `revoked access never exposes a stale song or active controls`() {
        val view = WidgetPresentation.from(false, true, "Old title", "Artist", true, setOf("pause", "next"))
        assertEquals("Bildirim erişimi gerekli", view.title)
        assertTrue(view.commands.isEmpty())
        assertEquals("İzin vermek için ayarlara dokun", view.subtitle)
    }

    @Test fun `missing session offers recovery instead of pretending to play`() {
        val view = WidgetPresentation.from(true, false, null, null, false, setOf("play"))
        assertEquals("Spotify'ı aç", view.title)
        assertEquals(setOf("play"), view.commands)
    }

    @Test fun `playing session exposes pause and only supported commands`() {
        val view = WidgetPresentation.from(true, true, "Track", "Artist", true, setOf("play", "pause", "next"))
        assertEquals("Track", view.title)
        assertEquals("Artist", view.subtitle)
        assertEquals("pause", view.toggleCommand)
        assertEquals(setOf("pause", "next"), view.commands)
    }

    @Test fun `paused session exposes play and absent metadata has usable fallback`() {
        val view = WidgetPresentation.from(true, true, " ", null, false, setOf("play", "previous"))
        assertEquals("Şarkı bilgisi yok", view.title)
        assertEquals("Spotify", view.subtitle)
        assertEquals("play", view.toggleCommand)
        assertEquals(setOf("play", "previous"), view.commands)
    }

    @Test fun `unsupported toggle is not enabled`() {
        val view = WidgetPresentation.from(true, true, "Song", null, true, setOf("next"))
        assertFalse(view.commands.contains(view.toggleCommand))
    }

    @Test fun `initializing or stale nonplaying session still offers start recovery`() {
        val view = WidgetPresentation.from(true, true, null, null, false, emptySet())
        assertEquals(setOf("play"), view.commands)
    }
}
