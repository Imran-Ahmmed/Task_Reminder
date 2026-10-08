package com.dailypush.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ---------- বাংলা সংখ্যা ও তারিখ ----------

fun String.bnDigits(): String =
    map { if (it in '0'..'9') ('০' + (it - '0')) else it }.joinToString("")

fun Int.bn(): String = toString().bnDigits()

fun minutesText(m: Int): String {
    if (m <= 0) return "০ মিনিট"
    val h = m / 60
    val r = m % 60
    return when {
        h == 0 -> "${r.bn()} মিনিট"
        r == 0 -> "${h.bn()} ঘণ্টা"
        else -> "${h.bn()} ঘণ্টা ${r.bn()} মিনিট"
    }
}

private val BN_DAYS = listOf("সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার", "রবিবার")
private val BN_MONTHS = listOf(
    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
)

fun dateText(d: LocalDate): String =
    "${BN_DAYS[d.dayOfWeek.value - 1]}, ${d.dayOfMonth.bn()} ${BN_MONTHS[d.monthValue - 1]} ${d.year.bn()}"

fun timeText(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    val period = when {
        h < 5 -> "রাত"
        h < 12 -> "সকাল"
        h < 15 -> "দুপুর"
        h < 18 -> "বিকাল"
        h < 20 -> "সন্ধ্যা"
        else -> "রাত"
    }
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$period ${h12.bn()}:${"%02d".format(m).bnDigits()}"
}

// ---------- মডেল ----------

data class Task(
    val id: Long,
    val title: String,
    val date: String,
    val done: Boolean,
    val minutes: Int
)

data class Badge(val days: Int, val emoji: String, val name: String, val desc: String)

class Celebration(
    val coins: Int,
    val streak: Int,
    val badge: Badge?,
    val badgeBonus: Int,
    val shieldEarned: Boolean
)

object Rules {
    val badges = listOf(
        Badge(1, "💥", "মহাবিস্ফোরণ", "যাত্রা শুরু! প্রথম দিনের সব কাজ শেষ।"),
        Badge(3, "✨", "প্রথম স্ফুলিঙ্গ", "টানা ৩ দিন — অভ্যাসের বীজ বোনা হলো।"),
        Badge(7, "⚡", "ঝড়ের গতি", "পুরো এক সপ্তাহ অবিচল!"),
        Badge(14, "⚔️", "অদম্য যোদ্ধা", "দুই সপ্তাহ — কেউ তোমাকে থামাতে পারছে না।"),
        Badge(21, "🔩", "লৌহ সংকল্প", "২১ দিন — অভ্যাস এখন রক্তে মিশে যাচ্ছে।"),
        Badge(30, "👑", "মাসের সম্রাট", "পুরো এক মাস! তুমিই রাজা।"),
        Badge(45, "🔥", "অগ্নিপুরুষ", "৪৫ দিনের আগুন সহজে নেভে না।"),
        Badge(60, "🌟", "ধ্রুবতারা", "৬০ দিন — তুমি এখন অন্যদের পথ দেখাও।"),
        Badge(100, "🏆", "শতদিনের সেনাপতি", "১০০ দিন! এটা কিংবদন্তির শুরু।"),
        Badge(150, "☀️", "সূর্যজয়ী", "১৫০ দিন — অন্ধকার তোমার কাছে হার মেনেছে।"),
        Badge(200, "🚀", "মহাকাশচারী", "২০০ দিন — তুমি আকাশ ছাড়িয়ে গেছ।"),
        Badge(365, "🐉", "কিংবদন্তি ড্রাগন", "পূর্ণ এক বছর! তুমি অপ্রতিরোধ্য।")
    )

    // এই স্ট্রিকে পৌঁছালে একটি করে শিল্ড পাওয়া যাবে
    val shieldDays = setOf(7, 14, 30, 45, 60, 100, 150, 200, 250, 300, 365)

    const val MAX_SHIELDS = 3
    const val BADGE_BONUS = 50

    fun dayReward(streak: Int): Int = 10 + minOf(streak, 20)
}

// ---------- অ্যাপের স্টেট ও ডেটাবেস (ফোনের ভেতরেই সেভ হয়) ----------

class AppState(context: Context) {

    private val ctx: Context = context.applicationContext
    private val prefs = ctx.getSharedPreferences(Reminders.PREFS, Context.MODE_PRIVATE)

    var today by mutableStateOf(LocalDate.now())
        private set
    var tasks by mutableStateOf<List<Task>>(emptyList())
        private set
    var coins by mutableIntStateOf(0)
        private set
    var streak by mutableIntStateOf(0)
        private set
    var longestStreak by mutableIntStateOf(0)
        private set
    var shields by mutableIntStateOf(0)
        private set
    var totalMinutes by mutableIntStateOf(0)
        private set
    var totalDays by mutableIntStateOf(0)
        private set
    var totalTasksDone by mutableIntStateOf(0)
        private set
    var lastCompleted by mutableStateOf<String?>(null)
        private set
    var awarded by mutableStateOf<Set<String>>(emptySet())
        private set
    var earnedBadges by mutableStateOf<Set<Int>>(emptySet())
        private set

    var remindersOn by mutableStateOf(true)
        private set
    var reminderTimes by mutableStateOf<List<Int>>(emptyList())
        private set

    var notice by mutableStateOf<String?>(null)
        private set
    var celebration by mutableStateOf<Celebration?>(null)
        private set

    init {
        load()
    }

    val todayTasks: List<Task>
        get() = tasks.filter { it.date == today.toString() }

    val doneToday: Boolean
        get() = today.toString() in awarded

    val streakAtRisk: Boolean
        get() = !doneToday && streak > 0 && lastCompleted == today.minusDays(1).toString()

    // ----- লোড / সেভ -----

    private fun load() {
        today = LocalDate.now()
        coins = prefs.getInt("coins", 0)
        streak = prefs.getInt("streak", 0)
        longestStreak = prefs.getInt("longest", 0)
        shields = prefs.getInt("shields", 0)
        totalMinutes = prefs.getInt("total_minutes", 0)
        totalDays = prefs.getInt("total_days", 0)
        totalTasksDone = prefs.getInt("total_tasks_done", 0)
        lastCompleted = prefs.getString("last_completed", null)
        awarded = (prefs.getString("awarded", "") ?: "")
            .split(",").filter { it.isNotBlank() }.toSet()
        earnedBadges = (prefs.getString("badges", "") ?: "")
            .split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

        val cut = today.minusDays(90)
        tasks = tasksFromJson(prefs.getString("tasks", "[]")).filter {
            runCatching { !LocalDate.parse(it.date).isBefore(cut) }.getOrDefault(false)
        }

        remindersOn = Reminders.enabled(ctx)
        reminderTimes = Reminders.times(ctx)
    }

    private fun save() {
        prefs.edit()
            .putInt("coins", coins)
            .putInt("streak", streak)
            .putInt("longest", longestStreak)
            .putInt("shields", shields)
            .putInt("total_minutes", totalMinutes)
            .putInt("total_days", totalDays)
            .putInt("total_tasks_done", totalTasksDone)
            .putString("last_completed", lastCompleted)
            .putString("awarded", awarded.joinToString(","))
            .putString("badges", earnedBadges.joinToString(","))
            .putString("tasks", tasksToJson(tasks))
            .apply()
    }

    /** অ্যাপ খুললে বা দিন বদলালে ডাকা হয়: ফোন থেকে ডেটা পড়ে স্ট্রিক ঠিকঠাক করে */
    fun refresh() {
        load()
        settle()
    }

    /** মিস হওয়া দিনের হিসাব: শিল্ড থাকলে স্ট্রিক বাঁচবে, না থাকলে স্ট্রিক শূন্য */
    private fun settle() {
        if (streak <= 0) return
        val last = lastCompleted?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val missed = (ChronoUnit.DAYS.between(last, today) - 1).toInt()
        if (missed <= 0) return

        if (shields >= missed) {
            shields -= missed
            lastCompleted = today.minusDays(1).toString()
            notice = "🛡️ ${missed.bn()} দিন কাজ মিস হয়েছিল!\n${missed.bn()}টি শিল্ড খরচ করে তোমার ${streak.bn()} দিনের স্ট্রিক বাঁচানো হয়েছে।\nআজ থেকে আবার ঝাঁপিয়ে পড়ো 💪"
        } else {
            val old = streak
            streak = 0
            notice = "😢 ${missed.bn()} দিন কাজ মিস হওয়ায় তোমার ${old.bn()} দিনের স্ট্রিক শেষ হয়ে গেছে।\nকোনো ব্যাপার না — আজই নতুন করে শুরু করো! 🚀"
        }
        save()
    }

    fun clearNotice() {
        notice = null
    }

    fun dismissCelebration() {
        celebration = null
    }

    // ----- কাজ -----

    fun addTask(title: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        tasks = tasks + Task(System.currentTimeMillis(), t, today.toString(), false, 0)
        save()
    }

    fun toggleTask(id: Long, done: Boolean) {
        val old = tasks.find { it.id == id } ?: return
        if (old.done == done) return
        tasks = tasks.map { if (it.id == id) it.copy(done = done) else it }
        totalTasksDone = if (done) totalTasksDone + 1 else maxOf(0, totalTasksDone - 1)
        checkDay()
        save()
    }

    fun deleteTask(id: Long) {
        tasks = tasks.filter { it.id != id }
        checkDay()
        save()
    }

    fun addMinutes(id: Long, minutes: Int) {
        if (minutes <= 0) return
        val m = minOf(minutes, 1440)
        if (tasks.none { it.id == id }) return
        tasks = tasks.map { if (it.id == id) it.copy(minutes = it.minutes + m) else it }
        totalMinutes += m
        save()
    }

    // ----- দিন সম্পূর্ণ হলে পুরস্কার -----

    private fun checkDay() {
        val todays = todayTasks
        if (todays.isNotEmpty() && todays.all { it.done } && !doneToday) {
            award()
        }
    }

    private fun award() {
        val t = today.toString()
        val newStreak = if (lastCompleted == today.minusDays(1).toString()) streak + 1 else 1
        streak = newStreak
        longestStreak = maxOf(longestStreak, newStreak)
        lastCompleted = t
        awarded = awarded + t
        totalDays += 1

        val gained = Rules.dayReward(newStreak)

        val badge = Rules.badges.firstOrNull { it.days == newStreak && it.days !in earnedBadges }
        var bonus = 0
        if (badge != null) {
            earnedBadges = earnedBadges + badge.days
            bonus = Rules.BADGE_BONUS
        }

        var shieldGot = false
        if (newStreak in Rules.shieldDays && shields < Rules.MAX_SHIELDS) {
            shields += 1
            shieldGot = true
        }

        coins += gained + bonus
        celebration = Celebration(gained, newStreak, badge, bonus, shieldGot)
    }

    // ----- রিমাইন্ডার সেটিংস -----

    fun switchReminders(on: Boolean) {
        Reminders.setEnabled(ctx, on)
        remindersOn = on
        Reminders.scheduleAll(ctx)
    }

    fun addReminder(minuteOfDay: Int) {
        if (minuteOfDay in reminderTimes) return
        val list = (reminderTimes + minuteOfDay).sorted()
        Reminders.saveTimes(ctx, list)
        reminderTimes = list
        Reminders.scheduleAll(ctx)
    }

    fun removeReminder(minuteOfDay: Int) {
        val list = reminderTimes.filter { it != minuteOfDay }
        Reminders.saveTimes(ctx, list)
        reminderTimes = list
        Reminders.scheduleAll(ctx)
    }
}

// ---------- JSON সাহায্যকারী ----------

private fun tasksToJson(list: List<Task>): String {
    val arr = JSONArray()
    list.forEach {
        arr.put(
            JSONObject()
                .put("id", it.id)
                .put("title", it.title)
                .put("date", it.date)
                .put("done", it.done)
                .put("minutes", it.minutes)
        )
    }
    return arr.toString()
}

private fun tasksFromJson(s: String?): List<Task> {
    if (s.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(s)
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Task(
                id = o.getLong("id"),
                title = o.getString("title"),
                date = o.getString("date"),
                done = o.optBoolean("done", false),
                minutes = o.optInt("minutes", 0)
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
