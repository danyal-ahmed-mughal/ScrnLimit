package com.scrnlimit.screen.time.control.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.components.Headline
import com.scrnlimit.screen.time.control.ui.components.SLButton
import com.scrnlimit.screen.time.control.ui.theme.SL

@Composable
fun HowItWorks(onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SL.Ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Headline(t("how_title"), align = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        HowLine(t("how_open"))
        Spacer(Modifier.height(14.dp))
        HowLine(t("how_pause"))
        Spacer(Modifier.height(14.dp))
        HowLine(t("how_cap"))
        Spacer(Modifier.weight(1f))
        SLButton(t("got_it"), onClick = onDone)
        Spacer(Modifier.height(8.dp))
        Text(
            t("skip"),
            color = SL.Mute,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onDone)
                .padding(8.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HowLine(text: String) {
    Text(
        text,
        color = SL.Mute,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
