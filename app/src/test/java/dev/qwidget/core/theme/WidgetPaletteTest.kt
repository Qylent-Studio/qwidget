package dev.qwidget.core.theme

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class WidgetPaletteTest {
    @Test fun `unknown persisted theme safely falls back`() {
        assertEquals("graphite", WidgetPalette.find("deleted-theme").id)
        assertEquals("graphite", WidgetPalette.find(null).id)
    }

    @Test fun `saved palette id round trips without changing selection`() {
        assertEquals("ocean", WidgetPalette.find("ocean").id)
    }

    @Test fun `every text and playback accent pair meets AA contrast`() {
        assertTrue(WidgetPalette.all.size >= 4)
        WidgetPalette.all.forEach { palette ->
            assertTrue("${palette.id} body", contrast(palette.text, palette.surface) >= 4.5)
            assertTrue("${palette.id} secondary", contrast(palette.secondary, palette.surface) >= 4.5)
            assertTrue("${palette.id} control", contrast(palette.onAccent, palette.accent) >= 4.5)
        }
    }

    private fun contrast(a: Long, b: Long): Double {
        fun luminance(color: Long): Double {
            val rgb = listOf(16, 8, 0).map { shift ->
                val value = ((color shr shift) and 255).toDouble() / 255
                if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }
            return rgb[0] * 0.2126 + rgb[1] * 0.7152 + rgb[2] * 0.0722
        }
        val x = luminance(a); val y = luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
}
