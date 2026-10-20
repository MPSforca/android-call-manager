package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme

/**
 * Themed [Scaffold] wrapper. Screens compose this instead of calling `Scaffold` directly so a
 * future change to the app shell's chrome (e.g. a shared snackbar host) only needs to land here.
 */
@Composable
fun CmScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        content = content,
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CmScaffoldPreview() {
    CallManagerTheme {
        CmScaffold(topBar = { CmTopAppBar(title = "Recent calls") }) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Text(text = "Content", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
