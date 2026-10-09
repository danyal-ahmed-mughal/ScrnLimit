package com.scrnlimit.screen.time.control.ui.gate

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.scrnlimit.screen.time.control.ui.GateRequest
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL
import kotlin.math.roundToInt

@Composable
fun PinGate(
    request: GateRequest,
    biometric: Boolean,
    verify: (String) -> Boolean,
    onSubmit: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(request, biometric) {
        if (!biometric) return@LaunchedEffect
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        val allowed = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (allowed != BiometricManager.BIOMETRIC_SUCCESS) return@LaunchedEffect
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (!request.deferred.isCompleted) onSubmit(true)
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("ScrnLimit")
                .setSubtitle(request.title)
                .setNegativeButtonText(Phrases.text("use_pin"))
                .build(),
        )
    }
    PinEntry(
        title = request.title,
        subtitle = t("enter_pin"),
        confirm = false,
        onCancel = { onSubmit(false) },
    ) { pin ->
        if (verify(pin)) {
            onSubmit(true)
            true
        } else {
            false
        }
    }
}

@Composable
fun CreatePin(onCancel: () -> Unit, onSave: (String) -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    PinEntry(
        title = if (first == null) t("choose_pin") else t("type_again"),
        subtitle = t("pin_sub"),
        confirm = true,
        onCancel = onCancel,
    ) { pin ->
        val existing = first
        if (existing == null) {
            first = pin
            true
        } else if (existing == pin) {
            onSave(pin)
            true
        } else {
            first = null
            false
        }
    }
}

@Composable
private fun PinEntry(
    title: String,
    subtitle: String,
    confirm: Boolean,
    onCancel: (() -> Unit)? = null,
    onPin: (String) -> Boolean,
) {
    var digits by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (!error) return@LaunchedEffect
        shake.snapTo(0f)
        shake.animateTo(
            0f,
            keyframes {
                durationMillis = 320
                -14f at 40
                14f at 80
                -10f at 140
                8f at 200
                0f at 320
            },
        )
        error = false
    }
    LaunchedEffect(digits) {
        if (digits.length < 4) return@LaunchedEffect
        val pin = digits
        val ok = onPin(pin)
        if (!ok) {
            digits = ""
            error = true
        } else if (confirm) {
            digits = ""
        }
    }
    Column(
        Modifier.fillMaxSize().background(SL.Ink).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = SL.Mute, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
        ) {
            repeat(4) { index ->
                Box(
                    Modifier.size(14.dp).clip(CircleShape).background(if (index < digits.length) SL.Signal else SL.Line),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.size(72.dp))
                    } else {
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SL.Card)
                                .clickable {
                                    digits = when (key) {
                                        "⌫" -> digits.dropLast(1)
                                        else -> if (digits.length < 4) digits + key else digits
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(key, color = SL.Paper, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (onCancel != null) {
            Spacer(Modifier.height(12.dp))
            Text(t("cancel"), color = SL.Mute, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onCancel).padding(8.dp))
        }
    }
}
