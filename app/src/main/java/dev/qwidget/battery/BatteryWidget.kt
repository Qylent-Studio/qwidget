package dev.qwidget.battery

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import android.view.View
import dev.qwidget.R
import dev.qwidget.settings.WidgetSettingsActivity
import dev.qwidget.settings.WidgetSettingsStore
import dev.qwidget.core.theme.WidgetGlassStyle

/** Battery monitoring does not depend on notification access or Spotify. */
class QWidgetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BatteryUpdates.ids(this).isNotEmpty()) BatteryUpdates.start(this)
    }
}

class BatteryWidgetReceiver : AppWidgetProvider() {
    override fun onEnabled(context: Context) { BatteryUpdates.start(context) }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        BatteryUpdates.start(context)
        BatteryUpdates.refresh(context)
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager,
        appWidgetId: Int, newOptions: Bundle) {
        BatteryUpdates.refresh(context)
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach(WidgetSettingsStore(context)::remove)
    }
    override fun onDisabled(context: Context) { BatteryUpdates.stop(context) }
}

object BatteryUpdates {
    private const val ADDITIONAL_BATTERY_CHANGED = "android.intent.action.ADDITIONAL_BATTERY_CHANGED"
    private val fastCharge = OppoFastChargeState()
    private var listening = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> refresh(context, intent)
                ADDITIONAL_BATTERY_CHANGED -> {
                    // OEM sticky data can outlive the charging session. Only accept live
                    // reports from the system through the non-exported receiver (API 33+).
                    if (isInitialStickyBroadcast) return
                    read(context)
                    fastCharge.additionalBatteryChanged(
                        intent.getIntExtra("status", -1) == BatteryManager.BATTERY_STATUS_CHARGING,
                        intent.getIntExtra("chargeplugged", 0),
                        if (intent.hasExtra("chargefastcharger")) intent.getBooleanExtra("chargefastcharger", false) else null,
                        false)
                    refresh(context)
                }
            }
        }
    }

    fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, BatteryWidgetReceiver::class.java))

    fun start(context: Context) {
        if (listening) return
        if (Build.VERSION.SDK_INT >= 33) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            filter.addAction(ADDITIONAL_BATTERY_CHANGED)
            context.applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            // No unprotected OEM broadcasts on older APIs without sender isolation.
            context.applicationContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
        listening = true
    }

    fun stop(context: Context) {
        if (!listening) return
        context.applicationContext.unregisterReceiver(receiver)
        listening = false
        fastCharge.batteryChanged(false, 0)
    }

    fun read(context: Context): BatteryPresentation = presentation(context.registerReceiver(null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)))

    private fun presentation(intent: Intent?): BatteryPresentation {
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) ?: 1
        fastCharge.batteryChanged(status == BatteryManager.BATTERY_STATUS_CHARGING,
            intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0)
        return BatteryPresentation.from(
            intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1,
            intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 0) ?: 0,
            status,
            intent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false) ?: false,
            fastCharge.fastCharging)
    }

    fun refresh(context: Context, battery: Intent? = null) {
        val ids = ids(context)
        if (ids.isEmpty()) return
        val view = battery?.let(::presentation) ?: read(context)
        val manager = AppWidgetManager.getInstance(context)
        val store = WidgetSettingsStore(context)
        ids.forEach { id ->
            // Minimum dimensions fit both launcher orientations; do not assume the
            // default 2x1 footprint after a resize or when options are unavailable.
            val options = manager.getAppWidgetOptions(id)
            val layout = BatteryLayoutSpec.fromSize(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                context.resources.configuration.fontScale)
            val padding = (layout.paddingDp * context.resources.displayMetrics.density).toInt()
            val verticalPadding = (layout.verticalPaddingDp * context.resources.displayMetrics.density).toInt()
            val palette = store.palette(id)
            val glass = WidgetGlassStyle.from(palette)
            val settings = Intent(context, WidgetSettingsActivity::class.java)
                // Extras do not participate in task/PendingIntent identity. Give this
                // destination its own identity so NEW_TASK cannot resume the dashboard.
                .setData(android.net.Uri.parse("qwidget://battery/$id"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            val click = PendingIntent.getActivity(context, id, settings,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val views = RemoteViews(context.packageName, R.layout.widget_battery).apply {
                setViewPadding(R.id.battery_content, padding, verticalPadding, padding, verticalPadding)
                setViewVisibility(R.id.battery_status, if (layout.showStatus) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.battery_progress, if (layout.showProgress) View.VISIBLE else View.GONE)
                setTextViewTextSize(R.id.battery_percentage, TypedValue.COMPLEX_UNIT_SP, layout.titleSp)
                setTextViewTextSize(R.id.battery_status, TypedValue.COMPLEX_UNIT_SP, layout.bodySp)
                setViewLayoutHeight(R.id.battery_progress, layout.progressHeightDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                setColorStateList(R.id.battery_surface, "setBackgroundTintList", ColorStateList.valueOf(glass.surface.toInt()))
                setTextViewText(R.id.battery_percentage, view.percentage)
                setTextViewText(R.id.battery_status, view.status)
                setContentDescription(R.id.battery_status, view.status)
                setTextColor(R.id.battery_percentage, glass.text.toInt())
                setTextColor(R.id.battery_status, glass.secondary.toInt())
                setProgressBar(R.id.battery_progress, 100, view.progress, false)
                setColorStateList(R.id.battery_progress, "setProgressTintList", ColorStateList.valueOf(palette.accent.toInt()))
                setColorStateList(R.id.battery_progress, "setProgressBackgroundTintList",
                    ColorStateList.valueOf(((palette.accent and 0x00FFFFFFL) or 0x33000000L).toInt()))
                setOnClickPendingIntent(R.id.battery_root, click)
                setContentDescription(R.id.battery_root, "Pil ${view.percentage}, ${view.status}. Renk ayarları")
            }
            manager.updateAppWidget(id, views)
        }
    }
}
