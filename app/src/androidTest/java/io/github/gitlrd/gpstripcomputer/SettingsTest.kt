package io.github.gitlrd.gpstripcomputer

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Settings need a real [Context], so these run on device. The conversion and trip maths
 * are plain Kotlin and are covered by the much faster JVM tests instead.
 */
@RunWith(AndroidJUnit4::class)
class SettingsTest {

    private lateinit var settings: Settings

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences(Settings.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        settings = Settings(context)
    }

    @Test
    fun defaultsAreMetricAndIncludeStoppedTime() {
        assertEquals(UnitSystem.METRIC, settings.unitSystem)
        assertTrue(settings.includeStoppedTime)
    }

    @Test
    fun unitSystemSurvivesARoundTrip() {
        settings.unitSystem = UnitSystem.IMPERIAL
        assertEquals(UnitSystem.IMPERIAL, settings.unitSystem)

        settings.unitSystem = UnitSystem.METRIC
        assertEquals(UnitSystem.METRIC, settings.unitSystem)
    }

    @Test
    fun includeStoppedTimeSurvivesARoundTrip() {
        settings.includeStoppedTime = false
        assertFalse(settings.includeStoppedTime)

        settings.includeStoppedTime = true
        assertTrue(settings.includeStoppedTime)
    }

    @Test
    fun aSecondInstanceSeesTheSameStoredValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        settings.unitSystem = UnitSystem.IMPERIAL
        settings.includeStoppedTime = false

        val reopened = Settings(context)
        assertEquals(UnitSystem.IMPERIAL, reopened.unitSystem)
        assertFalse(reopened.includeStoppedTime)
    }
}
