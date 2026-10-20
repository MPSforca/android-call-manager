package com.sforca.callmanager.feature.rules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sforca.callmanager.R
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing

/** Bottom sheet explaining one rule in plain language, with a worked example. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RuleInfoSheet(
    descriptor: RuleDescriptor,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large, vertical = spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.rules_info_position, descriptor.order),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = stringResource(descriptor.title), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(descriptor.description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = spacing.small),
            )
            Text(
                text = stringResource(R.string.rules_info_example_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = spacing.medium),
            )
            Text(
                text = stringResource(descriptor.example),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
