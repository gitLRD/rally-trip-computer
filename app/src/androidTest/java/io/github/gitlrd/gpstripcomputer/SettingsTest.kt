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
 * Settings need a real [Context], so these run on device. The conversion, trip and codec
 * logic is plain Kotlin and is covered by the much faster JVM tests instead.
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
    fun defaultsSuitRallyUse() {
        // Imperial: UK road-rally roadbooks are in miles.
        assertEquals(UnitSystem.IMPERIAL, settings.unitSystem)
        // Stops count towards the average by default.
        assertTrue(settings.includeStoppedTime)
        // Nothing tracks until asked.
        assertFalse(settings.trackingEnabled)
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(BRIGHTNESS_FOLLOW_SYSTEM, settings.screenBrightness, 0.0f)
    }

    @Test
    fun everySettingSurvivesARoundTrip() {
        settings.unitSystem = UnitSystem.METRIC
        settings.includeStoppedTime = false
        settings.trackingEnabled = true
        settings.themeMode = ThemeMode.NIGHT
        settings.screenBrightness = 0.25f

        assertEquals(UnitSystem.METRIC, settings.unitSystem)
        assertFalse(settings.includeStoppedTime)
        assertTrue(settings.trackingEnabled)
        assertEquals(ThemeMode.NIGHT, settings.themeMode)
        assertEquals(0.25f, settings.screenBrightness, 1e-6f)
    }

    @Test
    fun aSecondInstanceSeesTheSameStoredValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        settings.unitSystem = UnitSystem.METRIC
        settings.themeMode = ThemeMode.NIGHT

        val reopened = Settings(context)

        assertEquals(UnitSystem.METRIC, reopened.unitSystem)
        assertEquals(ThemeMode.NIGHT, reopened.themeMode)
    }

    /** A rally must survive the process being reclaimed between stages. */
    @Test
    fun tripsArePersistedAndRestored() {
        val trips = listOf(
            Trip(distanceMetres = 8_046.7, elapsedMillis = 1_800_000, movingMillis = 1_500_000, maxSpeedMps = 26.8),
            Trip(distanceMetres = 402.3, elapsedMillis = 90_000, movingMillis = 90_000, maxSpeedMps = 13.4)
        )
        settings.saveTrips(trips)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(trips, Settings(context).loadTrips(expectedCount = 2))
    }

    @Test
    fun noStoredTripsLoadsEmptyOnes() {
        assertEquals(listOf(Trip(), Trip()), settings.loadTrips(expectedCount = 2))
    }
}
