package com.prenovative.tracker

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Reminder {
    private fun pending(c: Context, id: Int) = PendingIntent.getBroadcast(
        c, id, Intent(c, AlarmReceiver::class.java).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun set(c: Context, t: Task, at: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(c, t.id)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    // Done today -> tomorrow's due time. Due later today -> due time. Overdue and not done -> soon.
    fun schedule(c: Context, t: Task) {
        val now = System.currentTimeMillis()
        val due = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, t.h); set(Calendar.MINUTE, t.m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val at = when {
            t.doneOn == Store.today() -> due.timeInMillis + 24 * 60 * 60 * 1000
            due.timeInMillis > now -> due.timeInMillis
            else -> now + 60 * 1000
        }
        set(c, t, at)
    }

    fun cancel(c: Context, id: Int) {
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(c, id))
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val t = Store.load(c).firstOrNull { it.id == i.getIntExtra("id", -1) } ?: return
        if (t.doneOn == Store.today()) { Reminder.schedule(c, t); return }
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("tasks", "Task reminders", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            t.id,
            Notification.Builder(c, "tasks")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Task not done yet")
                .setContentText(t.title)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
        // Keep nagging every 10 minutes until it is ticked off
        Reminder.set(c, t, System.currentTimeMillis() + 10 * 60 * 1000)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Store.load(c).forEach { Reminder.schedule(c, it) }
    }
}
