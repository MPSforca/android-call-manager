package com.sforca.callmanager.core.designsystem.component

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme

/** Primary, filled call-to-action button. Thin wrapper so button styling stays in one place. */
@Composable
fun CmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text)
    }
}

/** Secondary, outlined button for a less prominent action alongside a [CmButton]. */
@Composable
fun CmOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text)
    }
}

/** Lowest-emphasis action, e.g. "Cancel" in a dialog or a link-style action in a list row. */
@Composable
fun CmTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CmButtonPreview() {
    CallManagerTheme {
        androidx.compose.foundation.layout.Column(
            verticalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(8.dp),
        ) {
            CmButton(text = "Block number", onClick = {})
            CmOutlinedButton(text = "Allow number", onClick = {})
            CmTextButton(text = "Cancel", onClick = {})
        }
    }
}
