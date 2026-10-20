package com.sforca.callmanager.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sforca.callmanager.R
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme

/** A yes/no confirmation dialog, e.g. before removing a number from the block/allow list. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = stringResource(R.string.common_cancel),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { CmTextButton(text = confirmLabel, onClick = onConfirm) },
        dismissButton = { CmTextButton(text = dismissLabel, onClick = onDismiss) },
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConfirmDialogPreview() {
    CallManagerTheme {
        ConfirmDialog(
            title = "Remove from block list?",
            message = "+1 555 0100 will be able to call you again.",
            confirmLabel = "Remove",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
