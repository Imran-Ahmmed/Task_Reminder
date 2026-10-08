package com.dailypush.app

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar

object Reminders {
    const val PREFS = "daily_push"
    const val KEY_ON = "rem_on"
    const val KEY_TIMES = "rem_times"
    private const val KEY_CODES = "rem_codes"
    const val CHANNEL = "daily_push_reminders"
    const val ACTION = "com.dailypush.app.REMIND"

    // ডিফল্ট: সন্ধ্যা ৬:০০, রাত ৯:০০, রাত ১১:৩০ (মিনিটে)
    private const val DEFAULT_TIMES = "1080,1260,1410"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ON, true)

    fun setEnabled(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ON, on).apply()
    }

    fun times(ctx: Context): List<Int> =
        (prefs(ctx).getString(KEY_TIMES, DEFAULT_TIMES) ?: "")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0..1439 }
            .distinct()
            .sorted()

    fun saveTimes(ctx: Context, list: List<Int>) {
        prefs(ctx).edit().putString(KEY_TIMES, list.joinToString(",")).apply()
    }

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL, "দৈনিক রিমাইন্ডার", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "আজকের কাজ বাকি থাকলে স্ট্রিক হারানোর আগে মনে করিয়ে দেয়"
        nm.createNotificationChannel(ch)
    }

    private fun pending(ctx: Context, minute: Int): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION)
            .putExtra("minute", minute)
        return PendingIntent.getBroadcast(
            ctx,
            1000 + minute,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleAll(ctx: Context) {
        cancelAll(ctx)
        if (!enabled(ctx)) return
        val list = times(ctx)
        list.forEach { scheduleOne(ctx, it) }
        prefs(ctx).edit().putString(KEY_CODES, list.joinToString(",")).apply()
    }

    private fun cancelAll(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        (prefs(ctx).getString(KEY_CODES, "") ?: "")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .forEach { am.cancel(pending(ctx, it)) }
        prefs(ctx).edit().remove(KEY_CODES).apply()
    }

    /** পরবর্তী যে সময়টা আসবে (আজ বা কাল) সেই সময়ের জন্য অ্যালার্ম বসায় */
    fun scheduleOne(ctx: Context, minute: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minute / 60)
            set(Calendar.MINUTE, minute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis() + 1000) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(ctx, minute))
    }

    /** অ্যালার্ম বাজলে: আজকের কাজ বাকি থাকলে নোটিফিকেশন দেখায় */
    fun fire(ctx: Context) {
        val s = AppState(ctx)
        if (s.doneToday) return

        val todays = s.todayTasks
        val pendingCount = todays.count { !it.done }
        val risk = s.streakAtRisk

        val msg: Pair<String, String> = if (todays.isEmpty()) {
            if (risk) {
                Pair(
                    "⏰ আজকের কাজ এখনও যোগ করোনি!",
                    "🔥 ${s.streak.bn()} দিনের স্ট্রিক হারাতে বসেছ। এখনই কাজ যোগ করে শেষ করো!"
                )
            } else {
                Pair("🚀 আজকের কাজ যোগ করো", "আজই নতুন স্ট্রিক শুরু করার সেরা সময়।")
            }
        } else {
            if (risk) {
                Pair(
                    "⏰ ${pendingCount.bn()}টি কাজ এখনও বাকি!",
                    "আজ শেষ না করলে তোমার ${s.streak.bn()} দিনের স্ট্রিক হারাবে 🔥"
                )
            } else {
                Pair("⏰ ${pendingCount.bn()}টি কাজ বাকি", "শেষ করে আজকের স্ট্রিক নিশ্চিত করো 💪")
            }
        }
        show(ctx, msg.first, msg.second)
    }

    fun showTest(ctx: Context): Boolean =
        show(ctx, "✅ টেস্ট সফল!", "রিমাইন্ডার ঠিকমতো কাজ করছে। কাজ বাকি থাকলে এভাবেই মনে করিয়ে দেব 🔔")

    private fun show(ctx: Context, title: String, text: String): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false

        ensureChannel(ctx)

        val open = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        ctx.getSystemService(NotificationManager::class.java).notify(1001, n)
        return true
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val minute = intent.getIntExtra("minute", -1)
        if (minute < 0) return
        if (!Reminders.enabled(context) || minute !in Reminders.times(context)) return
        // আগে পরের দিনের অ্যালার্ম বসাই, তারপর নোটিফিকেশন
        Reminders.scheduleOne(context, minute)
        Reminders.fire(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.scheduleAll(context)
    }
}
