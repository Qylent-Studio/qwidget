package dev.qwidget.probe

object ProbePolicy {
    fun acceptsPackage(packageName: String?): Boolean = packageName == "com.spotify.music"
}

class ConnectionGate {
    private val connected = mutableSetOf<String>()

    @Synchronized
    fun connected(address: String?, selectedAddress: String?): Boolean {
        if (address.isNullOrBlank() || address != selectedAddress) return false
        return connected.add(address)
    }

    @Synchronized
    fun disconnected(address: String?) { connected.remove(address) }
}
