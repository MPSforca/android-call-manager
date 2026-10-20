package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing

/**
 * A labelled integer slider. The value shown tracks the thumb while dragging, but [onValueChange]
 * only fires when the drag ends, so callers persist once instead of on every pixel.
 */
@Composable
fun CmSliderRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    valueLabel: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    var dragging by remember { mutableFloatStateOf(Float.NaN) }
    val shown = if (dragging.isNaN()) value else dragging.toInt()
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = valueLabel(shown),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = if (dragging.isNaN()) value.toFloat() else dragging,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                if (!dragging.isNaN()) onValueChange(dragging.toInt())
                dragging = Float.NaN
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0),
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CmSliderRowPreview() {
    CallManagerTheme {
        CmSliderRow(
            title = "Allow recently answered",
            value = 30,
            onValueChange = {},
            valueRange = 1..90,
            valueLabel = { "$it days" },
        )
    }
}
