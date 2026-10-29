package dev.qwidget.battery

/** Launcher option sizes are dp; unknown dimensions must not select oversized text. */
data class BatteryLayoutSpec(
    val paddingDp: Int,
    val titleSp: Float,
    val bodySp: Float,
    val progressHeightDp: Int,
    val verticalPaddingDp: Int = paddingDp,
    val showStatus: Boolean = true,
    val showProgress: Boolean = true,
) {
    companion object {
        fun fromSize(width: Int, height: Int, fontScale: Float = 1f): BatteryLayoutSpec {
            val base = when {
                width < 140 || height < 96 -> BatteryLayoutSpec(6, 18f, 10f, 2)
                width >= 280 && height >= 180 -> BatteryLayoutSpec(16, 40f, 12f, 4)
                width >= 200 && height >= 140 -> BatteryLayoutSpec(16, 36f, 12f, 4)
                else -> BatteryLayoutSpec(16, 28f, 12f, 4)
            }
            val scale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
            val safeWidth = if (width > 0) width else 70
            val safeHeight = if (height > 0) height else 48
            val verticalPadding = if (base.paddingDp == 6) 4 else base.paddingDp
            val availableWidth = (safeWidth - base.paddingDp * 2).coerceAtLeast(1)
            val availableHeight = (safeHeight - verticalPadding * 2).coerceAtLeast(1)
            // Reserve up to 3 em for %100 and 1.25 em per line. At tiny sizes,
            // prioritize a complete percentage; the root retains the full spoken status.
            val title = minOf(base.titleSp, availableWidth / (3f * scale), availableHeight / (1.25f * scale))
            val titleHeight = title * scale * 1.25f
            val statusHeight = base.bodySp * scale * 1.25f
            val status = titleHeight + statusHeight <= availableHeight
            val progress = titleHeight + (if (status) statusHeight else 0f) + base.progressHeightDp <= availableHeight
            return base.copy(titleSp = title, verticalPaddingDp = verticalPadding, showStatus = status, showProgress = progress)
        }
    }
}
