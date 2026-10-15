package dev.octrow.meetcaptions

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Silent "recording" notice while captions are saved; it goes away on its own once the call is quiet. */
class RecordingNotice(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java).apply {
        createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_recording), NotificationManager.IMPORTANCE_LOW),
        )
    }

    /** [lastLine] stays off the lock screen: it is what someone said in the call. */
    fun show(lastLine: String) {
        if (!manager.areNotificationsEnabled()) return
        val notice = builder()
            .setContentText(lastLine)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(builder().build())
            .setTimeoutAfter(QUIET_TIMEOUT_MS)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(ID, notice)
    }

    fun cancel() = manager.cancel(ID)

    private fun builder(): Notification.Builder {
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(R.color.ic_launcher_background))
            .setContentTitle(context.getString(R.string.notice_recording))
            .setContentIntent(open)
            .setCategory(Notification.CATEGORY_STATUS)
    }

    private companion object {
        const val CHANNEL = "recording"
        const val ID = 1
        const val QUIET_TIMEOUT_MS = 60_000L
    }
}
