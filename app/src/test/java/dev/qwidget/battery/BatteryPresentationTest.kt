package dev.qwidget.battery

import org.junit.Assert.*
import org.junit.Test

class BatteryPresentationTest {
    @Test fun onlyConfirmedFastChargingChangesChargingLabel() {
        assertEquals("Hızlı şarj oluyor", BatteryPresentation.from(50, 100, 2, true, true).status)
        assertEquals("Şarj oluyor", BatteryPresentation.from(50, 100, 2, true, false).status)
        assertEquals("Şarj oluyor", BatteryPresentation.from(50, 100, 2, true, null).status)
    }

    @Test fun fastSignalCannotOverrideFullUnpluggedOrInvalidBattery() {
        assertEquals("Şarj tamamlandı", BatteryPresentation.from(100, 100, 5, true, true).status)
        assertEquals("Pil kullanılıyor", BatteryPresentation.from(50, 100, 3, true, true).status)
        assertEquals("Pil bilgisi yok", BatteryPresentation.from(-1, 100, 2, true, true).status)
    }

    @Test fun normalizesNonHundredScale() {
        val view = BatteryPresentation.from(3, 4, 2, true)
        assertEquals("%75", view.percentage)
        assertEquals(75, view.progress)
        assertEquals("Şarj oluyor", view.status)
    }

    @Test fun distinguishesFullDischargingAndNotCharging() {
        assertEquals("Şarj tamamlandı", BatteryPresentation.from(100, 100, 5, true).status)
        assertEquals("Pil kullanılıyor", BatteryPresentation.from(60, 100, 3, true).status)
        assertEquals("Şarj olmuyor", BatteryPresentation.from(60, 100, 4, true).status)
        assertEquals("Durum bilinmiyor", BatteryPresentation.from(60, 100, 1, true).status)
    }

    @Test fun missingOrInvalidBatteryDoesNotInventPercentage() {
        listOf(
            BatteryPresentation.from(-1, 100, 2, true),
            BatteryPresentation.from(20, 0, 2, true),
            BatteryPresentation.from(101, 100, 2, true),
            BatteryPresentation.from(50, 100, 2, false)
        ).forEach {
            assertEquals("—", it.percentage)
            assertEquals(0, it.progress)
            assertEquals("Pil bilgisi yok", it.status)
        }
    }

    @Test fun handlesEmptyFullAndLargeScalesWithoutOverflow() {
        assertEquals("%0", BatteryPresentation.from(0, 100, 3, true).percentage)
        assertEquals("%100", BatteryPresentation.from(Int.MAX_VALUE, Int.MAX_VALUE, 5, true).percentage)
    }
}
