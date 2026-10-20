package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing

/** Themed card surface for grouping related content, e.g. a permission prompt or a settings group. */
@Composable
fun CmCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(LocalSpacing.current.large)) {
            content()
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CmCardPreview() {
    CallManagerTheme {
        CmCard {
            Text(text = "Battery optimization", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Exempt this app so calls can still be screened in the background.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
