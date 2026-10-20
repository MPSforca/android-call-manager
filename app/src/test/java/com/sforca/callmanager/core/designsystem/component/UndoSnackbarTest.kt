package com.sforca.callmanager.core.designsystem.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UndoSnackbarTest {
    @Test
    fun `tapping undo runs the callback`() =
        runTest(UnconfinedTestDispatcher()) {
            val host = SnackbarHostState()
            var undone = 0
            val result = async(start = CoroutineStart.UNDISPATCHED) { host.showUndo("Deleted", "Undo") { undone++ } }

            host.currentSnackbarData!!.performAction()

            assertEquals(SnackbarResult.ActionPerformed, result.await())
            assertEquals(1, undone)
        }

    @Test
    fun `dismissing does not run the callback`() =
        runTest(UnconfinedTestDispatcher()) {
            val host = SnackbarHostState()
            var undone = 0
            val result = async(start = CoroutineStart.UNDISPATCHED) { host.showUndo("Deleted", "Undo") { undone++ } }

            host.currentSnackbarData!!.dismiss()

            assertEquals(SnackbarResult.Dismissed, result.await())
            assertEquals(0, undone)
        }
}
