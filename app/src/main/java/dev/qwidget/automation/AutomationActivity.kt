package dev.qwidget.automation

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.*
import dev.qwidget.automation.skip.*
import dev.qwidget.probe.SpotifyAccess
import dev.qwidget.settings.WidgetSettingsActivity
import dev.qwidget.spotify.widget.WidgetUpdates
import kotlinx.coroutines.*

/** Spotify-wide rules are separate from per-widget visual settings. */
class AutomationActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null
    private lateinit var content: LinearLayout
    private lateinit var currentLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var rulesLayout: LinearLayout
    private lateinit var deviceLabel: TextView
    private lateinit var enabledSwitch: Switch
    private lateinit var nowButton: Button
    private lateinit var countButton: Button
    private lateinit var alwaysButton: Button
    private var current: CurrentTrack? = null
    private var lastRules: List<SkipRule>? = null
    private var rendering = false
    private val btPrefs by lazy { getSharedPreferences("probe", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(0xFF0C0F11.toInt())
        }
        val scroll = ScrollView(this).apply { addView(content); setBackgroundColor(0xFF0C0F11.toInt()) }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        setContentView(scroll)
        label("Spotify · menü", 26f)
        currentLabel = label("Şarkı bilgisi yükleniyor…", 18f)
        statusLabel = label("", 14f)
        nowButton = button("Şimdi atla") {
            val expected = current?.id
            scope.launch {
                val sent = withContext(Dispatchers.IO) { SpotifyAccess.command(this@AutomationActivity, "next", expected) }
                toast(if (sent) "Atlama komutu gönderildi." else "Atlanamadı; Spotify oturumunu kontrol edin.")
                WidgetUpdates.request(this@AutomationActivity)
            }
        }
        countButton = button("N kez atla", true) { current?.let { track -> countDialog(null) { save(track, it) } } }
        alwaysButton = button("Hep atla") { current?.let { save(it, null) } }
        label("Çalıyorsa ilk atlama hemen denenir ve sayıya dahildir. Duraklatılmış şarkı başlatılmaz. Güvenilir Spotify kimliği olmayan içeriklere kural eklenmez.", 14f)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) button("Renk ve görünüm") {
            startActivity(Intent(this, WidgetSettingsActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }
        label("Atlama kuralların", 22f)
        enabledSwitch = Switch(this).apply {
            text = "Otomatik atlama açık"
            setTextColor(Color.WHITE); minHeight = dp(48)
            setOnCheckedChangeListener { _, checked ->
                if (!rendering) scope.launch {
                    if (!SkipAutomation.setEnabled(this@AutomationActivity, checked)) toast("Ayar kaydedilemedi.")
                    refresh()
                }
            }
        }
        content.addView(enabledSwitch)
        rulesLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(rulesLayout)
        label("Bluetooth ile devam et", 22f)
        deviceLabel = label("", 14f)
        button("Bluetooth aygıtı seç") { chooseDevice() }
        content.addView(Switch(this).apply {
            text = "Seçilen aygıt bağlanınca Spotify'ı devam ettir"
            setTextColor(Color.WHITE); minHeight = dp(48)
            isChecked = btPrefs.getBoolean("bluetooth_enabled", false)
            setOnCheckedChangeListener { _, checked ->
                btPrefs.edit().putBoolean("bluetooth_enabled", checked).apply()
            }
        })
        label("Yalnızca seçtiğin aygıtın A2DP / LE Audio ses bağlantısı izlenir. Spotify kapalıysa başlatıp müziği devam ettirmeyi dener. Android arka plan başlatmasını engelleyebilir; bu durumda widget'tan oynata dokun. Sürekli tekrar yapılmaz.", 14f)
        button("Bildirim erişimi") { startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }

    private suspend fun refresh() {
        val snapshot = SkipAutomation.snapshot(this)
        current = snapshot.current
        currentLabel.text = snapshot.current?.let { "${it.title}\n${it.artist}\n${if (it.playing) "Çalıyor" else "Duraklatıldı / bekliyor"}" } ?: "Spotify oturumu yok. Spotify'ı açıp müzik başlatın."
        statusLabel.text = if (snapshot.current?.id == null && snapshot.current != null)
            "Bu içerik için güvenilir şarkı kimliği yok; otomatik kural eklenemez." else snapshot.status
        nowButton.isEnabled = snapshot.current?.canSkip == true
        countButton.isEnabled = snapshot.current?.id != null
        alwaysButton.isEnabled = snapshot.current?.id != null
        rendering = true; enabledSwitch.isChecked = snapshot.enabled; rendering = false
        deviceLabel.text = "Aygıt: ${btPrefs.getString("device_name", "seçilmedi")}\n${btPrefs.getString("last_audio_status", "Henüz ses bağlantısı olayı yok.")}"
        if (lastRules != snapshot.rules) {
            lastRules = snapshot.rules
            rulesLayout.removeAllViews()
            if (snapshot.rules.isEmpty()) rulesLayout.addView(text("Henüz atlama kuralı yok.", 14f))
            snapshot.rules.forEach { rule ->
                rulesLayout.addView(text("${rule.title}\n${rule.artist}\n${rule.remaining?.let { "$it atlama kaldı" } ?: "Her zaman atla"}", 16f))
                rulesLayout.addView(makeButton("Düzenle / kaldır") {
                    AlertDialog.Builder(this).setTitle(rule.title)
                        .setItems(arrayOf("Atlama sayısını değiştir", "Hep atla", "Kuralı kaldır")) { _, index ->
                            when (index) {
                                0 -> countDialog(rule.remaining) { count -> scope.launch { toast(SkipAutomation.edit(this@AutomationActivity, rule, count)); refresh() } }
                                1 -> scope.launch { toast(SkipAutomation.edit(this@AutomationActivity, rule, null)); refresh() }
                                2 -> AlertDialog.Builder(this).setTitle("Kural kaldırılsın mı?")
                                    .setMessage(rule.title).setNegativeButton("Vazgeç", null)
                                    .setPositiveButton("Kaldır") { _, _ -> scope.launch { toast(SkipAutomation.remove(this@AutomationActivity, rule)); refresh() } }.show()
                            }
                        }.setNegativeButton("Vazgeç", null).show()
                })
            }
        }
    }

    private fun save(track: CurrentTrack, count: Int?) {
        scope.launch { toast(SkipAutomation.save(this@AutomationActivity, track, count)); refresh() }
    }

    private fun countDialog(count: Int?, save: (Int) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(3))
            hint = "1–999"; contentDescription = "Atlama sayısı"
            count?.let { setText(it.toString()) }
            setPadding(dp(24), dp(12), dp(24), dp(12))
        }
        val dialog = AlertDialog.Builder(this).setTitle("Kaç kez atla?").setView(input)
            .setMessage("Çalan şarkıya eklerken ilk atlama hemen denenir.")
            .setNegativeButton("Vazgeç", null).setPositiveButton("Kaydet", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val number = input.text.toString().toIntOrNull()
                if (number == null || number !in 1..999) input.error = "1 ile 999 arasında sayı girin."
                else { save(number); dialog.dismiss() }
            }
        }
        dialog.show()
    }

    private fun chooseDevice() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            toast("İzni verdikten sonra aygıt seçimine tekrar dokunun."); return
        }
        try {
            val devices = getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices?.toList().orEmpty()
            if (devices.isEmpty()) { toast("Önce Android Bluetooth ayarlarından aygıt eşleştirin."); return }
            AlertDialog.Builder(this).setTitle("Bluetooth aygıtı seç")
                .setItems(devices.map { it.name ?: "Adsız aygıt" }.toTypedArray()) { _, index ->
                    val device = devices[index]
                    btPrefs.edit().putString("device", device.address).putString("device_name", device.name ?: "Adsız aygıt")
                        .remove("audio_marker_device").remove("audio_profiles").remove("last_audio_status").apply()
                    toast("Aygıt seçildi; test için bağlantıyı kesip yeniden bağlayın.")
                    scope.launch { refresh() }
                }.setNegativeButton("Vazgeç", null).show()
        } catch (_: SecurityException) { toast("Bluetooth erişimi kaldırılmış.") }
    }

    private fun text(value: String, size: Float) = TextView(this).apply {
        text = value; textSize = size; setTextColor(if (size >= 18) 0xFFF5F7F6.toInt() else 0xFFADB6B1.toInt())
        setPadding(0, dp(12), 0, dp(12))
    }
    private fun label(value: String, size: Float) = text(value, size).also(content::addView)
    private fun makeButton(value: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false; minHeight = dp(52)
        backgroundTintList = android.content.res.ColorStateList.valueOf(if (primary) 0xFFB8F39B.toInt() else 0xFF252B28.toInt())
        setTextColor(if (primary) 0xFF172112.toInt() else Color.WHITE)
        setOnClickListener { action() }
    }
    private fun button(value: String, primary: Boolean = false, action: () -> Unit) = makeButton(value, primary, action).also {
        content.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onResume() {
        super.onResume()
        refreshJob = scope.launch { while (isActive) { refresh(); delay(1000) } }
    }
    override fun onPause() { refreshJob?.cancel(); super.onPause() }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
