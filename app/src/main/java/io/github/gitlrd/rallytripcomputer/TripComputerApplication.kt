package io.github.gitlrd.rallytripcomputer

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Tracking is owned by the process, not by the Activity or the ViewModel, because
 * [TrackingService] keeps it running while the app is in the background — which is the
 * point of the service. The UI reads the same tracker rather than owning one.
 */
class TripComputerApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settings: Settings by lazy { Settings(this) }

    val tracker: TripTracker by lazy { TripTracker(this, scope, settings) }

    /** Owned by the process for the same reason as the tracker: a timing must outlive the
     * Activity, so unfolding the phone mid-regularity cannot reset it. */
    val stopwatches: StopwatchBank by lazy { StopwatchBank(settings) }
}
