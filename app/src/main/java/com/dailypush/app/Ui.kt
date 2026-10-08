package com.dailypush.app

import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate

// ---------- রং ও থিম ----------

val Bg = Color(0xFF12112B)
val CardBg = Color(0xFF1E1C3F)
val Orange = Color(0xFFFF8A3D)
val Gold = Color(0xFFFFC857)
val Muted = Color(0xFFB4B1D6)
val Green = Color(0xFF4ADE80)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Orange,
            onPrimary = Color.Black,
            secondary = Gold,
            background = Bg,
            onBackground = Color.White,
            surface = CardBg,
            onSurface = Color.White,
            surfaceVariant = CardBg,
            onSurfaceVariant = Muted,
            outline = Color(0xFF5A5790)
        ),
        content = content
    )
}

class Actions(
    val testNotification: () -> Unit,
    val openAppSettings: () -> Unit
)

// ---------- মূল স্ক্রিন ----------

@Composable
fun DailyPushApp(state: AppState, actions: Actions) {
    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    // অ্যাপ খোলা থাকা অবস্থায় রাত ১২টা পেরোলে নতুন দিনের জন্য রিফ্রেশ
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            if (LocalDate.now() != state.today) state.refresh()
        }
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = CardBg) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("📋", fontSize = 22.sp) },
                    label = { Text("আজ") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("👤", fontSize = 22.sp) },
                    label = { Text("প্রোফাইল") }
                )
            }
        }
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            if (tab == 0) {
                TodayScreen(state, onSettings = { showSettings = true })
            } else {
                ProfileScreen(state)
            }

            state.celebration?.let { c ->
                CelebrationOverlay(c, onDone = { state.dismissCelebration() })
            }
        }
    }

    if (showSettings) {
        SettingsDialog(state, actions, onClose = { showSettings = false })
    }

    state.notice?.let { msg ->
        AlertDialog(
            onDismissRequest = { state.clearNotice() },
            confirmButton = { TextButton(onClick = { state.clearNotice() }) { Text("ঠিক আছে") } },
            title = { Text("খবর আছে!") },
            text = { Text(msg) }
        )
    }
}

// ---------- অনুপ্রেরণার ছবি (পাহাড়ের চূড়ায় পতাকা, সূর্যোদয়) ----------

private val Quotes = listOf(
    "আজকের কাজ আজই শেষ করো — কালকের তুমি ধন্যবাদ দেবে।",
    "ছোট ছোট পদক্ষেপই একদিন পাহাড় জয় করে।",
    "থামলে চলবে না, চলতে চলতেই পথ তৈরি হয়।",
    "নিজের সঙ্গে করা প্রতিশ্রুতিটাই সবচেয়ে বড় প্রতিশ্রুতি।",
    "স্ট্রিক ভাঙতে দিও না — আজই শেষ করো!",
    "ইচ্ছা থাকলে উপায় হয়, অভ্যাস থাকলে সাফল্য আসে।",
    "প্রতিদিন একটু একটু এগোলেই অনেক দূর যাওয়া যায়।"
)

private val Stars = listOf(
    0.08f to 0.10f, 0.20f to 0.22f, 0.33f to 0.08f, 0.47f to 0.18f, 0.58f to 0.07f,
    0.66f to 0.24f, 0.80f to 0.10f, 0.91f to 0.20f, 0.14f to 0.34f, 0.52f to 0.32f
)

@Composable
fun MotivationBanner(today: LocalDate) {
    val quote = Quotes[today.dayOfYear % Quotes.size]

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(22.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // আকাশ
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B1464),
                        Color(0xFF6A3DB8),
                        Color(0xFFFF8A3D),
                        Color(0xFFFFC857)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // তারা
            for (s in Stars) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 1.6f * density,
                    center = Offset(w * s.first, h * s.second)
                )
            }

            // সূর্য ও তার আভা
            val sunCenter = Offset(w * 0.74f, h * 0.55f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x88FFE08A), Color.Transparent),
                    center = sunCenter,
                    radius = h * 0.5f
                ),
                radius = h * 0.5f,
                center = sunCenter
            )
            drawCircle(color = Color(0xFFFFE9A8), radius = h * 0.15f, center = sunCenter)

            // পেছনের পাহাড়
            val back = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.62f)
                lineTo(w * 0.18f, h * 0.45f)
                lineTo(w * 0.34f, h * 0.60f)
                lineTo(w * 0.52f, h * 0.40f)
                lineTo(w * 0.75f, h * 0.66f)
                lineTo(w, h * 0.50f)
                lineTo(w, h)
                close()
            }
            drawPath(back, Color(0xFF3B2A7A))

            // সামনের বড় পাহাড়
            val front = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.80f)
                lineTo(w * 0.14f, h * 0.58f)
                lineTo(w * 0.30f, h * 0.30f)
                lineTo(w * 0.48f, h * 0.62f)
                lineTo(w * 0.62f, h * 0.52f)
                lineTo(w * 0.85f, h * 0.78f)
                lineTo(w, h * 0.70f)
                lineTo(w, h)
                close()
            }
            drawPath(front, Color(0xFF1A1442))

            // চূড়ার বরফ
            val snow = Path().apply {
                moveTo(w * 0.30f, h * 0.30f)
                lineTo(w * 0.255f, h * 0.395f)
                lineTo(w * 0.285f, h * 0.37f)
                lineTo(w * 0.30f, h * 0.42f)
                lineTo(w * 0.325f, h * 0.37f)
                lineTo(w * 0.345f, h * 0.395f)
                close()
            }
            drawPath(snow, Color.White.copy(alpha = 0.92f))

            // চূড়ায় পতাকা
            drawLine(
                color = Color.White,
                start = Offset(w * 0.30f, h * 0.30f),
                end = Offset(w * 0.30f, h * 0.13f),
                strokeWidth = 2.2f * density
            )
            val flag = Path().apply {
                moveTo(w * 0.30f, h * 0.13f)
                lineTo(w * 0.30f + w * 0.085f, h * 0.17f)
                lineTo(w * 0.30f, h * 0.21f)
                close()
            }
            drawPath(flag, Color(0xFFFF5252))

            // লেখার জন্য নিচে হালকা ছায়া
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xDD0B0A1E)),
                    startY = h * 0.50f,
                    endY = h
                )
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("🔥 আজকের অনুপ্রেরণা", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(quote, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ---------- আজকের কাজ ----------

@Composable
fun StatChip(emoji: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = CardBg, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun TodayScreen(state: AppState, onSettings: () -> Unit) {
    val tasks = state.todayTasks
    val doneCount = tasks.count { it.done }
    var newTitle by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Task?>(null) }

    fun add() {
        if (newTitle.isNotBlank()) {
            state.addTask(newTitle)
            newTitle = ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { MotivationBanner(state.today) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("আজকের লক্ষ্য", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(dateText(state.today), color = Muted, fontSize = 14.sp)
                }
                IconButton(onClick = onSettings) { Text("⚙️", fontSize = 24.sp) }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("🔥", state.streak.bn(), Modifier.weight(1f))
                StatChip("🪙", state.coins.bn(), Modifier.weight(1f))
                StatChip("🛡️", state.shields.bn(), Modifier.weight(1f))
            }
        }

        item {
            val msg: String
            val msgColor: Color
            when {
                state.doneToday -> {
                    msg = "✅ আজকের স্ট্রিক নিশ্চিত! চালিয়ে যাও।"
                    msgColor = Green
                }
                state.streakAtRisk -> {
                    msg = "⚠️ আজ কাজ শেষ না করলে ${state.streak.bn()} দিনের স্ট্রিক হারাবে!"
                    msgColor = Orange
                }
                else -> {
                    msg = "🚀 আজ সব কাজ শেষ করে স্ট্রিক শুরু/বাড়াও!"
                    msgColor = Gold
                }
            }
            val frac = if (tasks.isEmpty()) 0f else doneCount / tasks.size.toFloat()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(msg, color = msgColor, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { frac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = Orange,
                        trackColor = Color(0xFF34315F)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${doneCount.bn()}/${tasks.size.bn()} টি কাজ শেষ",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("নতুন কাজ লেখো…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() })
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { add() }) { Text("➕ যোগ") }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Text(
                        "আজ এখনও কোনো কাজ নেই।\nউপরে লিখে আজকের প্রথম কাজটা যোগ করো ✍️",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        color = Muted
                    )
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { state.toggleTask(task.id, it) },
                    onDelete = { deleting = task },
                    onAddMinutes = { state.addMinutes(task.id, it) }
                )
            }
        }
    }

    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("কাজটি মুছবে?") },
            text = { Text(t.title) },
            confirmButton = {
                TextButton(onClick = {
                    state.deleteTask(t.id)
                    deleting = null
                }) { Text("মুছো") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("না") } }
        )
    }
}

@Composable
fun TaskRow(
    task: Task,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onAddMinutes: (Int) -> Unit
) {
    var showTime by remember { mutableStateOf(false) }
    var minText by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.done,
                    onCheckedChange = onToggle,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Orange,
                        checkmarkColor = Color.Black,
                        uncheckedColor = Muted
                    )
                )
                Text(
                    task.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = if (task.done) Muted else Color.White,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null
                )
                IconButton(onClick = onDelete) { Text("🗑️", fontSize = 18.sp) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⏱ ${minutesText(task.minutes)}",
                    color = Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showTime = !showTime }) {
                    Text(if (showTime) "বন্ধ করো" else "＋ সময় যোগ করো", fontSize = 13.sp)
                }
            }
            if (showTime) {
                Row(
                    Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it.filter { ch -> ch.isDigit() }.take(4) },
                        modifier = Modifier.weight(1f),
                        label = { Text("কত মিনিট কাজ করলে?") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val m = minText.toIntOrNull() ?: 0
                        if (m > 0) {
                            onAddMinutes(m)
                            minText = ""
                            showTime = false
                        }
                    }) { Text("যোগ") }
                }
            }
        }
    }
}

// ---------- প্রোফাইল ----------

@Composable
fun StatCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gold)
            Text(label, fontSize = 12.sp, color = Muted)
        }
    }
}

@Composable
fun BadgeCard(badge: Badge, earned: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (earned) Color(0xFF2D2A5C) else CardBg
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (earned) badge.emoji else "🔒",
                fontSize = 34.sp,
                modifier = Modifier.alpha(if (earned) 1f else 0.5f)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    badge.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (earned) Gold else Muted
                )
                Text("${badge.days.bn()} দিনের স্ট্রিক", fontSize = 12.sp, color = Muted)
                Text(badge.desc, fontSize = 12.sp, color = Muted)
            }
            if (earned) Text("✅", fontSize = 20.sp)
        }
    }
}

@Composable
fun ProfileScreen(state: AppState) {
    val next = Rules.badges.firstOrNull { it.days > state.streak && it.days !in state.earnedBadges }
    val earnedCount = Rules.badges.count { it.days in state.earnedBadges }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("👤 আমার প্রোফাইল", fontSize = 26.sp, fontWeight = FontWeight.Bold) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("🔥", "বর্তমান স্ট্রিক", "${state.streak.bn()} দিন", Modifier.weight(1f))
                StatCard("🏅", "সর্বোচ্চ স্ট্রিক", "${state.longestStreak.bn()} দিন", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("🪙", "মোট কয়েন", state.coins.bn(), Modifier.weight(1f))
                StatCard(
                    "🛡️",
                    "শিল্ড",
                    "${state.shields.bn()}/${Rules.MAX_SHIELDS.bn()}",
                    Modifier.weight(1f)
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("⏱️", "মোট কাজের সময়", minutesText(state.totalMinutes), Modifier.weight(1f))
                StatCard("📅", "সফল দিন", "${state.totalDays.bn()} দিন", Modifier.weight(1f))
            }
        }
        item {
            StatCard("✅", "মোট সম্পন্ন কাজ", state.totalTasksDone.bn(), Modifier.fillMaxWidth())
        }

        if (next != null) {
            item {
                val left = next.days - state.streak
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2350))
                ) {
                    Text(
                        "🎯 পরের ব্যাজ: ${next.emoji} ${next.name}\nআর ${left.bn()} দিন টানা কাজ করলেই পাবে!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Text(
                "🏅 ব্যাজ সংগ্রহ (${earnedCount.bn()}/${Rules.badges.size.bn()})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        items(Rules.badges) { b -> BadgeCard(b, b.days in state.earnedBadges) }

        item {
            val days = Rules.shieldDays.sorted().take(6).joinToString(", ") { it.bn() }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Text(
                    "🛡️ শিল্ড কীভাবে পাবে?\nস্ট্রিক $days… দিনে একটি করে শিল্ড মেলে। কোনো দিন কাজ মিস হলে শিল্ড নিজে থেকেই স্ট্রিক বাঁচিয়ে দেয়। একসঙ্গে সর্বোচ্চ ${Rules.MAX_SHIELDS.bn()}টি জমানো যায়।",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ---------- সেটিংস ----------

@Composable
fun SettingsDialog(state: AppState, actions: Actions, onClose: () -> Unit) {
    val ctx = LocalContext.current

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("ঠিক আছে") } },
        title = { Text("⚙️ রিমাইন্ডার সেটিংস") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("রিমাইন্ডার চালু", modifier = Modifier.weight(1f), fontSize = 16.sp)
                    Switch(
                        checked = state.remindersOn,
                        onCheckedChange = { state.switchReminders(it) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("কাজ বাকি থাকলে এই সময়গুলোতে নোটিফিকেশন আসবে:", color = Muted, fontSize = 13.sp)

                state.reminderTimes.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏰ ${timeText(m)}", modifier = Modifier.weight(1f), fontSize = 16.sp)
                        TextButton(onClick = { state.removeReminder(m) }) { Text("মুছো") }
                    }
                }

                if (state.reminderTimes.size < 8) {
                    OutlinedButton(onClick = {
                        TimePickerDialog(
                            ctx,
                            { _, h, mi -> state.addReminder(h * 60 + mi) },
                            20,
                            0,
                            false
                        ).show()
                    }) { Text("＋ নতুন সময় যোগ করো") }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = actions.testNotification) { Text("🔔 টেস্ট নোটিফিকেশন") }

                Spacer(Modifier.height(14.dp))
                Text(
                    "💡 ভিভো/ফানটাচ ওএসে অ্যাপ বন্ধ থাকলেও রিমাইন্ডার পেতে: অ্যাপের সেটিংসে গিয়ে ব্যাটারি → \"Unrestricted / ব্যাকগ্রাউন্ডে চলতে দাও\" এবং অটো-স্টার্ট চালু করে দিও।",
                    color = Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = actions.openAppSettings) { Text("🔋 অ্যাপ সেটিংস খোলো") }
            }
        }
    )
}
