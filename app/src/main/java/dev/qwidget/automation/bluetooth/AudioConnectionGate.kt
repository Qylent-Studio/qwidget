package dev.qwidget.automation.bluetooth

class AudioConnectionGate(initialProfiles: Set<String> = emptySet()) {
    private val active = initialProfiles.filter { it in setOf("a2dp", "le") }.toMutableSet()
    val profiles: Set<String> get() = active.toSet()
    fun update(profile: String, address: String?, selected: String?, connected: Boolean, enabled: Boolean): Boolean {
        if (profile !in setOf("a2dp", "le") || address.isNullOrBlank() || address != selected) return false
        val wasEmpty = active.isEmpty()
        if (connected) active.add(profile) else active.remove(profile)
        return connected && wasEmpty && active.isNotEmpty() && enabled
    }
}
