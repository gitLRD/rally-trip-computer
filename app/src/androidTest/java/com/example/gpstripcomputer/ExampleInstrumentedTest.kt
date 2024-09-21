package com.example.gpstripcomputer

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gpstripcomputer.SpeedUnit.KILOMETERS_PER_HOUR
import com.example.gpstripcomputer.SpeedUnit.MILES_PER_HOUR

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("com.example.gpstripcomputer", appContext.packageName)
        }
    }

    @Test
    fun convertSpeed_kmhToMph_returnsCorrectValue() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val mainActivity = MainActivity()
            val speedKmh = 100f
            val expectedSpeedMph = 62.1371f
            val actualSpeedMph = mainActivity.convertSpeed(speedKmh, KILOMETERS_PER_HOUR, MILES_PER_HOUR)
            assertEquals(expectedSpeedMph, actualSpeedMph, 0.001f)
        }
    }

    @Test
    fun convertSpeed_mphToKmh_returnsCorrectValue() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val mainActivity = MainActivity()
            val speedMph = 62.1371f
            val expectedSpeedKmh = 100f
            val actualSpeedKmh = mainActivity.convertSpeed(speedMph, MILES_PER_HOUR, KILOMETERS_PER_HOUR)
            assertEquals(expectedSpeedKmh, actualSpeedKmh, 0.001f)
        }
    }

    @Test
    fun convertDistance_kmToMiles_returnsCorrectValue() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val mainActivity = MainActivity()
            val distanceKm = 10f
            val expectedDistanceMiles = 6.21371f
            val actualDistanceMiles = mainActivity.convertDistance(distanceKm, DistanceUnit.KILOMETERS, DistanceUnit.MILES)
            assertEquals(expectedDistanceMiles, actualDistanceMiles, 0.001f)
        }
    }

    @Test
    fun convertDistance_milesToKm_returnsCorrectValue() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val mainActivity = MainActivity()
            val distanceMiles = 6.21371f
            val expectedDistanceKm = 10f
            val actualDistanceKm = mainActivity.convertDistance(
                distanceMiles,
                DistanceUnit.MILES,
                DistanceUnit.KILOMETERS
            )
            assertEquals(expectedDistanceKm, actualDistanceKm, 0.001f)
        }
    }

    @Test
    fun setAndGetUnitPreference_worksCorrectly() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val mainActivity = MainActivity()

            mainActivity.setUnitPreference("imperial")
            Thread.sleep(100) // Introduce a delay of 100 milliseconds
            val retrievedUnits = mainActivity.getDistanceUnit("imperial")
            assertEquals(DistanceUnit.MILES, retrievedUnits)

            mainActivity.setUnitPreference("metric")
            Thread.sleep(100) // Introduce a delay of 100 milliseconds
            val retrievedUnits2 = mainActivity.getDistanceUnit("metric")
            assertEquals(DistanceUnit.KILOMETERS, retrievedUnits2)
        }
    }
}