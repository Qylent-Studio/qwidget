package dev.qwidget.core.theme

data class WidgetGlassStyle(val surface: Long, val text: Long, val secondary: Long) {
    companion object {
        // Only the surface is translucent. Opaque text keeps contrast against the
        // composited wallpaper; a restrained highlight is supplied by the drawable.
        fun from(palette: WidgetPalette) = WidgetGlassStyle(
            (palette.surface and 0x00FFFFFFL) or 0xB8000000L, palette.text, palette.text)
    }
}
