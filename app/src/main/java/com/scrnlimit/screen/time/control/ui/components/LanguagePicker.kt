package com.scrnlimit.screen.time.control.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL
import kotlin.math.cos
import kotlin.math.sin

val languageChoices = listOf(
    "system" to "System",
    "en" to "English",
    "de" to "Deutsch",
    "fr" to "Français",
    "es" to "Español",
    "it" to "Italiano",
    "pt" to "Português",
    "nl" to "Nederlands",
    "pl" to "Polski",
    "ro" to "Română",
    "el" to "Ελληνικά",
    "sv" to "Svenska",
    "uk" to "Українська",
    "ru" to "Русский",
    "tr" to "Türkçe",
    "ar" to "العربية",
    "fa" to "فارسی",
    "zh" to "中文",
    "ko" to "한국어",
    "vi" to "Tiếng Việt",
    "fil" to "Filipino",
)

@Composable
fun languageTitle(code: String): String {
    if (code == "system") return t("lang_system")
    return languageChoices.find { it.first == code }?.second ?: code
}

fun languageCode(code: String): String {
    val active = if (code == "system") Phrases.code else code
    return if (active == "fil") "FIL" else active.uppercase()
}

@Composable
fun LanguageChip(code: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(SL.Card)
            .border(1.dp, SL.Line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Globe(Modifier.size(14.dp))
        Text(
            languageCode(code),
            color = SL.Paper,
            fontFamily = Outfit,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, end = 4.dp),
        )
        Text("▾", color = SL.Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LanguageField(code: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageMark(code)
        Spacer(Modifier.width(10.dp))
        Text(
            languageTitle(code),
            color = SL.Paper,
            fontFamily = Outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("▾", color = SL.Accent, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun LanguageDialog(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SL.Card)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Text(
                t("language"),
                color = SL.Paper,
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                languageChoices.forEach { (code, _) ->
                    val on = code == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (on) SL.Signal.copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { onSelect(code) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        LanguageMark(code)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            languageTitle(code),
                            color = if (on) SL.Accent else SL.Paper,
                            fontFamily = Outfit,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (on) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(SL.Accent))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageMark(code: String) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        Modifier.size(width = 22.dp, height = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (code) {
            "system" -> Globe(Modifier.size(15.dp))
            "ar", "fa" -> Spacer(Modifier.fillMaxSize())
            else -> Canvas(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .border(1.dp, SL.Line, shape),
            ) { drawFlag(code) }
        }
    }
}

@Composable
private fun Globe(modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color(0xFF1F6F8B), r)
        drawCircle(Color.White, r * 0.72f, style = Stroke(r * 0.12f))
        drawLine(Color.White, Offset(c.x - r * 0.72f, c.y), Offset(c.x + r * 0.72f, c.y), r * 0.1f)
        drawOval(
            Color.White,
            topLeft = Offset(c.x - r * 0.28f, c.y - r * 0.72f),
            size = Size(r * 0.56f, r * 1.44f),
            style = Stroke(r * 0.1f),
        )
    }
}

private fun DrawScope.drawFlag(code: String) {
    val w = size.width
    val h = size.height
    fun hBand(index: Int, count: Int, color: Color) {
        drawRect(color, Offset(0f, h * index / count), Size(w, h / count + 0.8f))
    }
    fun vBand(index: Int, count: Int, color: Color) {
        drawRect(color, Offset(w * index / count, 0f), Size(w / count + 0.8f, h))
    }
    when (code) {
        "en" -> {
            repeat(7) { hBand(it, 7, if (it % 2 == 0) Color(0xFFB22234) else Color.White) }
            drawRect(Color(0xFF3C3B6E), size = Size(w * 0.44f, h * 0.54f))
        }
        "de" -> {
            hBand(0, 3, Color.Black)
            hBand(1, 3, Color(0xFFDD0000))
            hBand(2, 3, Color(0xFFFFCE00))
        }
        "fr" -> {
            vBand(0, 3, Color(0xFF0055A4))
            vBand(1, 3, Color.White)
            vBand(2, 3, Color(0xFFEF4135))
        }
        "es" -> {
            hBand(0, 4, Color(0xFFAA151B))
            drawRect(Color(0xFFF1BF00), Offset(0f, h * 0.25f), Size(w, h * 0.5f))
            hBand(3, 4, Color(0xFFAA151B))
        }
        "it" -> {
            vBand(0, 3, Color(0xFF009246))
            vBand(1, 3, Color.White)
            vBand(2, 3, Color(0xFFCE2B37))
        }
        "pt" -> {
            drawRect(Color(0xFF006600), size = Size(w * 0.4f, h))
            drawRect(Color(0xFFFF0000), Offset(w * 0.4f, 0f), Size(w * 0.6f, h))
            drawCircle(Color(0xFFFFCC00), h * 0.26f, Offset(w * 0.4f, h / 2f))
        }
        "nl" -> {
            hBand(0, 3, Color(0xFFAE1C28))
            hBand(1, 3, Color.White)
            hBand(2, 3, Color(0xFF21468B))
        }
        "pl" -> {
            hBand(0, 2, Color.White)
            hBand(1, 2, Color(0xFFDC143C))
        }
        "ro" -> {
            vBand(0, 3, Color(0xFF002B7F))
            vBand(1, 3, Color(0xFFFCD116))
            vBand(2, 3, Color(0xFFCE1126))
        }
        "el" -> {
            val blue = Color(0xFF0D5EAF)
            repeat(5) { hBand(it, 5, if (it % 2 == 0) blue else Color.White) }
            drawRect(blue, size = Size(w * 0.4f, h * 0.6f))
            drawRect(Color.White, Offset(w * 0.16f, 0f), Size(w * 0.08f, h * 0.6f))
            drawRect(Color.White, Offset(0f, h * 0.26f), Size(w * 0.4f, h * 0.1f))
        }
        "sv" -> {
            drawRect(Color(0xFF006AA7))
            drawRect(Color(0xFFFECC00), Offset(w * 0.28f, 0f), Size(w * 0.16f, h))
            drawRect(Color(0xFFFECC00), Offset(0f, h * 0.4f), Size(w, h * 0.2f))
        }
        "uk" -> {
            hBand(0, 2, Color(0xFF005BBB))
            hBand(1, 2, Color(0xFFFFD500))
        }
        "ru" -> {
            hBand(0, 3, Color.White)
            hBand(1, 3, Color(0xFF0039A6))
            hBand(2, 3, Color(0xFFD52B1E))
        }
        "tr" -> {
            drawRect(Color(0xFFE30A17))
            drawCircle(Color.White, h * 0.28f, Offset(w * 0.38f, h / 2f))
            drawCircle(Color(0xFFE30A17), h * 0.22f, Offset(w * 0.44f, h / 2f))
            star(Offset(w * 0.64f, h / 2f), h * 0.16f, Color.White)
        }
        "zh" -> {
            drawRect(Color(0xFFDE2910))
            star(Offset(w * 0.28f, h * 0.35f), h * 0.2f, Color(0xFFFFDE00))
        }
        "ko" -> {
            drawRect(Color.White)
            drawCircle(Color(0xFFCD2E3A), h * 0.28f, Offset(w / 2f, h / 2f))
            drawArc(Color(0xFF0047A0), 20f, 180f, false, Offset(w / 2f - h * 0.28f, h / 2f - h * 0.28f), Size(h * 0.56f, h * 0.56f))
        }
        "vi" -> {
            drawRect(Color(0xFFDA251D))
            star(Offset(w / 2f, h / 2f), h * 0.26f, Color(0xFFFF0))
        }
        "fil" -> {
            drawRect(Color(0xFF0038A8), size = Size(w, h / 2f))
            drawRect(Color(0xFFCE1126), Offset(0f, h / 2f), Size(w, h / 2f))
            val triangle = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.46f, h / 2f)
                lineTo(0f, h)
                close()
            }
            drawPath(triangle, Color.White)
            drawCircle(Color(0xFFFCD116), h * 0.12f, Offset(w * 0.16f, h / 2f))
        }
    }
}

private fun DrawScope.star(center: Offset, radius: Float, color: Color) {
    val path = Path()
    repeat(10) { index ->
        val ring = if (index % 2 == 0) radius else radius * 0.42f
        val angle = Math.toRadians(-90.0 + index * 36.0)
        val x = center.x + (cos(angle) * ring).toFloat()
        val y = center.y + (sin(angle) * ring).toFloat()
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
