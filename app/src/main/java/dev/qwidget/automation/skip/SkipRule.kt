package dev.qwidget.automation.skip

data class SkipRule(val trackId: String, val title: String, val artist: String, val remaining: Int?, val revision: String)

object TrackIdentity {
    private val pattern = Regex("spotify:track:[A-Za-z0-9]{22}")
    fun parse(mediaId: String?): String? = mediaId?.takeIf(pattern::matches)
}

class SkipRuleBook(initial: List<SkipRule> = emptyList(), private val persist: (List<SkipRule>) -> Boolean = { true }) {
    private var rules = initial.filter(::valid).distinctBy { it.trackId }
    fun all(): List<SkipRule> = rules.toList()
    fun find(id: String): SkipRule? = rules.firstOrNull { it.trackId == id }
    fun put(rule: SkipRule): Boolean = valid(rule) && replace(rules.filterNot { it.trackId == rule.trackId } + rule)
    fun remove(id: String): Boolean = replace(rules.filterNot { it.trackId == id })
    fun consume(expected: SkipRule): Boolean {
        if (find(expected.trackId) != expected) return false
        val count = expected.remaining ?: return true
        return if (count == 1) remove(expected.trackId) else put(expected.copy(remaining = count - 1))
    }
    private fun replace(next: List<SkipRule>): Boolean {
        if (!persist(next)) return false
        rules = next
        return true
    }
    companion object {
        fun valid(rule: SkipRule): Boolean = TrackIdentity.parse(rule.trackId) != null &&
            (rule.remaining == null || rule.remaining in 1..999) && rule.revision.isNotBlank() &&
            rule.revision.length <= 128 && rule.title.length <= 512 && rule.artist.length <= 512
    }
}

object RuleCodec {
    fun encode(rules: List<SkipRule>): String {
        val props = java.util.Properties()
        props.setProperty("version", "1")
        rules.filter(SkipRuleBook::valid).forEach { rule ->
            val prefix = rule.trackId
            props.setProperty("$prefix.count", rule.remaining?.toString() ?: "always")
            props.setProperty("$prefix.title", rule.title)
            props.setProperty("$prefix.artist", rule.artist)
            props.setProperty("$prefix.revision", rule.revision)
        }
        return java.io.StringWriter().also { props.store(it, null) }.toString()
    }
    fun decode(raw: String?): List<SkipRule> = try {
        val props = java.util.Properties().apply { load(java.io.StringReader(raw.orEmpty())) }
        if (props.getProperty("version") != "1") emptyList() else props.stringPropertyNames()
            .filter { it.endsWith(".count") }.sorted().mapNotNull { property ->
                val id = property.removeSuffix(".count")
                val value = props.getProperty(property)
                val count = if (value == "always") null else value.toIntOrNull() ?: return@mapNotNull null
                SkipRule(id, props.getProperty("$id.title", ""), props.getProperty("$id.artist", ""),
                    count, props.getProperty("$id.revision", "")).takeIf(SkipRuleBook::valid)
            }
    } catch (_: IllegalArgumentException) { emptyList() }
}
