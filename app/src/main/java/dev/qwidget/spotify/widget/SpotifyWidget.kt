package dev.qwidget.spotify.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import dev.qwidget.R
import dev.qwidget.core.theme.WidgetPalette
import dev.qwidget.core.theme.WidgetGlassStyle
import dev.qwidget.probe.SpotifyAccess
import dev.qwidget.settings.WidgetSettingsActivity
import dev.qwidget.automation.AutomationActivity
import dev.qwidget.settings.WidgetSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpotifyWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(160.dp, 120.dp), DpSize(250.dp, 190.dp), DpSize(250.dp, 280.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val revision = currentState<Preferences>()[WidgetUpdates.revisionKey] ?: 0L
            val snapshot by produceState<SpotifyWidgetSnapshot?>(null, revision) {
                value = withContext(Dispatchers.IO) { SpotifyWidgetSnapshot.read(context) }
            }
            val palette = remember(revision) { WidgetSettingsStore(context).palette(widgetId) }
            val settings = actionStartActivity(Intent(context, AutomationActivity::class.java)
                .setData(android.net.Uri.parse("qwidget://spotify-menu/$widgetId"))
                .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            snapshot?.let { Player(it, palette, settings) }
        }
    }

    @Composable
    private fun Player(snapshot: SpotifyWidgetSnapshot, palette: WidgetPalette, settings: Action) {
        val glass = WidgetGlassStyle.from(palette)
        val size = LocalSize.current
        val compact = SpotifyLayoutSpec.isCompact(size.width.value, size.height.value)
        Box(GlanceModifier.fillMaxSize().appWidgetBackground().background(Color(glass.surface)).cornerRadius(24.dp)) {
            Image(ImageProvider(R.drawable.battery_glass_highlight), contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            if (compact) CompactPlayer(snapshot, palette, settings) else TallPlayer(snapshot, palette, settings)
        }
    }

    @Composable
    private fun CompactPlayer(snapshot: SpotifyWidgetSnapshot, palette: WidgetPalette, settings: Action) {
        val view = snapshot.presentation
        val glass = WidgetGlassStyle.from(palette)
        Column(GlanceModifier.fillMaxSize().padding(8.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.size(32.dp).cornerRadius(8.dp).background(Color(palette.accent).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center) {
                    if (snapshot.cover != null) Image(ImageProvider(snapshot.cover), "${view.title} albüm kapağı",
                        modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Image(ImageProvider(R.drawable.ic_music), "Albüm kapağı yok", modifier = GlanceModifier.size(24.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(Color(glass.text))))
                }
                Spacer(GlanceModifier.width(6.dp))
                Column(GlanceModifier.defaultWeight().clickable(settings)) {
                    Text(view.title, maxLines = 1, style = TextStyle(color = ColorProvider(Color(glass.text)), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    Text(view.subtitle, maxLines = 1, style = TextStyle(color = ColorProvider(Color(glass.secondary)), fontSize = 11.sp))
                }
                Menu(palette, settings)
            }
            Spacer(GlanceModifier.defaultWeight())
            Controls(view, palette, compact = true)
        }
    }

    @Composable
    private fun TallPlayer(snapshot: SpotifyWidgetSnapshot, palette: WidgetPalette, settings: Action) {
        val view = snapshot.presentation
        val glass = WidgetGlassStyle.from(palette)
        Column(GlanceModifier.fillMaxSize().padding(16.dp)) {
            Box(GlanceModifier.fillMaxWidth().defaultWeight().cornerRadius(16.dp)
                .background(Color(palette.accent).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                if (snapshot.cover != null) {
                    Image(ImageProvider(snapshot.cover), contentDescription = "${view.title} albüm kapağı",
                        modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Image(ImageProvider(R.drawable.ic_music), contentDescription = "Albüm kapağı yok",
                        modifier = GlanceModifier.size(40.dp), colorFilter = ColorFilter.tint(ColorProvider(Color(palette.secondary))))
                }
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                    Menu(palette, settings)
                }
            }
            Spacer(GlanceModifier.height(10.dp))
            Text(view.title, maxLines = 1, style = TextStyle(color = ColorProvider(Color(glass.text)),
                fontSize = 18.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.fillMaxWidth().clickable(settings))
            Text(view.subtitle, maxLines = 1, style = TextStyle(color = ColorProvider(Color(glass.secondary)), fontSize = 12.sp))
            Spacer(GlanceModifier.height(8.dp))
            Controls(view, palette)
        }
    }

    @Composable
    private fun Menu(palette: WidgetPalette, settings: Action) {
        Box(GlanceModifier.size(48.dp).cornerRadius(24.dp).background(Color(palette.surface).copy(alpha = 0.9f)).clickable(settings),
            contentAlignment = Alignment.Center) {
            Image(ImageProvider(R.drawable.ic_more), "Şarkı menüsü ve widget ayarları",
                modifier = GlanceModifier.size(24.dp), colorFilter = ColorFilter.tint(ColorProvider(Color(palette.text))))
        }
    }

    @Composable
    private fun Controls(view: WidgetPresentation, palette: WidgetPalette, compact: Boolean = false) {
            Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically) {
                Control("previous", "Önceki şarkı", R.drawable.ic_previous, view, palette)
                if (!compact) Spacer(GlanceModifier.width(16.dp))
                Control(view.toggleCommand, if (view.toggleCommand == "pause") "Duraklat" else "Oynat",
                     if (view.toggleCommand == "pause") R.drawable.ic_pause else R.drawable.ic_play, view, palette, true, compact)
                if (!compact) Spacer(GlanceModifier.width(16.dp))
                Control("next", "Sonraki şarkı", R.drawable.ic_next, view, palette)
            }
    }

    @Composable
    private fun Control(command: String, label: String, icon: Int, view: WidgetPresentation, palette: WidgetPalette, primary: Boolean = false, compact: Boolean = false) {
        val enabled = command in view.commands
        val foreground = if (primary) palette.onAccent else palette.text
        var modifier = GlanceModifier.size(if (primary && !compact) 56.dp else 48.dp).cornerRadius(28.dp)
        if (primary) modifier = modifier.background(Color(if (enabled) palette.accent else palette.secondary))
        if (enabled) modifier = modifier.clickable(
            if (command == "play") androidx.glance.appwidget.action.actionStartActivity(
                android.content.Intent(LocalContext.current, dev.qwidget.probe.SpotifyPlayActivity::class.java))
            else actionRunCallback<SpotifyPlaybackAction>(
                actionParametersOf(SpotifyPlaybackAction.commandKey to command)))
        Box(modifier, contentAlignment = Alignment.Center) {
            Image(ImageProvider(icon), contentDescription = if (enabled) label else "$label, kullanılamıyor",
                modifier = GlanceModifier.size(26.dp), colorFilter = ColorFilter.tint(
                    ColorProvider(Color(foreground).copy(alpha = if (enabled || primary) 1f else 0.38f))))
        }
    }
}

class SpotifyPlaybackAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val command = parameters[commandKey] ?: return
        if (command !in setOf("play", "pause", "next", "previous")) return
        if (command == "play") dev.qwidget.probe.SpotifyStarter.resume(context)
        else SpotifyAccess.command(context, command)
        WidgetUpdates.refresh(context)
    }
    companion object { val commandKey = ActionParameters.Key<String>("spotify_command") }
}

class SpotifyCompactReceiver : SpotifyReceiver()
class SpotifyLargeReceiver : SpotifyReceiver()

open class SpotifyReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpotifyWidget()
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val settings = WidgetSettingsStore(context)
        appWidgetIds.forEach(settings::remove)
        super.onDeleted(context, appWidgetIds)
    }
}
