package com.lonnnnnng.biu.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayPreferencesPolicyTest {
    @Test
    fun listDensityDefaultsToStandard() {
        assertEquals(AppListDensity.STANDARD, AppListDensity.fromStoredValue(null))
    }

    @Test
    fun listDensityRestoresEverySupportedValue() {
        AppListDensity.entries.forEach { density ->
            assertEquals(density, AppListDensity.fromStoredValue(density.name))
        }
    }

    @Test
    fun invalidListDensityFallsBackToStandard() {
        assertEquals(AppListDensity.STANDARD, AppListDensity.fromStoredValue("UNSUPPORTED"))
    }

    @Test
    fun textScaleDefaultsToStandard() {
        assertEquals(AppTextScale.STANDARD, AppTextScale.fromStoredValue(null))
    }

    @Test
    fun textScaleRestoresEverySupportedValue() {
        AppTextScale.entries.forEach { scale ->
            assertEquals(scale, AppTextScale.fromStoredValue(scale.name))
        }
    }

    @Test
    fun invalidTextScaleFallsBackToStandard() {
        assertEquals(AppTextScale.STANDARD, AppTextScale.fromStoredValue("UNSUPPORTED"))
    }

    @Test
    fun videoLayoutDefaultsToList() {
        assertEquals(AppVideoLayout.LIST, AppVideoLayout.fromStoredValue(null))
    }

    @Test
    fun videoLayoutRestoresEverySupportedValue() {
        AppVideoLayout.entries.forEach { layout ->
            assertEquals(layout, AppVideoLayout.fromStoredValue(layout.name))
        }
    }

    @Test
    fun invalidVideoLayoutFallsBackToList() {
        assertEquals(AppVideoLayout.LIST, AppVideoLayout.fromStoredValue("UNSUPPORTED"))
    }
}
