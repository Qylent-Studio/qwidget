package dev.qwidget.core.theme

data class WidgetPalette(val id: String, val label: String, val surface: Long, val text: Long, val secondary: Long, val accent: Long, val onAccent: Long) {
    companion object {
        val all = listOf(
            WidgetPalette("graphite", "Grafit", 0xFF171A1CL, 0xFFF5F7F6L, 0xFFADB6B1L, 0xFFB8F39BL, 0xFF172112L),
            WidgetPalette("forest", "Orman", 0xFF14251EL, 0xFFF1F8F3L, 0xFFAFBFB4L, 0xFF9EE6B2L, 0xFF10271AL),
            WidgetPalette("ocean", "Okyanus", 0xFF14232FL, 0xFFF2F7FCL, 0xFFAFBDCCL, 0xFFA6D9F7L, 0xFF102637L),
            WidgetPalette("plum", "Mürdüm", 0xFF291D2BL, 0xFFFAF2FAL, 0xFFC5B3C6L, 0xFFE5BAE9L, 0xFF321636L),
            WidgetPalette("sand", "Kum", 0xFFF0E9DCL, 0xFF252822L, 0xFF555C51L, 0xFF334B37L, 0xFFF3FAEFL),
            WidgetPalette("ruby", "Kırmızı", 0xFF421B25L, 0xFFFFF6F7L, 0xFFE9BEC5L, 0xFFFFA9B3L, 0xFF3C101CL),
        )
        val forBattery = all
        fun find(id: String?): WidgetPalette = forBattery.firstOrNull { it.id == id } ?: all.first()
    }
}
