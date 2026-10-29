package dev.qwidget.battery

import org.junit.Assert.*
import org.junit.Test

class OppoFastChargeStateTest {
    @Test fun acceptsLiveMatchingOppoReportAndAllowsReturnToNormal() {
        val state = OppoFastChargeState()
        state.batteryChanged(true, 1)
        state.additionalBatteryChanged(true, 1, true, false)
        assertEquals(true, state.fastCharging)
        state.additionalBatteryChanged(true, 1, false, false)
        assertEquals(false, state.fastCharging)
    }

    @Test fun missingAndInitialStickyDataDoNotClaimFastCharging() {
        val state = OppoFastChargeState()
        state.batteryChanged(true, 1)
        state.additionalBatteryChanged(true, 1, true, true)
        assertNull(state.fastCharging)
        state.additionalBatteryChanged(true, 1, null, false)
        assertNull(state.fastCharging)
    }

    @Test fun differentChargerOrNonChargingReportCannotEnableFastLabel() {
        val state = OppoFastChargeState()
        state.batteryChanged(true, 2)
        state.additionalBatteryChanged(true, 1, true, false)
        assertNull(state.fastCharging)
        state.additionalBatteryChanged(false, 2, true, false)
        assertNull(state.fastCharging)
    }

    @Test fun unplugAndPowerSourceChangeClearOldFastSignal() {
        val state = OppoFastChargeState()
        state.batteryChanged(true, 1)
        state.additionalBatteryChanged(true, 1, true, false)
        state.batteryChanged(false, 0)
        assertNull(state.fastCharging)
        state.batteryChanged(true, 1)
        assertNull(state.fastCharging)
        state.additionalBatteryChanged(true, 1, true, false)
        state.batteryChanged(true, 2)
        assertNull(state.fastCharging)
    }
}
