package com.scrnlimit.screen.time.control.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

enum class Mark { Instagram, YouTube, TikTok, X, Facebook, Reddit, Games }

fun markFor(packageName: String): Mark? = when (packageName) {
    "com.instagram.android", "com.instagram.lite" -> Mark.Instagram
    "com.google.android.youtube" -> Mark.YouTube
    "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> Mark.TikTok
    "com.twitter.android" -> Mark.X
    "com.facebook.katana", "com.facebook.lite" -> Mark.Facebook
    "com.reddit.frontpage" -> Mark.Reddit
    "games" -> Mark.Games
    else -> null
}

@Composable
fun BrandIcon(mark: Mark, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val radius = s * 0.28f
        when (mark) {
            Mark.Instagram -> {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF4F5BD5), Color(0xFF962FBF), Color(0xFFD62976), Color(0xFFFA7E1E), Color(0xFFFEDA75)),
                        start = Offset(s, 0f),
                        end = Offset(0f, s),
                    ),
                    size = Size(s, s),
                    cornerRadius = CornerRadius(radius, radius),
                )
                val center = Offset(s / 2f, s / 2f)
                drawCircle(Color.White, s * 0.24f, center, style = Stroke(s * 0.055f))
                drawCircle(Color.White, s * 0.09f, center, style = Stroke(s * 0.05f))
                drawCircle(Color.White, s * 0.035f, Offset(s * 0.70f, s * 0.30f))
            }
            Mark.YouTube -> {
                drawRoundRect(Color(0xFFFF0033), size = Size(s, s), cornerRadius = CornerRadius(radius, radius))
                val play = Path().apply {
                    moveTo(s * 0.40f, s * 0.30f)
                    lineTo(s * 0.40f, s * 0.70f)
                    lineTo(s * 0.74f, s * 0.50f)
                    close()
                }
                drawPath(play, Color.White)
            }
            Mark.TikTok -> {
                drawRoundRect(Color(0xFF111111), size = Size(s, s), cornerRadius = CornerRadius(radius, radius))
                note(Offset(-s * 0.045f, s * 0.02f), Color(0xFF25F4EE))
                note(Offset(s * 0.045f, -s * 0.02f), Color(0xFFFE2C55))
                note(Offset.Zero, Color.White)
            }
            Mark.X -> {
                drawRoundRect(Color(0xFF111111), size = Size(s, s), cornerRadius = CornerRadius(radius, radius))
                val stroke = s * 0.09f
                drawLine(Color.White, Offset(s * 0.30f, s * 0.30f), Offset(s * 0.70f, s * 0.70f), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(s * 0.70f, s * 0.30f), Offset(s * 0.30f, s * 0.70f), stroke, StrokeCap.Round)
            }
            Mark.Facebook -> {
                drawRoundRect(Color(0xFF1877F2), size = Size(s, s), cornerRadius = CornerRadius(radius, radius))
                val stroke = s * 0.075f
                drawLine(Color.White, Offset(s * 0.46f, s * 0.22f), Offset(s * 0.46f, s * 0.78f), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(s * 0.34f, s * 0.46f), Offset(s * 0.66f, s * 0.46f), stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(s * 0.46f, s * 0.28f), Offset(s * 0.62f, s * 0.22f), stroke, StrokeCap.Round)
            }
            Mark.Reddit -> {
                drawCircle(Color(0xFFFF4500), s / 2f, Offset(s / 2f, s / 2f))
                drawCircle(Color.White, s * 0.30f, Offset(s * 0.50f, s * 0.54f))
                drawCircle(Color(0xFF1A1A1A), s * 0.045f, Offset(s * 0.40f, s * 0.50f))
                drawCircle(Color(0xFF1A1A1A), s * 0.045f, Offset(s * 0.60f, s * 0.50f))
                drawArc(
                    Color(0xFF1A1A1A),
                    20f,
                    140f,
                    false,
                    Offset(s * 0.38f, s * 0.54f),
                    Size(s * 0.24f, s * 0.16f),
                    style = Stroke(s * 0.03f, cap = StrokeCap.Round),
                )
                drawLine(Color.White, Offset(s * 0.36f, s * 0.30f), Offset(s * 0.28f, s * 0.16f), s * 0.035f, StrokeCap.Round)
                drawCircle(Color.White, s * 0.04f, Offset(s * 0.26f, s * 0.14f))
            }
            Mark.Games -> {
                drawRoundRect(Color(0xFF5B4DFF), size = Size(s, s), cornerRadius = CornerRadius(radius, radius))
                drawRoundRect(
                    Color.White,
                    topLeft = Offset(s * 0.18f, s * 0.36f),
                    size = Size(s * 0.64f, s * 0.32f),
                    cornerRadius = CornerRadius(s * 0.12f, s * 0.12f),
                )
                val arm = s * 0.045f
                drawLine(Color(0xFF5B4DFF), Offset(s * 0.32f, s * 0.46f), Offset(s * 0.32f, s * 0.58f), arm, StrokeCap.Round)
                drawLine(Color(0xFF5B4DFF), Offset(s * 0.26f, s * 0.52f), Offset(s * 0.38f, s * 0.52f), arm, StrokeCap.Round)
                drawCircle(Color(0xFF5B4DFF), s * 0.035f, Offset(s * 0.62f, s * 0.48f))
                drawCircle(Color(0xFF5B4DFF), s * 0.035f, Offset(s * 0.70f, s * 0.56f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.note(offset: Offset, color: Color) {
    val s = size.minDimension
    val head = Offset(s * 0.38f, s * 0.62f) + offset
    drawOval(color, head, Size(s * 0.24f, s * 0.16f))
    drawLine(
        color,
        Offset(s * 0.60f, s * 0.66f) + offset,
        Offset(s * 0.60f, s * 0.30f) + offset,
        s * 0.045f,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(s * 0.60f, s * 0.32f) + offset,
        Offset(s * 0.76f, s * 0.40f) + offset,
        s * 0.045f,
        StrokeCap.Round,
    )
}
