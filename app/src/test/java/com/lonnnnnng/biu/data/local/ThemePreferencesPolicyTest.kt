package com.lonnnnnng.biu.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesPolicyTest {
    @Test
    fun themeDefaultsToSystemMode() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStoredValue(null))
    }

    @Test
    fun themeRestoresEverySupportedMode() {
        AppThemeMode.entries.forEach { mode ->
            assertEquals(mode, AppThemeMode.fromStoredValue(mode.name))
        }
    }

    @Test
    fun invalidThemeValueFallsBackToSystemMode() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStoredValue("UNKNOWN"))
    }
}
