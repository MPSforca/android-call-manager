package com.sforca.callmanager.feature.lists

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.sforca.callmanager.core.data.CallRepository
import com.sforca.callmanager.core.database.CallManagerDatabase
import com.sforca.callmanager.core.database.entity.AllowListEntry
import com.sforca.callmanager.core.database.entity.AllowedNamePattern
import com.sforca.callmanager.core.database.entity.BlockListEntry
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.testing.ComposeHostActivityRule
import com.sforca.callmanager.testing.FakeContactsLookup
import com.sforca.callmanager.testing.FakeRecentOutgoingLookup
import com.sforca.callmanager.testing.FakeSettingsRepository
import com.sforca.callmanager.testing.MainDispatcherRule
import com.sforca.callmanager.testing.inMemoryTestDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListsScreenTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(MainDispatcherRule()).around(ComposeHostActivityRule()).around(compose)

    private lateinit var db: CallManagerDatabase
    private lateinit var repository: CallRepository
    private lateinit var viewModel: ListsViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = inMemoryTestDatabase(context)
        repository = CallRepository(db, FakeContactsLookup(), FakeRecentOutgoingLookup(), FakeSettingsRepository())
        viewModel = ListsViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun show(
        state: ListsUiState,
        onPickBlock: () -> Unit = {},
        onPickAllow: () -> Unit = {},
    ) {
        compose.setContent {
            CallManagerTheme { ListsContent(state, onPickBlock, onPickAllow, viewModel) }
        }
    }

    // Waits for the dialog's write to land rather than reading whatever the table holds right now.
    private fun <T> awaitNonEmpty(flow: Flow<List<T>>): List<T> =
        runBlocking { withTimeout(5_000) { flow.first { it.isNotEmpty() } } }

    private fun patterns() = runBlocking { repository.observeAllowedNamePatterns().first() }

    @Test
    fun `empty tab shows its empty state and subtitle`() {
        show(ListsUiState())
        compose.onNodeWithText("No blocked numbers").assertIsDisplayed()
        compose.onNodeWithText("Calls from these numbers are always blocked.").assertIsDisplayed()
    }

    @Test
    fun `no matches is shown when the query hides every entry`() {
        show(ListsUiState(query = "zzz", totalInTab = 3))
        compose.onNodeWithText("No matches").assertIsDisplayed()
    }

    @Test
    fun `rows show the contact name with the number as supporting text`() {
        show(ListsUiState(blocked = listOf(BlockListEntry("+15550000001", "Spammer", 0)), totalInTab = 1))
        compose.onNodeWithText("Spammer").assertIsDisplayed()
    }

    @Test
    fun `allowed tab and name patterns tab render their entries`() {
        show(
            ListsUiState(
                tab = ListTab.Allowed,
                allowed = listOf(AllowListEntry("+15550000002", "Friend", 0)),
                totalInTab = 1,
            ),
        )
        compose.onNodeWithText("Friend").assertIsDisplayed()
    }

    @Test
    fun `name patterns render without a supporting line`() {
        show(
            ListsUiState(
                tab = ListTab.NamePatterns,
                namePatterns = listOf(AllowedNamePattern("PagerDuty", 0)),
                totalInTab = 1,
            ),
        )
        compose.onNodeWithText("PagerDuty").assertIsDisplayed()
    }

    private fun awaitState(predicate: (ListsUiState) -> Boolean) =
        runBlocking { withTimeout(5_000) { viewModel.uiState.first(predicate) } }

    @Test
    fun `segmented buttons switch tab and typing in search updates the query`() {
        show(ListsUiState())
        compose.onNodeWithText("Names").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Search").performTextInput("abc")

        val state = awaitState { it.tab == ListTab.NamePatterns && it.query == "abc" }

        assertEquals("abc", state.query)
    }

    @Test
    fun `clear button resets the query`() {
        show(ListsUiState(query = "abc"))
        viewModel.setQuery("abc")

        compose.onNodeWithContentDescription("Clear search").performClick()

        assertEquals("", awaitState { it.query.isEmpty() }.query)
    }

    @Test
    fun `adding a number from the dialog blocks it`() {
        show(ListsUiState())
        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Block a number").assertIsDisplayed()
        compose.onNodeWithText("Phone number").performTextInput("+15550000009")
        compose.onNodeWithText("Add").performClick()

        val blocked = awaitNonEmpty(repository.observeBlockList())
        assertEquals(listOf("15550000009"), blocked.map { it.number.filter(Char::isDigit) })
    }

    @Test
    fun `adding a number on the allowed tab allows it`() {
        show(ListsUiState(tab = ListTab.Allowed))
        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Phone number").performTextInput("+15550000008")
        compose.onNodeWithText("Add").performClick()

        val allowed = awaitNonEmpty(repository.observeAllowList())
        assertEquals(listOf("15550000008"), allowed.map { it.number.filter(Char::isDigit) })
    }

    @Test
    fun `adding a name pattern from the dialog stores it and cancel closes the dialog`() {
        show(ListsUiState(tab = ListTab.NamePatterns))
        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Allow a caller name").assertDoesNotExist()

        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Name contains").performTextInput("PagerDuty")
        compose.onNodeWithText("Add").performClick()

        assertEquals(listOf("PagerDuty"), awaitNonEmpty(repository.observeAllowedNamePatterns()).map { it.pattern })
    }

    @Test
    fun `choose from contacts closes the dialog and invokes the picker for the tab`() {
        var blockPicks = 0
        var allowPicks = 0
        show(ListsUiState(), onPickBlock = { blockPicks++ }, onPickAllow = { allowPicks++ })
        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Choose from contacts").performClick()

        assertEquals(1, blockPicks)
        assertEquals(0, allowPicks)
        compose.onNodeWithText("Block a number").assertDoesNotExist()
    }

    @Test
    fun `allowed tab picker and name patterns tab without picker`() {
        var allowPicks = 0
        show(ListsUiState(tab = ListTab.Allowed), onPickAllow = { allowPicks++ })
        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Choose from contacts").performClick()
        assertEquals(1, allowPicks)
    }

    @Test
    fun `name patterns dialog has no contact picker`() {
        show(ListsUiState(tab = ListTab.NamePatterns))
        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Choose from contacts").assertDoesNotExist()
        assertTrue(patterns().isEmpty())
    }
}
