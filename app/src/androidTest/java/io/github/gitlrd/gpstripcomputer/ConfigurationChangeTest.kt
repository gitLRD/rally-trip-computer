package io.github.gitlrd.gpstripcomputer

import android.Manifest
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Trip data used to live in the Activity, so rotating — and, more to the point, unfolding a
 * foldable — threw away the journey so far and restarted tracking. It now lives in the
 * Application, with display settings in a ViewModel. This pins both down.
 */
@RunWith(AndroidJUnit4::class)
class ConfigurationChangeTest {

    /**
     * GrantPermissionRule rather than UiAutomation.grantRuntimePermission, which only
     * exists from API 28 and threw NoSuchMethodError on the API 24 leg of CI.
     */
    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Before
    fun clearSettings() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences(Settings.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun theViewModelSurvivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var before: TripComputerViewModel
            scenario.onActivity {
                before = ViewModelProvider(it)[TripComputerViewModel::class.java]
            }
            before.onUnitSystemSelected(UnitSystem.METRIC)
            before.onThemeModeSelected(ThemeMode.NIGHT)

            scenario.recreate()

            lateinit var after: TripComputerViewModel
            scenario.onActivity {
                after = ViewModelProvider(it)[TripComputerViewModel::class.java]
            }

            assertSame(before, after)
            assertEquals(UnitSystem.METRIC, after.unitSystem)
            assertEquals(ThemeMode.NIGHT, after.themeMode)
        }
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

    /**
     * The tracker is process-scoped precisely so the foreground service can keep it running
     * with no Activity present. Recreating the Activity must not discard its trips.
     */
    @Test
    fun theTrackerOutlivesTheActivity() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TripComputerApplication
        val tracker = application.tracker

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val tripsBefore = tracker.trips

            scenario.recreate()

            assertSame(tracker, application.tracker)
            assertEquals(tripsBefore, application.tracker.trips)
        }
    }

    /** A casual launch must not silently begin background location recording. */
    @Test
    fun launchingDoesNotStartTrackingUnlessItWasLeftOn() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                val viewModel = ViewModelProvider(it)[TripComputerViewModel::class.java]
                assertEquals(false, viewModel.shouldResumeTracking)
            }
        }
    }
}
