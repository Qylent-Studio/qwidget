package dev.qwidget.automation.skip

import android.content.Context
import android.media.MediaMetadata
import android.media.session.PlaybackState
import dev.qwidget.probe.SpotifyAccess
import dev.qwidget.spotify.widget.WidgetUpdates
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CurrentTrack(val id: String?, val title: String, val artist: String, val playing: Boolean, val canSkip: Boolean, val session: String)
data class AutomationSnapshot(val current: CurrentTrack?, val rules: List<SkipRule>, val enabled: Boolean, val status: String)

/** Serializes persistence, user edits and media callbacks off the service/UI thread. */
object SkipAutomation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var book: SkipRuleBook? = null
    private var engine: SkipEngine? = null
    private var status = "Kurallar telefonda saklanır."

    private fun prefs(context: Context) = context.getSharedPreferences("skip_rules", Context.MODE_PRIVATE)
    private fun initialize(context: Context) {
        if (book != null) return
        val prefs = prefs(context)
        val rules = SkipRuleBook(RuleCodec.decode(prefs.getString("rules", null))) {
            prefs.edit().putString("rules", RuleCodec.encode(it)).commit()
        }
        book = rules
        val restored = TrackIdentity.parse(prefs.getString("attempt_id", null))?.let { id ->
            prefs.getString("attempt_revision", null)?.let { SkipAttempt(id, it) }
        }
        val boot = android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, -1)
        val cooldown = if (boot >= 0 && prefs.getInt("budget_boot", -2) == boot) prefs.getLong("restart_cooldown_until", 0) else 0
        engine = SkipEngine(rules, restored, cooldown) { attempt ->
            prefs.edit().apply {
                if (attempt == null) { remove("attempt_id"); remove("attempt_revision") }
                else {
                    putString("attempt_id", attempt.trackId); putString("attempt_revision", attempt.revision)
                    putLong("restart_cooldown_until", android.os.SystemClock.elapsedRealtime() + 30000)
                    putInt("budget_boot", boot)
                }
            }.commit()
        }
    }

    private fun current(context: Context): CurrentTrack? = try {
        SpotifyAccess.find(context)?.let { controller ->
            val metadata = controller.metadata
            val state = controller.playbackState
            CurrentTrack(TrackIdentity.parse(metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)),
                metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.take(512) ?: "Şarkı bilgisi yok",
                metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.take(512).orEmpty(),
                state?.state == PlaybackState.STATE_PLAYING,
                ((state?.actions ?: 0L) and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L,
                controller.sessionToken.hashCode().toString())
        }
    } catch (_: SecurityException) { null }

    fun observe(context: Context) {
        val app = context.applicationContext
        scope.launch { mutex.withLock { initialize(app); evaluate(app) } }
    }

    private fun evaluate(context: Context) {
        val state = current(context)
        if (!prefs(context).getBoolean("enabled", true) || state == null) { engine!!.disconnected(); return }
        val shouldSkip = engine!!.observe(state.id, state.session, state.playing, state.canSkip, android.os.SystemClock.elapsedRealtime())
        if (shouldSkip && !SpotifyAccess.command(context, "next", state.id, state.session)) engine!!.commandFailed()
        if (engine!!.message.isNotBlank()) status = engine!!.message
    }

    suspend fun snapshot(context: Context): AutomationSnapshot = withContext(Dispatchers.IO) {
        mutex.withLock {
            initialize(context)
            AutomationSnapshot(current(context), book!!.all(), prefs(context).getBoolean("enabled", true), status)
        }
    }

    suspend fun save(context: Context, expected: CurrentTrack, count: Int?): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            initialize(context)
            val current = current(context)
            if (expected.id == null || current?.id != expected.id) return@withLock "Şarkı değişti veya kimlik güvenilir değil; kural eklenmedi."
            val saved = book!!.put(SkipRule(expected.id, expected.title, expected.artist, count, java.util.UUID.randomUUID().toString()))
            if (!saved) return@withLock "Kural kaydedilemedi. Sayı 1–999 olmalı."
            status = "Kural kaydedildi."
            evaluate(context)
            WidgetUpdates.request(context)
            "Kural kaydedildi; çalıyorsa ilk atlama hemen denenir."
        }
    }

    suspend fun edit(context: Context, rule: SkipRule, count: Int?): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            initialize(context)
            if (book!!.find(rule.trackId) != rule) return@withLock "Kural değişti; listeyi yeniden kontrol edin."
            if (!book!!.put(rule.copy(remaining = count, revision = java.util.UUID.randomUUID().toString()))) return@withLock "Kural kaydedilemedi."
            evaluate(context)
            "Kural güncellendi."
        }
    }

    suspend fun remove(context: Context, rule: SkipRule): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            initialize(context)
            if (book!!.find(rule.trackId) != rule) return@withLock "Kural değişti; listeyi yeniden kontrol edin."
            if (book!!.remove(rule.trackId)) "Kural kaldırıldı." else "Kural kaldırılamadı."
        }
    }

    suspend fun setEnabled(context: Context, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            initialize(context)
            val saved = prefs(context).edit().putBoolean("enabled", enabled).commit()
            if (saved) { if (!enabled) engine!!.disconnected() else evaluate(context) }
            saved
        }
    }
}
