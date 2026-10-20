package com.sforca.callmanager.feature.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sforca.callmanager.R
import com.sforca.callmanager.core.data.rule.TraceOutcome
import com.sforca.callmanager.core.designsystem.component.CmCard
import com.sforca.callmanager.core.designsystem.component.CmPhoneTextField
import com.sforca.callmanager.core.designsystem.component.StatusPill
import com.sforca.callmanager.core.designsystem.component.StatusTone
import com.sforca.callmanager.core.designsystem.theme.LocalSpacing
import com.sforca.callmanager.core.model.ScreeningDecision
import com.sforca.callmanager.core.telephony.PhoneNumberFormatter

/**
 * "What would happen?" card: the user types a number (and optionally a caller name) and sees the
 * verdict of the rules as they are right now, updating as they change a rule above it.
 * [onChange] receives the number normalised for storage, or an empty string when the field is blank.
 */
@Composable
internal fun RulePreviewCard(
    preview: RulePreview?,
    onChange: (number: String, callerName: String?) -> Unit,
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    var number by rememberSaveable { mutableStateOf("") }
    var callerName by rememberSaveable { mutableStateOf("") }

    fun publish() {
        val region = PhoneNumberFormatter.defaultRegion(context)
        val normalised = if (number.isBlank()) "" else PhoneNumberFormatter.normalizeForStorage(context, number, region)
        onChange(normalised, callerName.ifBlank { null })
    }

    CmCard(modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small)) {
        Text(stringResource(R.string.rules_preview_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.rules_preview_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CmPhoneTextField(
            value = number,
            onValueChange = {
                number = it
                publish()
            },
            label = stringResource(R.string.rules_preview_number_label),
            modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
        )
        OutlinedTextField(
            value = callerName,
            onValueChange = {
                callerName = it
                publish()
            },
            label = { Text(stringResource(R.string.rules_preview_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
        )
        if (preview != null && number.isNotBlank()) {
            PreviewVerdict(preview)
        }
    }
}

@Composable
private fun PreviewVerdict(preview: RulePreview) {
    val spacing = LocalSpacing.current
    val decidedBy =
        preview.trace.steps
            .first { it.outcome == TraceOutcome.DECIDED }
            .rule
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        when (preview.trace.decision) {
            is ScreeningDecision.Allow ->
                StatusPill(stringResource(R.string.rules_preview_allowed), StatusTone.ALLOWED)
            is ScreeningDecision.Reject ->
                StatusPill(stringResource(R.string.rules_preview_blocked), StatusTone.BLOCKED)
        }
        Text(
            stringResource(R.string.rules_preview_decided_by, stringResource(RuleCatalog.forRule(decidedBy).title)),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
