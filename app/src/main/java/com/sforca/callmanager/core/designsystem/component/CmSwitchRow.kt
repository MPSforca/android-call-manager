package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing

/**
 * A settings row pairing a label (and optional explanatory subtitle) with a toggle. When [onInfoClick]
 * is set, an (i) button appears before the toggle, labelled by [infoContentDescription].
 */
@Composable
fun CmSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onInfoClick: (() -> Unit)? = null,
    infoContentDescription: String? = null,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onInfoClick != null) {
            CmInfoButton(onClick = onInfoClick, contentDescription = infoContentDescription)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CmSwitchRowPreview() {
    CallManagerTheme {
        Column {
            CmSwitchRow(title = "Reject instead of silence", checked = false, onCheckedChange = {})
            CmSwitchRow(
                title = "Dynamic color",
                subtitle = "Theme the app from your wallpaper",
                checked = true,
                onCheckedChange = {},
            )
        }
    }
}
