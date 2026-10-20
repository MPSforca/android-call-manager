package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.designsystem.theme.LocalCallManagerColors

private data class ToneColors(
    val container: Color,
    val onContainer: Color,
)

@Composable
private fun toneColors(tone: StatusTone): ToneColors {
    val colors = LocalCallManagerColors.current
    return when (tone) {
        StatusTone.BLOCKED -> ToneColors(colors.blockedContainer, colors.onBlockedContainer)
        StatusTone.ALLOWED -> ToneColors(colors.allowedContainer, colors.onAllowedContainer)
        StatusTone.SILENCED -> ToneColors(colors.silencedContainer, colors.onSilencedContainer)
        StatusTone.UNKNOWN -> ToneColors(colors.unknownContainer, colors.onUnknownContainer)
    }
}

/** A small colored label for a call outcome, e.g. "Blocked", "Allowed", "Silenced". */
@Composable
fun StatusPill(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val toneColors = toneColors(tone)
    Text(
        text = text,
        color = toneColors.onContainer,
        style = MaterialTheme.typography.labelMedium,
        modifier =
            modifier
                .background(toneColors.container, MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatusPillPreview() {
    CallManagerTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            StatusPill("Blocked", StatusTone.BLOCKED)
            StatusPill("Allowed", StatusTone.ALLOWED)
            StatusPill("Silenced", StatusTone.SILENCED)
            StatusPill("Unknown", StatusTone.UNKNOWN)
        }
    }
}
