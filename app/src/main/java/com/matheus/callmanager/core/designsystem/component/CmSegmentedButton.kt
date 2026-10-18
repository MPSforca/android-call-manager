package com.matheus.callmanager.core.designsystem.component

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme

/** A single-choice segmented control over [options]; [label] renders each option's text. */
@Composable
fun <T> CmSegmentedButton(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label(option))
            }
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CmSegmentedButtonPreview() {
    CallManagerTheme {
        CmSegmentedButton(
            options = listOf("Silence", "Reject"),
            selected = "Silence",
            onSelect = {},
            label = { it },
        )
    }
}
