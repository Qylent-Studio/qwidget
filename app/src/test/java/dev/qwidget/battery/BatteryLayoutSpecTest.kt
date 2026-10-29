package dev.qwidget.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryLayoutSpecTest {
    @Test fun shortHeightKeepsPercentageAndMovesStatusToAccessibleDescription() {
        val spec = BatteryLayoutSpec.fromSize(110, 40)
        assertFalse(spec.showStatus)
        assertTrue(spec.titleSp * 1.25f <= 40 - spec.verticalPaddingDp * 2)
    }

    @Test fun largeFontsAtMinimumWidthDoNotClipPercentage() {
        val spec = BatteryLayoutSpec.fromSize(70, 48, 2f)
        assertFalse(spec.showStatus)
        assertTrue(spec.titleSp * 2f * 3f <= 70 - spec.paddingDp * 2)
        assertTrue(spec.titleSp * 2f * 1.25f <= 48 - spec.verticalPaddingDp * 2)
    }

    @Test fun enlargedWidgetRestoresSecondaryContentAtLargeFont() {
        val spec = BatteryLayoutSpec.fromSize(320, 220, 2f)
        assertTrue(spec.showStatus)
        assertTrue(spec.showProgress)
    }

    @Test fun minimumCellUsesCompactTextAndPadding() {
        val spec = BatteryLayoutSpec.fromSize(70, 48)
        assertEquals(6, spec.paddingDp)
        assertEquals(18f, spec.titleSp)
        assertEquals(10f, spec.bodySp)
        assertEquals(2, spec.progressHeightDp)
    }

    @Test fun eitherConstrainedDimensionKeepsLayoutCompact() {
        assertEquals(18f, BatteryLayoutSpec.fromSize(70, 240).titleSp)
        assertEquals(18f, BatteryLayoutSpec.fromSize(320, 48).titleSp)
        assertEquals(18f, BatteryLayoutSpec.fromSize(139, 96).titleSp)
        assertEquals(18f, BatteryLayoutSpec.fromSize(160, 95).titleSp)
    }

    @Test fun normalSizeRestoresStandardTypography() {
        val spec = BatteryLayoutSpec.fromSize(140, 96)
        assertEquals(16, spec.paddingDp)
        assertEquals(28f, spec.titleSp)
        assertEquals(12f, spec.bodySp)
        assertEquals(4, spec.progressHeightDp)
    }

    @Test fun largerTextRequiresBothDimensions() {
        assertEquals(36f, BatteryLayoutSpec.fromSize(200, 140).titleSp)
        assertEquals(40f, BatteryLayoutSpec.fromSize(280, 180).titleSp)
        assertEquals(28f, BatteryLayoutSpec.fromSize(199, 180).titleSp)
        assertEquals(28f, BatteryLayoutSpec.fromSize(280, 139).titleSp)
        assertEquals(36f, BatteryLayoutSpec.fromSize(279, 180).titleSp)
        assertEquals(36f, BatteryLayoutSpec.fromSize(280, 179).titleSp)
    }

    @Test fun missingOrInvalidOptionsUseSafeCompactFallback() {
        assertEquals(18f, BatteryLayoutSpec.fromSize(0, 0).titleSp)
        assertEquals(18f, BatteryLayoutSpec.fromSize(-1, 200).titleSp)
        assertEquals(18f, BatteryLayoutSpec.fromSize(300, -1).titleSp)
    }
}
