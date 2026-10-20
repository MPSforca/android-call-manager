package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing

/**
 * Wraps [content] so a swipe toward the end calls [onDismiss]. The row snaps back until the
 * caller removes it from the list, so a failed delete never leaves a ghost gap. [icon] is the
 * glyph revealed behind the row, a bin by default.
 */
@Composable
fun SwipeToDismissRow(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    deleteContentDescription: String? = null,
    icon: ImageVector = Icons.Filled.Delete,
    content: @Composable () -> Unit,
) {
    val spacing = LocalSpacing.current
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    val state =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) currentOnDismiss.value()
                false
            },
        )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = spacing.large),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = deleteContentDescription,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) { content() }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SwipeToDismissRowPreview() {
    CallManagerTheme {
        Column {
            SwipeToDismissRow(onDismiss = {}) {
                ListItem(headlineContent = { Text("+1 415 555 0100") })
            }
        }
    }
}
