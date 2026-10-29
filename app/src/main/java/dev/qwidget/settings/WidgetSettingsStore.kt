package dev.qwidget.settings

import android.content.Context
import dev.qwidget.core.theme.WidgetPalette

/** Common per-widget preferences. No song data or cover art is stored. */
class WidgetSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
    fun palette(widgetId: Int): WidgetPalette = WidgetPalette.find(prefs.getString("theme_$widgetId", null))
    fun savePalette(widgetId: Int, id: String) { prefs.edit().putString("theme_$widgetId", WidgetPalette.find(id).id).apply() }
    fun remove(widgetId: Int) { prefs.edit().remove("theme_$widgetId").apply() }
}
