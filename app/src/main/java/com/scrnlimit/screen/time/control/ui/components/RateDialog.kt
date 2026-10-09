package com.scrnlimit.screen.time.control.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL

@Composable
fun RateDialog(onRate: () -> Unit, onLater: () -> Unit) {
    Dialog(onDismissRequest = onLater) {
        Column(Modifier.fillMaxWidth().slCard().padding(22.dp)) {
            Text(
                t("rate_title"),
                color = SL.Paper,
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(t("rate_body"), color = SL.Mute, fontSize = 15.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(18.dp))
            SLButton(t("rate_now"), onClick = onRate)
            Spacer(Modifier.height(8.dp))
            SLButton(t("rate_later"), secondary = true, onClick = onLater)
        }
    }
}
