package com.sforca.callmanager.feature.settings

import app.cash.turbine.test
import com.sforca.callmanager.core.model.ThemeMode
import com.sforca.callmanager.core.model.UserPreferences
import com.sforca.callmanager.testing.FakeSettingsRepository
import com.sforca.callmanager.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        viewModel = SettingsViewModel(settings)
    }

    @Test
    fun `preferences start at the defaults and follow the repository`() =
        runTest {
            viewModel.preferences.test {
                assertEquals(UserPreferences(), awaitItem())

                settings.update { copy(themeMode = ThemeMode.DARK) }

                assertEquals(UserPreferences(themeMode = ThemeMode.DARK), awaitItem())
            }
        }

    @Test
    fun `setters write through to the repository`() =
        runTest {
            viewModel.setThemeMode(ThemeMode.LIGHT)
            viewModel.setDynamicColor(true)

            viewModel.preferences.test {
                assertEquals(UserPreferences(themeMode = ThemeMode.LIGHT, dynamicColor = true), expectMostRecentItem())
            }
        }
}
