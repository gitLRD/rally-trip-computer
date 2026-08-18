package io.github.gitlrd.rallytripcomputer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

private const val CHANNEL_ID = "tracking"
private const val NOTIFICATION_ID = 1
private const val NOTIFICATION_REFRESH_MILLIS = 2000L

/**
 * Keeps tracking alive with the app in the background, so switching to a map or taking a
 * call mid-rally does not stop the trip meters — and so the screen no longer has to be
 * held on just to keep recording.
 *
 * The service does not own any trip state; it starts and stops the process-wide tracker
 * and mirrors it into a notification.
 */
class TrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationJob: Job? = null

    private val tracker: TripTracker
        get() = (application as TripComputerApplication).tracker

    private val settings: Settings
        get() = (application as TripComputerApplication).settings

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // Stopping from the notification is as deliberate as using the drawer toggle,
            // so it must not be resumed on next launch.
            settings.trackingEnabled = false
            stopSelf()
            return START_NOT_STICKY
        }

        createChannel()
        startInForeground(buildNotification())
        tracker.start()

        notificationJob?.cancel()
        notificationJob = scope.launch {
            while (isActive) {
                delay(NOTIFICATION_REFRESH_MILLIS)
                getSystemService(NotificationManager::class.java)
                    ?.notify(NOTIFICATION_ID, buildNotification())
            }
        }

        // Restart if killed: a rally in progress should survive memory pressure.
        return START_STICKY
    }

    override fun onDestroy() {
        notificationJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        tracker.stop()
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.tracking_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val units = settings.unitSystem
        val trip = tracker.trips.firstOrNull() ?: Trip()

        val distance = String.format(
            Locale.getDefault(),
            "%.2f %s",
            metresTo(trip.distanceMetres, units.distanceUnit),
            units.distanceUnit.abbreviation
        )
        val speed = String.format(
            Locale.getDefault(),
            "%.2f %s",
            metresPerSecondTo(tracker.currentSpeedMps, units.speedUnit),
            units.speedUnit.abbreviation
        )

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, TrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tracking)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text, distance, speed))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(open)
            .addAction(0, getString(R.string.stop), stop)
            .build()
    }

    companion object {
        const val ACTION_STOP = "io.github.gitlrd.rallytripcomputer.STOP"

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrackingService::class.java))
        }
    }
}
