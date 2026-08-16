package io.github.gitlrd.gpstripcomputer

import android.Manifest
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Trip data used to live in the Activity, so rotating the device threw away the journey so
 * far and restarted tracking. It now lives in a ViewModel; this pins that down.
 */
@RunWith(AndroidJUnit4::class)
class ConfigurationChangeTest {

    @Before
    fun grantLocationPermission() {
        // Otherwise launching the Activity raises the runtime permission dialog and blocks.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    @Test
    fun theViewModelAndItsTripsSurviveRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var before: TripComputerViewModel
            scenario.onActivity {
                before = ViewModelProvider(it)[TripComputerViewModel::class.java]
            }
            before.onUnitSystemSelected(UnitSystem.IMPERIAL)

            // Let the tracker's tick put something on the clock, so that a reset would show.
            val elapsedBefore = awaitElapsedTime(before)
            assertTrue("expected the tracker to have started ticking", elapsedBefore > 0)

            scenario.recreate()

            lateinit var after: TripComputerViewModel
            scenario.onActivity {
                after = ViewModelProvider(it)[TripComputerViewModel::class.java]
            }

            // Same instance means neither the trips nor the tracking were torn down.
            assertSame(before, after)
            // Tracking carries on across the recreation, so elapsed time may have advanced
            // further. What matters is that it did not go back to zero.
            assertTrue(
                "elapsed time went backwards across recreation",
                after.trips[0].elapsedMillis >= elapsedBefore
            )
            assertEquals(UnitSystem.IMPERIAL, after.unitSystem)
        }
    }

    /** Waits for the one-second tick to register, returning the elapsed time it saw. */
    private fun awaitElapsedTime(
        viewModel: TripComputerViewModel,
        timeoutMillis: Long = 5_000
    ): Long {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            val elapsed = viewModel.trips[0].elapsedMillis
            if (elapsed > 0) return elapsed
            Thread.sleep(100)
        }
        return viewModel.trips[0].elapsedMillis
    }

    @Test
    fun settingsChangedBeforeRecreationAreStillApplied() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var viewModel: TripComputerViewModel
            scenario.onActivity {
                viewModel = ViewModelProvider(it)[TripComputerViewModel::class.java]
            }
            viewModel.onIncludeStoppedTimeChanged(false)

            scenario.recreate()

            scenario.onActivity {
                val after = ViewModelProvider(it)[TripComputerViewModel::class.java]
                assertEquals(false, after.includeStoppedTime)
            }
        }
    }
}
