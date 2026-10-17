package com.matheus.callmanager.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing

/**
 * One row in a call list: avatar, name/number, a formatted timestamp, and the outcome pill.
 * Deliberately takes plain strings/enums rather than a feature's call model, so the design system
 * stays free of a dependency on the domain layer.
 */
@Composable
fun CallRow(
    displayName: String,
    number: String,
    timeLabel: String,
    outcomeLabel: String,
    outcomeTone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarInitials(name = displayName)
        Spacer(modifier = Modifier.width(spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = number,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(spacing.small))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(spacing.extraSmall))
            StatusPill(text = outcomeLabel, tone = outcomeTone, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CallRowPreview() {
    CallManagerTheme {
        Column {
            CallRow(
                displayName = "Ada Lovelace",
                number = "+1 555 0100",
                timeLabel = "10:42",
                outcomeLabel = "Blocked",
                outcomeTone = StatusTone.BLOCKED,
            )
            CallRow(
                displayName = "Unknown",
                number = "+1 555 0199",
                timeLabel = "Yesterday",
                outcomeLabel = "Allowed",
                outcomeTone = StatusTone.ALLOWED,
            )
        }
    }
}
