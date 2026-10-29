package dev.qwidget.probe

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.*

/** Temporary feasibility controls, not the finished widget/product UI. */
class ProbeActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var information: TextView
    private lateinit var cover: ImageView
    private lateinit var deviceLabel: TextView
    private val prefs by lazy { getSharedPreferences("probe", MODE_PRIVATE) }
    private val update = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 60, 32, 48)
        }
        layout.addView(TextView(this).apply { text = "QWidget • Teknik doğrulama"; textSize = 24f })
        layout.addView(TextView(this).apply {
            text = "Yalnızca Spotify oturumu izlenir; bildirim metinleri ve dinleme geçmişi kaydedilmez. Bu sürüm ana ekran widgetı içermez."
        })
        fun button(label: String, action: () -> Unit) {
            layout.addView(Button(this).apply { text = label; setOnClickListener { action() } })
        }
        button("1. Bildirim erişimini aç") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        button("2. Spotify'ı aç") {
            val launch = packageManager.getLaunchIntentForPackage("com.spotify.music")
            if (launch != null) startActivity(launch)
            else ProbeState.status = "Spotify yüklü değil."
        }
        cover = ImageView(this).apply { contentDescription = "Albüm kapağı"; adjustViewBounds = true }
        layout.addView(cover, LinearLayout.LayoutParams(-1, (160 * resources.displayMetrics.density).toInt()))
        information = TextView(this).apply { textSize = 16f }
        layout.addView(information)
        button("Önceki") { SpotifyAccess.command(this, "previous") }
        button("Oynat") { SpotifyAccess.command(this, "play") }
        button("Duraklat") { SpotifyAccess.command(this, "pause") }
        button("Sonraki / şimdi atla") { SpotifyAccess.command(this, "next") }
        deviceLabel = TextView(this)
        layout.addView(deviceLabel)
        button("3. Bluetooth aygıtı seç") { chooseDevice() }
        layout.addView(Switch(this).apply {
            text = "Seçilen aygıt bağlanınca devam ettirmeyi dene"
            isChecked = prefs.getBoolean("bluetooth_enabled", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("bluetooth_enabled", checked).apply()
            }
        })
        button("Pil / arka plan ayarlarını aç") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:$packageName")))
        }
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun chooseDevice() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            ProbeState.status = "İzni verdikten sonra aygıt seç düğmesine tekrar dokunun."
            return
        }
        try {
            val adapter = getSystemService(BluetoothManager::class.java)?.adapter
            val devices = adapter?.bondedDevices?.toList().orEmpty()
            if (devices.isEmpty()) {
                ProbeState.status = "Eşleştirilmiş aygıt yok. Önce Android ayarlarından eşleştirin."
                return
            }
            AlertDialog.Builder(this).setTitle("Devam ettirme aygıtı")
                .setItems(devices.map { it.name ?: "Adsız aygıt" }.toTypedArray()) { _, index ->
                    val device = devices[index]
                    prefs.edit().putString("device", device.address)
                        .putString("device_name", device.name ?: "Adsız aygıt")
                        .remove("audio_profiles").remove("audio_marker_device").apply()
                    ProbeState.status = "Aygıt seçildi. Test için bağlantıyı kesip yeniden bağlayın."
                    render()
                }.setNegativeButton("İptal", null).show()
        } catch (_: SecurityException) {
            ProbeState.status = "Bluetooth izni kullanılamıyor."
        }
    }

    private fun render() {
        information.text = "Dinleyici: ${if (ProbeState.listenerConnected) "bağlı" else "bağlı değil"}\n" +
            "${ProbeState.title}\n${ProbeState.artist}\n" +
            "Durum: ${if (ProbeState.playing) "çalıyor" else "çalmıyor"}\n" +
            "Medya kimliği: ${if (ProbeState.trackId.isBlank()) "yok (kalıcı kural için araştırılmalı)" else "mevcut"}\n" +
            "Algılanan değişim: ${ProbeState.changes} • Son: ${ProbeState.lastChange}\n${ProbeState.status}"
        cover.setImageBitmap(ProbeState.cover)
        deviceLabel.text = "Seçilen aygıt: ${prefs.getString("device_name", "yok")}"
    }

    override fun onResume() { super.onResume(); handler.post(update) }
    override fun onPause() { handler.removeCallbacks(update); super.onPause() }
}
