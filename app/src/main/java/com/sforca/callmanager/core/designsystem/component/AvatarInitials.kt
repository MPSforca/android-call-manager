package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme

/** Derives up to two initials from a display name, for use where no contact photo exists. */
fun initialsOf(name: String?): String {
    val words =
        name
            ?.trim()
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            .orEmpty()
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}

/** A circular avatar showing a caller's initials, used when there is no contact photo. */
@Composable
fun AvatarInitials(
    name: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AvatarInitialsPreview() {
    CallManagerTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(8.dp),
        ) {
            AvatarInitials("Ada Lovelace")
            AvatarInitials("Grace")
            AvatarInitials(null)
        }
    }
}
