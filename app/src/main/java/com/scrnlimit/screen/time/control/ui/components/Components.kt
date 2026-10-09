package com.scrnlimit.screen.time.control.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.scrnlimit.screen.time.control.domain.DayPoint
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun Modifier.slCard(): Modifier = this
    .clip(RoundedCornerShape(22.dp))
    .background(SL.Card)
    .border(1.dp, SL.Line, RoundedCornerShape(22.dp))

@Composable
fun Kicker(text: String, color: Color = SL.Accent, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = color,
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        modifier = modifier,
    )
}

@Composable
fun SLButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false,
    tint: Color = SL.Signal,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val fill = if (secondary) Color.Transparent else tint
    val ink = if (secondary && !SL.Dark && tint == SL.Signal) SL.Accent else tint
    val content = if (secondary) ink else SL.OnSignal
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (enabled) fill else fill.copy(alpha = 0.35f))
            .border(1.dp, if (secondary) ink.copy(alpha = if (enabled) 1f else 0.35f) else Color.Transparent, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = content.copy(alpha = if (enabled) 1f else 0.55f), fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun Headline(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = SL.Paper,
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
        textAlign = align,
    )
}

@Composable
fun MeterRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke * 1.7f
            val arc = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val face = arc.minDimension / 2f - stroke * 0.35f
            drawCircle(SL.Card, face, center)
            val gap = 72f
            val sweepAll = 360f - gap
            val start = 90f + gap / 2f
            val line = Stroke(stroke, cap = StrokeCap.Round)
            drawArc(SL.Line, start, sweepAll, false, topLeft, arc, style = line)
            val filled = progress.coerceAtLeast(0f).coerceAtMost(1f)
            val over = (progress - 1f).coerceIn(0f, 1f)
            if (filled > 0.004f) {
                val sweep = sweepAll * filled
                drawArc(
                    color.copy(alpha = 0.18f),
                    start,
                    sweep,
                    false,
                    topLeft,
                    arc,
                    style = Stroke(stroke * 2.6f, cap = StrokeCap.Round),
                )
                drawArc(color, start, sweep, false, topLeft, arc, style = line)
                if (over < 0.004f && filled < 0.985f) {
                    val end = Math.toRadians((start + sweep).toDouble())
                    val radius = arc.minDimension / 2f
                    drawCircle(
                        color,
                        stroke * 0.42f,
                        Offset(center.x + radius * cos(end).toFloat(), center.y + radius * sin(end).toFloat()),
                    )
                }
            }
            if (over > 0.004f) {
                val overSweep = (gap - 18f) * over.coerceAtMost(0.85f)
                drawArc(
                    color,
                    start + sweepAll,
                    overSweep,
                    false,
                    topLeft,
                    arc,
                    style = Stroke(stroke * 0.62f, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@Composable
fun LimitDial(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 268.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2f + 10.dp.toPx()
            val arc = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = arc.minDimension / 2f
            drawArc(SL.Line, 0f, 360f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Butt))
            val amount = progress.coerceAtLeast(0f)
            val within = amount.coerceAtMost(1f)
            if (within > 0.004f) {
                val sweep = 360f * within
                val cap = if (sweep > 350f) StrokeCap.Butt else StrokeCap.Round
                drawArc(SL.Signal, -90f, sweep, false, topLeft, arc, style = Stroke(stroke, cap = cap))
            }
            val extra = (amount - 1f).coerceAtLeast(0f)
            if (extra > 0.004f) {
                drawArc(
                    SL.Coral,
                    -90f,
                    360f * extra.coerceAtMost(0.42f),
                    false,
                    topLeft,
                    arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            drawLine(
                SL.Paper,
                Offset(center.x, center.y - radius - stroke * 0.15f),
                Offset(center.x, center.y - radius + stroke * 0.72f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        content()
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = SL.Paper) {
    Column(modifier.slCard().padding(horizontal = 12.dp, vertical = 12.dp)) {
        Text(label, color = SL.Faint, fontSize = 12.sp, lineHeight = 15.sp, fontFamily = Outfit, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(value, color = valueColor, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        if (action != null && onAction != null) {
            Text(action, color = SL.Accent, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        text = text,
        color = if (selected) SL.OnSignal else SL.Paper,
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = modifier
            .clip(shape)
            .background(if (selected) SL.Signal else SL.Card)
            .border(1.dp, if (selected) SL.Signal else SL.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().slCard().padding(4.dp)) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) SL.Signal else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) SL.OnSignal else SL.Mute, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun PackageIcon(packageName: String, label: String, color: Color, size: Dp = 44.dp) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val bitmap = remember(packageName, px) {
        if (packageName == "games") return@remember null
        val drawable = runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull() ?: return@remember null
        runCatching { drawable.toBitmap(px, px, Bitmap.Config.ARGB_8888).asImageBitmap() }.getOrNull()
    }
    val mark = markFor(packageName)
    val shape = RoundedCornerShape((size.value * 0.28f).dp)
    when {
        bitmap != null -> Image(
            bitmap = bitmap,
            contentDescription = label,
            modifier = Modifier.size(size).clip(shape),
            contentScale = ContentScale.Crop,
        )
        mark != null -> BrandIcon(mark, size)
        else -> Monogram(label, color, size)
    }
}

@Composable
fun Monogram(label: String, color: Color, size: Dp = 44.dp) {
    val letter = label.trim().take(1).uppercase().ifEmpty { "?" }
    Box(
        Modifier.size(size).clip(RoundedCornerShape((size.value * 0.28f).dp)).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = Color.White, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4f).sp)
    }
}

@Composable
fun UsageBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(SL.CardHigh)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

@Composable
fun ChecklistRow(title: String, body: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().slCard().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).border(1.5.dp, SL.Signal, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(body, color = SL.Mute, fontSize = 13.sp)
        }
    }
}

@Composable
fun PermissionLine(title: String, body: String, granted: Boolean, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().slCard().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(
                if (granted) t("on") else action,
                color = if (granted) SL.OnSignal else SL.Paper,
                fontFamily = Outfit,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (granted) SL.Signal else SL.CardHigh)
                    .border(1.dp, if (granted) SL.Signal else SL.Line, RoundedCornerShape(999.dp))
                    .clickable(enabled = !granted, onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(body, color = SL.Mute, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
fun DayChart(days: List<DayPoint>, highlightLast: Boolean = true) {
    if (days.isEmpty()) return
    val max = days.maxOf { it.distractingMs }.coerceAtLeast(1L)
    Column(Modifier.fillMaxWidth().slCard().padding(16.dp)) {
        Canvas(Modifier.fillMaxWidth().height(112.dp)) {
            val gap = 6.dp.toPx()
            val width = (size.width - gap * (days.size - 1).coerceAtLeast(0)) / days.size
            days.forEachIndexed { index, day ->
                val fraction = day.distractingMs.toFloat() / max.toFloat()
                val bar = (size.height * fraction).coerceAtLeast(if (day.distractingMs > 0) 6.dp.toPx() else 2.dp.toPx())
                val left = index * (width + gap)
                val color = if (highlightLast && index == days.lastIndex) SL.Signal else SL.Signal.copy(alpha = 0.38f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, size.height - bar),
                    size = Size(width, bar),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Formats.shortDay(days.first().date), color = SL.Faint, fontSize = 11.sp)
            Text(Formats.shortDay(days.last().date), color = SL.Faint, fontSize = 11.sp)
        }
    }
}

@Composable
fun HourStrip(hours: LongArray) {
    val max = hours.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val peak = hours.indices.maxByOrNull { hours[it] } ?: 0
    Column(Modifier.fillMaxWidth().slCard().padding(16.dp)) {
        Text(t("peak", Formats.hourLabel(peak)), color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            hours.forEachIndexed { hour, value ->
                val fraction = value.toFloat() / max.toFloat()
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(min(1f, fraction.coerceAtLeast(0.06f)))
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (hour == peak && value > 0) SL.Amber else SL.Signal.copy(alpha = 0.45f)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18).forEach { hour ->
                Text(Formats.hourLabel(hour), color = SL.Faint, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun TimelineStrip(blocks: List<Pair<Color, Float>>) {
    if (blocks.isEmpty()) {
        Text(t("timeline_empty"), color = SL.Mute, fontSize = 14.sp)
        return
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        blocks.forEach { (color, weight) ->
            Box(
                Modifier
                    .width((28 + weight * 90).dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(color),
            )
        }
    }
}

@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StepKey("–", onMinus)
        Text(value, color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 28.sp, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.width(120.dp))
        StepKey("+", onPlus)
    }
}

@Composable
private fun StepKey(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(SL.CardHigh).border(1.dp, SL.Line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = SL.Paper, fontSize = 22.sp, fontFamily = Outfit, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SwitchRow(title: String, body: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(body, color = SL.Mute, fontSize = 13.sp)
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = SL.Signal,
                checkedThumbColor = SL.OnSignal,
                uncheckedTrackColor = SL.CardHigh,
                uncheckedThumbColor = SL.Mute,
                uncheckedBorderColor = SL.Line,
            ),
        )
    }
}
