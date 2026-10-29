package dev.qwidget.battery

data class BatteryPresentation(val percentage: String, val progress: Int, val status: String) {
    companion object {
        fun from(level: Int, scale: Int, status: Int, present: Boolean, fastCharging: Boolean? = null): BatteryPresentation {
            if (!present || scale <= 0 || level < 0 || level > scale) {
                return BatteryPresentation("—", 0, "Pil bilgisi yok")
            }
            val percent = (level.toLong() * 100 / scale).toInt()
            // Android BatteryManager.BATTERY_STATUS_* values.
            val label = when (status) {
                2 -> if (fastCharging == true) "Hızlı şarj oluyor" else "Şarj oluyor"
                3 -> "Pil kullanılıyor"
                4 -> "Şarj olmuyor"
                5 -> "Şarj tamamlandı"
                else -> "Durum bilinmiyor"
            }
            return BatteryPresentation("%$percent", percent, label)
        }
    }
}
