package com.dailypush.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

private class Piece(
    val x: Float,       // 0..1 (স্ক্রিনের প্রস্থের অনুপাত)
    val delay: Float,   // কত সেকেন্ড পরে পড়া শুরু
    val speed: Float,   // সেকেন্ডে স্ক্রিনের কত অংশ নামবে
    val sz: Float,      // আকার (dp)
    val color: Color,
    val phase: Float,
    val sway: Float,    // দোল খাওয়ার পরিমাণ (dp)
    val spin: Float,    // ঘোরার গতি
    val kind: Int       // 0 = আয়তক্ষেত্র, 1 = গোল, 2 = ট্রফি
)

private val ConfettiColors = listOf(
    Color(0xFFFF5252), Color(0xFFFFC857), Color(0xFF4ADE80), Color(0xFF38BDF8),
    Color(0xFFA78BFA), Color(0xFFF472B6), Color(0xFFFF8A3D), Color(0xFFFFFFFF)
)

@Composable
fun ConfettiCanvas(modifier: Modifier = Modifier) {
    val pieces = remember {
        val r = Random(System.nanoTime())
        val list = ArrayList<Piece>()
        repeat(170) {
            list.add(
                Piece(
                    x = r.nextFloat(),
                    delay = r.nextFloat() * 3.5f,
                    speed = 0.18f + r.nextFloat() * 0.30f,
                    sz = 7f + r.nextFloat() * 8f,
                    color = ConfettiColors[r.nextInt(ConfettiColors.size)],
                    phase = r.nextFloat() * 6.28f,
                    sway = 8f + r.nextFloat() * 28f,
                    spin = 120f + r.nextFloat() * 420f,
                    kind = r.nextInt(2)
                )
            )
        }
        repeat(14) {
            list.add(
                Piece(
                    x = 0.05f + r.nextFloat() * 0.9f,
                    delay = r.nextFloat() * 3.5f,
                    speed = 0.14f + r.nextFloat() * 0.14f,
                    sz = 26f + r.nextFloat() * 22f,
                    color = Color.White,
                    phase = r.nextFloat() * 6.28f,
                    sway = 10f + r.nextFloat() * 20f,
                    spin = 40f + r.nextFloat() * 80f,
                    kind = 2
                )
            )
        }
        list
    }

    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = androidx.compose.runtime.withFrameNanos { it }
        while (time < 13f) {
            androidx.compose.runtime.withFrameNanos { now ->
                time = (now - start) / 1_000_000_000f
            }
        }
    }

    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = time
        for (p in pieces) {
            val tt = t - p.delay
            if (tt <= 0f) continue
            val y = -80f + tt * p.speed * h
            if (y > h + 100f) continue
            val x = p.x * w + sin(tt * 2.2f + p.phase) * p.sway * density
            val rot = tt * p.spin + p.phase * 50f
            val s = p.sz * density
            when (p.kind) {
                2 -> drawIntoCanvas { c ->
                    paint.textSize = s * 1.6f
                    val n = c.nativeCanvas
                    n.save()
                    n.rotate(sin(tt * 1.5f + p.phase) * 25f, x, y)
                    n.drawText("🏆", x, y, paint)
                    n.restore()
                }
                0 -> rotate(rot, Offset(x, y)) {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(x - s / 2f, y - s * 0.3f),
                        size = Size(s, s * 0.6f)
                    )
                }
                else -> drawCircle(color = p.color, radius = s * 0.35f, center = Offset(x, y))
            }
        }
    }
}

private val Cheers = listOf(
    "তুমি আজ নিজের কথা রেখেছ! 💪",
    "প্রতিদিনের এই ছোট জয়ই বড় সাফল্যের ভিত্তি।",
    "অসাধারণ! আজকের দিনটা তোমার।",
    "থেমো না — তুমি দারুণ করছ! 🚀",
    "নিজের সঙ্গে করা প্রতিশ্রুতি রাখাই আসল শক্তি।"
)

@Composable
fun CelebrationOverlay(c: Celebration, onDone: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val cheer = remember { Cheers.random() }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val inf = rememberInfiniteTransition(label = "pulse")
    val pulse by inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD0B0A1E))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🏆", fontSize = 72.sp, modifier = Modifier.scale(pulse))
                Text(
                    "অভিনন্দন! 🎉",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Text(
                    "আজকের সব কাজ শেষ!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(cheer, fontSize = 14.sp, color = Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))

                Text("🪙 +${c.coins.bn()} কয়েন", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gold)
                Text(
                    "🔥 ${c.streak.bn()} দিনের স্ট্রিক",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Orange
                )

                if (c.badge != null) {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2A5C))
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("নতুন ব্যাজ অর্জন! (+${c.badgeBonus.bn()} 🪙)", fontSize = 13.sp, color = Muted)
                            Text(c.badge.emoji, fontSize = 40.sp)
                            Text(
                                c.badge.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gold
                            )
                            Text(
                                c.badge.desc,
                                fontSize = 13.sp,
                                color = Muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (c.shieldEarned) {
                    Text(
                        "🛡️ তুমি একটি শিল্ড পেয়েছ! মিস হলে স্ট্রিক বাঁচাবে।",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Green,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("দারুণ! 💪", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // কাগজি জরি আর ট্রফি উপর থেকে ঝরে পড়ে (সবার উপরে আঁকা হয়)
        ConfettiCanvas(Modifier.fillMaxSize())
    }
}
