package dev.qwidget.automation.skip

data class SkipAttempt(val trackId: String, val revision: String)

class SkipEngine(
    private val rules: SkipRuleBook,
    restoredAttempt: SkipAttempt? = null,
    private val restoredCooldownUntilMs: Long = 0,
    private val recordAttempt: (SkipAttempt?) -> Boolean = { true },
) {
    private data class Pending(val rule: SkipRule, val session: String, val sentAt: Long)
    private var pending: Pending? = null
    private var lastAttempt = restoredAttempt
    private var lastKnownId = restoredAttempt?.trackId
    private val recentCommands = ArrayDeque<Long>()
    var message: String = ""
        private set

    fun observe(trackId: String?, session: String, playing: Boolean, canSkip: Boolean, nowMs: Long): Boolean {
        pending?.let {
            if (it.session != session || nowMs - it.sentAt !in 0..8000) {
                pending = null
                message = "Atlama teyit edilmedi; sayaç değiştirilmedi."
            }
        }
        val key = TrackIdentity.parse(trackId) ?: return false
        pending?.let {
            if (key != it.rule.trackId && playing) {
                val saved = rules.consume(it.rule)
                pending = null
                message = if (saved) "Atlama doğrulandı." else "Kural değişti veya sayaç kaydedilemedi."
            }
        }
        if (lastKnownId != key) {
            if (lastAttempt != null && recordAttempt(null)) lastAttempt = null
            lastKnownId = key
        }
        if (!playing || !canSkip || pending != null) return false
        val rule = rules.find(key) ?: return false
        if (nowMs < restoredCooldownUntilMs) {
            message = "Yeniden başlatma sonrası 30 saniyelik güvenlik beklemesi."
            return false
        }
        val attempt = SkipAttempt(key, rule.revision)
        if (attempt == lastAttempt) return false
        if (!recordAttempt(attempt)) { message = "Deneme kaydedilemedi; güvenlik için atlama yapılmadı."; return false }
        lastAttempt = attempt
        while (recentCommands.isNotEmpty() && nowMs - recentCommands.first() >= 30000) recentCommands.removeFirst()
        if (recentCommands.size >= 5) {
            message = "Atlama döngüsü sınırı: 30 saniyede en fazla 5 otomatik atlama."
            return false
        }
        recentCommands.addLast(nowMs)
        pending = Pending(rule, session, nowMs)
        message = "Otomatik atlama gönderiliyor; sayaç için şarkı değişimi bekleniyor."
        return true
    }

    fun commandFailed() { pending = null; message = "Atlama gönderilemedi; sayaç değiştirilmedi." }
    fun disconnected() { pending = null }
}

object AutomaticSkipTarget {
    fun matches(expectedId: String, expectedSession: String, actualId: String?, actualSession: String,
                playing: Boolean): Boolean = expectedId == actualId && expectedSession == actualSession && playing
}
