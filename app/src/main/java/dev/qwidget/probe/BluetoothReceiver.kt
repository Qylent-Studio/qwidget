package dev.qwidget.probe

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothLeAudio
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dev.qwidget.automation.bluetooth.AudioConnectionGate
import dev.qwidget.automation.bluetooth.AudioEventDispatcher
import kotlinx.coroutines.*

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val profile = when (intent.action) {
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> "a2dp"
            BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED -> "le"
            else -> return
        }
        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
        if (state != BluetoothProfile.STATE_CONNECTED && state != BluetoothProfile.STATE_DISCONNECTED) return
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        val device = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        } ?: return
        // Invalidate pending playback immediately, not behind the suspending event queue.
        val generation = try {
            val selected = context.getSharedPreferences("probe", Context.MODE_PRIVATE).getString("device", null)
            if (selected == null || device.address != selected) return
            if (state == BluetoothProfile.STATE_DISCONNECTED) connectionGeneration.incrementAndGet()
            else connectionGeneration.get()
        } catch (_: SecurityException) { return }
        val result = goAsync()
        events.submit {
            try {
                    val prefs = context.getSharedPreferences("probe", Context.MODE_PRIVATE)
                    val selected = prefs.getString("device", null)
                    val address = device.address
                    if (selected == null || address != selected) return@submit
                    if (gateSelected != selected) { gateSelected = selected; gate = AudioConnectionGate() }
                    // Profile broadcasts are not sticky. Do not restore a persisted connection
                    // snapshot after reboot/permission revocation; missed disconnects make it stale.
                    val trigger = gate.update(profile, address, selected, state == BluetoothProfile.STATE_CONNECTED,
                        prefs.getBoolean("bluetooth_enabled", false))
                    prefs.edit().remove("audio_profiles").remove("audio_marker_device").apply()
                    if (trigger) {
                        val sent = SpotifyStarter.resume(context) {
                            connectionGeneration.get() == generation &&
                                prefs.getBoolean("bluetooth_enabled", false) &&
                                prefs.getString("device", null) == selected &&
                                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                        }
                        prefs.edit().putString("last_audio_status", if (sent)
                            "Ses bağlantısı hazır; devam ettirme komutu gönderildi." else
                            "Devam ettirilemedi. Spotify'ı, bildirim erişimini ve arka plan kısıtlamalarını kontrol edin.").apply()
                    }
            } catch (_: SecurityException) {
                ProbeState.status = "Bluetooth izni kaldırılmış."
            } finally { result.finish() }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val connectionGeneration = java.util.concurrent.atomic.AtomicLong()
        private val events = AudioEventDispatcher(scope)
        private var gateSelected: String? = null
        private var gate = AudioConnectionGate()
    }
}
