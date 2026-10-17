package com.matheus.callmanager.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme

/** Thin wrapper over [TopAppBar] so every screen's app bar styling stays in one place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CmTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        actions = { actions() },
        modifier = modifier,
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CmTopAppBarPreview() {
    CallManagerTheme {
        CmTopAppBar(title = "Recent calls")
    }
}
