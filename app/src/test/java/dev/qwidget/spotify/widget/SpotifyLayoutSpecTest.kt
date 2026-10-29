package dev.qwidget.spotify.widget

import org.junit.Assert.*
import org.junit.Test

class SpotifyLayoutSpecTest {
    @Test fun smallWidgetUsesCompactPlayer() {
        assertTrue(SpotifyLayoutSpec.isCompact(160f, 120f))
    }
    @Test fun shortOrNarrowWidgetDoesNotUseTallCoverLayout() {
        assertTrue(SpotifyLayoutSpec.isCompact(400f, 120f))
        assertTrue(SpotifyLayoutSpec.isCompact(180f, 400f))
    }
    @Test fun originalAndExpandedSizesKeepCoverLayout() {
        assertFalse(SpotifyLayoutSpec.isCompact(250f, 190f))
        assertFalse(SpotifyLayoutSpec.isCompact(250f, 280f))
        assertFalse(SpotifyLayoutSpec.isCompact(500f, 500f))
    }
}
