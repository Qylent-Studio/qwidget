package dev.qwidget.core.theme

import dev.qwidget.core.theme.WidgetPalette
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class WidgetGlassStyleTest {
    @Test fun rubyIsNowAvailableToBothWidgetTypesAndOldSelectionsStillResolve() {
        assertTrue(WidgetPalette.forBattery.any { it.id == "ruby" })
        assertEquals("ruby", WidgetPalette.find("ruby").id)
        assertTrue(WidgetPalette.all.any { it.id == "ruby" })
        assertEquals("ocean", WidgetPalette.find("ocean").id)
    }

    @Test fun everyBatterySurfaceRevealsWallpaperWithoutFadingText() {
        WidgetPalette.forBattery.forEach { palette ->
            val glass = WidgetGlassStyle.from(palette)
            assertTrue("${palette.id} transparency", (glass.surface ushr 24) in 1..254)
            assertEquals(255L, glass.text ushr 24)
            assertEquals(255L, glass.secondary ushr 24)
        }
    }

    @Test fun textRemainsReadableOverBlackAndWhiteWallpaperIncludingTopSheen() {
        WidgetPalette.forBattery.forEach { palette ->
            val glass = WidgetGlassStyle.from(palette)
            listOf(0xFF000000L, 0xFFFFFFFFL).forEach { wallpaper ->
                val surface = composite(glass.surface, wallpaper)
                listOf(surface, composite(0x14FFFFFFL, surface)).forEach { background ->
                    assertTrue("${palette.id} primary", contrast(glass.text, background) >= 4.5)
                    assertTrue("${palette.id} secondary", contrast(glass.secondary, background) >= 4.5)
                }
            }
        }
    }

    private fun composite(foreground: Long, background: Long): Long {
        val alpha = (foreground ushr 24).toDouble() / 255
        return listOf(16, 8, 0).fold(0xFF000000L) { result, shift ->
            val component = (((foreground shr shift) and 255) * alpha + ((background shr shift) and 255) * (1 - alpha)).toLong()
            result or (component shl shift)
        }
    }

    private fun contrast(a: Long, b: Long): Double {
        fun luminance(color: Long): Double {
            val components = listOf(16, 8, 0).map { shift ->
                val value = ((color shr shift) and 255).toDouble() / 255
                if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }
            return components[0] * 0.2126 + components[1] * 0.7152 + components[2] * 0.0722
        }
        val x = luminance(a); val y = luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
}
