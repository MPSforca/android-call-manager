package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme

/** A small (i) icon button that opens an explanation of the item next to it. */
@Composable
fun CmInfoButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.Info, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CmInfoButtonPreview() {
    CallManagerTheme {
        CmInfoButton(onClick = {}, contentDescription = "About contacts")
    }
}
