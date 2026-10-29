package dev.qwidget.battery

/** Short-lived OEM evidence only; never infer charge speed from current or wattage. */
class OppoFastChargeState {
    var fastCharging: Boolean? = null
        private set
    private var charging = false
    private var plugged = 0

    fun batteryChanged(charging: Boolean, plugged: Int) {
        if (!charging || plugged == 0 || this.plugged != plugged) fastCharging = null
        this.charging = charging
        this.plugged = plugged
    }

    fun additionalBatteryChanged(charging: Boolean, plugged: Int, fast: Boolean?, initialSticky: Boolean) {
        if (initialSticky || !this.charging || !charging || plugged == 0 || this.plugged != plugged) return
        fastCharging = fast
    }
}
