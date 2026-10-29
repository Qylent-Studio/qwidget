package dev.qwidget.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import dev.qwidget.core.theme.WidgetPalette
import dev.qwidget.probe.ProbeActivity
import dev.qwidget.spotify.widget.*
import dev.qwidget.battery.BatteryUpdates
import dev.qwidget.battery.BatteryWidgetReceiver
import dev.qwidget.core.theme.WidgetGlassStyle
import android.graphics.drawable.LayerDrawable
import kotlinx.coroutines.*

/** Native configuration shared by Spotify and battery widgets. */
class WidgetSettingsActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selection = WidgetPalette.all.first()
    private lateinit var content: LinearLayout
    private lateinit var preview: LinearLayout
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var screen: LinearLayout
    private var previewJob: Job? = null
    private val paletteButtons = mutableListOf<Pair<RadioButton, WidgetPalette>>()
    private val store by lazy { WidgetSettingsStore(this) }
    private val manager by lazy { AppWidgetManager.getInstance(this) }
    private fun isBattery(id: Int) = manager.getAppWidgetInfo(id)?.provider?.className == BatteryWidgetReceiver::class.java.name
    private fun allIds() = WidgetUpdates.ids(this).toList() + BatteryUpdates.ids(this).toList()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        renderScreen(savedInstanceState)
    }

    private fun renderScreen(savedInstanceState: Bundle?) {
        previewJob?.cancel()
        paletteButtons.clear()
        widgetId = savedInstanceState?.takeIf { it.containsKey("configured_widget") }?.getInt("configured_widget")
            ?: intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && !isOwnWidget(widgetId)) { finish(); return }
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE && widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        selection = WidgetPalette.find(savedInstanceState?.getString("selection") ?: store.palette(widgetId).id)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val scroll = ScrollView(this).apply { addView(content); isFillViewport = true }
        screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.background)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        screen.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(screen)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) dashboard() else configure()
    }

    private fun dashboard() {
        content.removeAllViews()
        heading("QWidget", 30f)
        note("Ana ekranın. Senin renklerin.")
        val add = card("Widget ekle", "Müzik veya pil. Ekle, rengini seç, boyutunu ayarla.")
        val addRow = row(add)
        button("Spotify ekle", true, addRow) {
            android.app.AlertDialog.Builder(this).setTitle("Spotify boyutu")
                .setItems(arrayOf("4×3 · Kompakt", "4×4 · Büyük kapak")) { _, which ->
                    pin(if (which == 0) SpotifyCompactReceiver::class.java else SpotifyLargeReceiver::class.java)
                }.setNegativeButton("Vazgeç", null).show()
        }.layoutParams = rowParams()
        button("Pil ekle", false, addRow) { pin(BatteryWidgetReceiver::class.java) }.layoutParams = rowParams()
        heading("Widgetlarım", 20f)
        val ids = allIds()
        if (ids.isEmpty()) note("Henüz widget eklemedin. Yukarıdan ilk widgetını seç.") else {
            ids.forEachIndexed { index, id ->
                val large = manager.getAppWidgetInfo(id)?.provider?.className == SpotifyLargeReceiver::class.java.name
                val palette = store.palette(id)
                val widgetLabel = if (isBattery(id)) "Pil" else "Spotify"
                val size = if (isBattery(id)) "2×1" else if (large) "4×4" else "4×3"
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = ripple(Ui.panel)
                    minimumHeight = dp(76)
                    isFocusable = true
                    contentDescription = "${index + 1}. $widgetLabel, $size başlangıç boyutu, ${palette.label}. Renk ayarları"
                    setOnClickListener {
                        startActivity(Intent(this@WidgetSettingsActivity, WidgetSettingsActivity::class.java)
                            .setData(android.net.Uri.parse("qwidget://settings/$id"))
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                    }
                }
                tile.addView(View(this).apply {
                    background = rounded(palette.surface.toInt(), 12, palette.accent.toInt())
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })
                val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                label(widgetLabel, 16f, Ui.text, info, true)
                label("$size başlangıç · ${palette.label}", 12f, Ui.secondary, info)
                tile.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
                label("›", 26f, Ui.secondary, tile).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(24), -2)
                    gravity = Gravity.CENTER
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                content.addView(tile, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
        }
        val automation = card("Otomasyonlar", "Şarkı atlama kuralları ve seçtiğin Bluetooth aygıtıyla devam ettirme.")
        button("Kurallar ve Bluetooth", parent = automation) {
            startActivity(Intent(this, dev.qwidget.automation.AutomationActivity::class.java))
        }
        val allowed = getSystemService(android.app.NotificationManager::class.java)
            .isNotificationListenerAccessGranted(dev.qwidget.probe.SpotifyAccess.component(this))
        val permissions = card("İzin durumu", "Pil widgetı izin gerektirmez. Müzik bilgisi için bildirim erişimi kullanılır.")
        label(if (allowed) "Spotify erişimi açık" else "Spotify için erişim gerekli", 14f,
            if (allowed) Ui.accent else Ui.warning, permissions, true)
        val bluetooth = checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        label(if (bluetooth) "Bluetooth izni açık" else "Bluetooth izni kapalı", 12f, Ui.secondary, permissions)
        button("Bildirim erişimi", !allowed, permissions) { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        button("Teknik kontrol", parent = permissions) { startActivity(Intent(this, ProbeActivity::class.java)) }
        note("Boyut için ana ekranda widgeta uzun basıp kenarlarından sürükle. En küçük boyutu başlatıcın belirler. Verilerin cihazında kalır.")
    }

    private fun configure() {
        heading(if (isBattery(widgetId)) "Pil widgetı" else "Spotify widgetı", 14f)
        heading("Rengini seç", 28f)
        note("Yalnızca bu widget değişir. Diğer widgetların kendi renklerini korur.")
        preview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        if (!isBattery(widgetId)) {
            val cover = ImageView(this).apply {
                contentDescription = "Albüm kapağı önizlemesi"
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(dev.qwidget.R.drawable.ic_music)
                background = rounded(Ui.panel, 12)
                clipToOutline = true
            }
            preview.addView(cover, LinearLayout.LayoutParams(-1, dp(80)))
            title = label("Spotify", 20f, Ui.text, preview, true).apply { setPadding(0, dp(8), 0, 0); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
            artist = label("Önizleme yükleniyor…", 13f, Ui.secondary, preview).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
            previewJob = scope.launch {
                val snapshot = withContext(Dispatchers.IO) { SpotifyWidgetSnapshot.read(this@WidgetSettingsActivity) }
                title.text = snapshot.presentation.title
                artist.text = snapshot.presentation.subtitle
                snapshot.cover?.let(cover::setImageBitmap)
            }
        } else {
            val battery = BatteryUpdates.read(this)
            title = TextView(this).apply {
                text = battery.percentage; textSize = 28f; setTypeface(null, android.graphics.Typeface.BOLD)
            }
            artist = TextView(this).apply { text = battery.status; textSize = 14f }
            preview.addView(title); preview.addView(artist)
        }
        content.addView(preview, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16); bottomMargin = dp(16) })
        heading("Renk paleti", 18f)
        WidgetPalette.all.chunked(2).forEach { pair ->
            val group = row(content)
            pair.forEach { palette ->
                val choice = RadioButton(this).apply {
                id = View.generateViewId()
                text = palette.label
                textSize = 14f
                minHeight = dp(56)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setTextColor(palette.text.toInt())
                buttonTintList = ColorStateList.valueOf(palette.accent.toInt())
                isSaveEnabled = false
                isChecked = palette.id == selection.id
                setOnClickListener { selection = palette; applyPreview() }
                }
                paletteButtons += choice to palette
                group.addView(choice, rowParams())
            }
        }
        applyPreview()
        note("Cam yüzey hafif saydamdır. Bu önizleme koyu zemin üzerindedir; ana ekranda duvar kâğıdın yüzeyin arkasından görünür. Gerçek arka plan bulanıklığı uygulanmaz.")
        if (!isBattery(widgetId)) {
            val recovery = row(content)
            button("Bildirim erişimi", parent = recovery) { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }.layoutParams = rowParams()
            button("Spotify'ı aç", parent = recovery) { openSpotify() }.layoutParams = rowParams()
        } else {
            note("Hızlı şarj yalnızca telefonun doğruladığı canlı veride gösterilir. Süreç kapalıyken pil yenilemesi 30 dakika veya daha fazla gecikebilir.")
        }
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(Ui.background)
        }
        screen.addView(footer)
        button("Vazgeç", parent = footer) { finish() }.layoutParams = rowParams()
        button("Rengi kaydet", true, footer) { save() }.layoutParams = rowParams()
    }

    private fun save() {
        if (!isOwnWidget(widgetId)) { finish(); return }
        store.savePalette(widgetId, selection.id)
        if (isBattery(widgetId)) BatteryUpdates.refresh(this) else WidgetUpdates.request(this)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        Toast.makeText(this, "Widget rengi kaydedildi", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun applyPreview() {
        val glass = WidgetGlassStyle.from(selection)
        val surface = rounded(glass.surface.toInt(), 24)
        val highlight = getDrawable(dev.qwidget.R.drawable.battery_glass_highlight)!!.mutate()
        preview.background = LayerDrawable(arrayOf(surface, highlight))
        title.setTextColor(glass.text.toInt()); artist.setTextColor(glass.secondary.toInt())
        paletteButtons.forEach { (button, palette) ->
            button.isChecked = palette.id == selection.id
            button.background = rounded(palette.surface.toInt(), 14,
                if (button.isChecked) palette.accent.toInt() else Ui.border)
        }
    }

    private fun pin(provider: Class<*>) {
        if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(this, provider), null, null)
        else Toast.makeText(this, "Ana ekrana uzun basıp Widgetlar → QWidget yolunu kullanın.", Toast.LENGTH_LONG).show()
    }

    private fun openSpotify() {
        val launch = packageManager.getLaunchIntentForPackage("com.spotify.music")
        if (launch == null) Toast.makeText(this, "Önce Spotify uygulamasını yükleyin.", Toast.LENGTH_LONG).show()
        else try { startActivity(launch) } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Spotify açılamadı; yüklü olduğunu kontrol edin.", Toast.LENGTH_LONG).show()
        }
    }

    private fun isOwnWidget(id: Int): Boolean = manager.getAppWidgetInfo(id)?.provider?.let {
        it.packageName == packageName && it.className in setOf(SpotifyCompactReceiver::class.java.name, SpotifyLargeReceiver::class.java.name, BatteryWidgetReceiver::class.java.name)
    } == true

    private fun heading(label: String, size: Float) {
        label(label, size, Ui.text, content, true).apply {
            setPadding(0, dp(12), 0, dp(8)); isAccessibilityHeading = true
        }
    }

    private fun note(label: String) {
        label(label, 13f, Ui.secondary, content).setPadding(0, 0, 0, dp(16))
    }

    private fun button(label: String, primary: Boolean = false, parent: LinearLayout = content, action: () -> Unit): Button {
        val button = Button(this).apply {
            text = label; contentDescription = label; isAllCaps = false; textSize = 14f; minHeight = dp(52)
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = ripple(if (primary) Ui.accent else Ui.button)
            setTextColor(if (primary) Ui.onAccent else Ui.text)
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        return button
    }

    private fun label(text: String, size: Float, color: Int, parent: LinearLayout, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }

    private fun card(title: String, description: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(16))
        background = rounded(Ui.panel, 22, Ui.border)
        label(title, 18f, Ui.text, this, true).apply { isAccessibilityHeading = true; setPadding(0, 0, 0, dp(6)) }
        label(description, 13f, Ui.secondary, this)
        content.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12); bottomMargin = dp(4) })
    }

    private fun row(parent: LinearLayout): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }

    private fun rowParams() = LinearLayout.LayoutParams(0, -2, 1f).apply {
        setMargins(dp(3), dp(6), dp(3), dp(6))
    }

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }

    private fun ripple(color: Int) = RippleDrawable(ColorStateList.valueOf(Ui.ripple), rounded(color, 16), rounded(Ui.text, 16))

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("selection", selection.id)
        outState.putInt("configured_widget", widgetId)
        super.onSaveInstanceState(outState)
    }
    override fun onResume() {
        super.onResume()
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID && ::content.isInitialized) dashboard()
        WidgetUpdates.request(this)
        BatteryUpdates.refresh(this)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Rebuild from the delivered widget ID; recreate() may replay the old task intent.
        renderScreen(null)
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private object Ui {
        val background = 0xFF0C0F11.toInt()
        val panel = 0xFF191F22.toInt()
        val button = 0xFF2A3336.toInt()
        val border = 0xFF394247.toInt()
        val text = 0xFFF5F7F6.toInt()
        val secondary = 0xFFB4C1BB.toInt()
        val accent = 0xFFB8F39B.toInt()
        val onAccent = 0xFF172112.toInt()
        val warning = 0xFFFFD39B.toInt()
        val ripple = 0x337F8F82
    }
}
