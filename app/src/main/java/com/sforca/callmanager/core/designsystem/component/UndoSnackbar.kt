package com.sforca.callmanager.core.designsystem.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * Shows [message] with an Undo action and runs [onUndo] if the user taps it. Suspends until the
 * snackbar is dismissed, so callers launch it from a coroutine and may commit the pending
 * deletion afterwards.
 */
suspend fun SnackbarHostState.showUndo(
    message: String,
    actionLabel: String,
    onUndo: () -> Unit,
): SnackbarResult {
    currentSnackbarData?.dismiss()
    val result =
        showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
            withDismissAction = true,
        )
    if (result == SnackbarResult.ActionPerformed) onUndo()
    return result
}
