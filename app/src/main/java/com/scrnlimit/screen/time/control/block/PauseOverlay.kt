package com.scrnlimit.screen.time.control.block

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrnlimit.screen.time.control.domain.BlockReason
import com.scrnlimit.screen.time.control.domain.Catalog
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.ui.components.MeterRing
import com.scrnlimit.screen.time.control.ui.components.PackageIcon
import com.scrnlimit.screen.time.control.ui.components.SLButton
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL
import com.scrnlimit.screen.time.control.ui.theme.meterColor
import kotlinx.coroutines.delay

@Composable
fun PauseOverlay(
    model: PauseModel,
    onOpen: () -> Unit,
    onLater: () -> Unit,
    onClose: () -> Unit,
    onEmergency: () -> Unit,
) {
    val context = LocalContext.current
    var left by remember(model.packageName, model.mode) { mutableIntStateOf(model.waitSeconds) }
    val ritual = model.mode == PauseMode.RITUAL
    LaunchedEffect(model.packageName, model.mode) {
        if (!ritual) return@LaunchedEffect
        left = model.waitSeconds
        while (left > 0) {
            delay(1_000)
            left--
        }
        tick(context)
    }
    val ready = !ritual || left == 0
    val breath = rememberInfiniteTransition(label = "breath")
    val scale by breath.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "scale",
    )
    val progress = if (!ritual || model.waitSeconds == 0) 1f else 1f - left / model.waitSeconds.toFloat()
    val color = if (ritual) SL.Signal else meterColor(if (model.reason == BlockReason.CAP || model.reason == BlockReason.APP_CAP) 1f else 0.9f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SL.Ink)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        PackageIcon(model.packageName, model.label, androidx.compose.ui.graphics.Color(Catalog.colorFor(model.packageName)), 56.dp)
        Spacer(Modifier.height(14.dp))
        Text(model.label, color = SL.Mute, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(22.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(if (ritual && model.breath) scale else 1f)) {
            MeterRing(progress = progress.coerceIn(0f, 1f), color = color, size = 196.dp) {
                Text(
                    text = if (ritual && !ready) left.toString() else if (ritual) t("now") else t("stop"),
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    color = SL.Paper,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = if (ritual) t("important") else Phrases.reasonTitle(model.reason),
            fontFamily = Outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = SL.Paper,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                ritual && model.breath && !ready -> if (left > model.waitSeconds / 2) t("breathe_in") else t("breathe_out")
                ritual -> t("wait_point")
                else -> model.detail.ifBlank { Phrases.reasonBody(model.reason) }
            },
            color = SL.Mute,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        if (ritual) {
            SLButton(t("not_now"), onClick = onLater, enabled = ready)
            Spacer(Modifier.height(10.dp))
            SLButton(t("open_for", model.burstMinutes), onClick = onOpen, enabled = ready, secondary = true)
        } else {
            SLButton(t("close"), onClick = onClose)
            Spacer(Modifier.height(10.dp))
            if (model.passesLeft > 0) {
                SLButton(t("use_pass"), onClick = onEmergency, secondary = true, tint = SL.Coral)
                Spacer(Modifier.height(8.dp))
                Text(Phrases.passesLine(model.passesLeft), color = SL.Faint, fontSize = 13.sp)
            } else {
                Text(t("no_passes"), color = SL.Faint, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun TimerPill(label: String, ratio: Float) {
    val color = meterColor(ratio)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SL.Ink, androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = color, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(t("left_burst"), color = SL.Mute, fontSize = 10.sp)
        }
    }
}

@Composable
fun CoverCard(title: String, body: String, action: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SL.Ink.copy(alpha = 0.94f))
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(t("in_app").uppercase(), color = SL.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 32.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(body, color = SL.Mute, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        SLButton(action, onClick = onAction, modifier = Modifier.fillMaxWidth())
    }
}

private fun tick(context: android.content.Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(20)
    }
}
