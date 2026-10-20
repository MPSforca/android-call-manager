package com.matheus.callmanager.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.matheus.callmanager.R
import com.matheus.callmanager.core.designsystem.component.CallRow
import com.matheus.callmanager.core.designsystem.component.CmCard
import com.matheus.callmanager.core.designsystem.component.CmTextButton
import com.matheus.callmanager.core.designsystem.component.PermissionBanner
import com.matheus.callmanager.core.designsystem.component.SectionHeader
import com.matheus.callmanager.core.designsystem.component.StatCard
import com.matheus.callmanager.core.designsystem.component.StatusTone
import com.matheus.callmanager.core.designsystem.theme.LocalSpacing
import com.matheus.callmanager.core.model.RecentCallGroup
import com.matheus.callmanager.core.telephony.PhoneNumberFormatter
import com.matheus.callmanager.feature.onboarding.OnboardingStep
import com.matheus.callmanager.feature.recent.callOutcomeLabel
import com.matheus.callmanager.feature.recent.isBlocked
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Stateless body of [HomeScreen]: the protection status with its next fix, today's counts and the
 * newest activity. [today] and [zone] are parameters so the counts are deterministic under test.
 */
@Composable
internal fun HomeContent(
    state: HomeUiState,
    today: LocalDate,
    zone: ZoneId,
    actions: HomeActions,
) {
    val spacing = LocalSpacing.current
    val summary =
        remember(
            state.calls,
            state.blockedCalls,
            today,
            zone,
        ) { summarize(state.calls, state.blockedCalls, today, zone) }
    LazyColumn(
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(bottom = spacing.large),
    ) {
        item(key = "status") {
            ProtectionHeader(state.permissions.protectionStatus(), Modifier.padding(spacing.large))
        }
        state.permissions.nextStep?.let { step ->
            item(key = "fix") {
                PermissionBanner(
                    message = stringResource(step.messageRes()),
                    actionLabel = stringResource(R.string.home_fix),
                    onAction = { actions.onFix(step) },
                )
            }
        }
        item(key = "stats") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(spacing.large),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                StatCard(value = summary.blockedToday.toString(), label = stringResource(R.string.home_blocked_today))
                StatCard(value = summary.allowedToday.toString(), label = stringResource(R.string.home_allowed_today))
            }
        }
        item(key = "activity-header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(title = stringResource(R.string.home_activity), modifier = Modifier.weight(1f))
                CmTextButton(text = stringResource(R.string.home_see_all), onClick = actions.onSeeAll)
            }
        }
        if (summary.activity.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.home_activity_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacing.large),
                )
            }
        } else {
            items(summary.activity, key = { it.number }) { group ->
                ActivityRow(group, state, zone, actions.onCallSelected)
            }
        }
    }
}

@Composable
private fun ProtectionHeader(
    status: ProtectionStatus,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val active = status == ProtectionStatus.ACTIVE
    CmCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Icon(
                imageVector = if (active) Icons.Filled.Shield else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                stringResource(if (active) R.string.home_status_on else R.string.home_status_attention),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            stringResource(if (active) R.string.home_status_on_detail else R.string.home_status_attention_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.small),
        )
    }
}

@Composable
private fun ActivityRow(
    group: RecentCallGroup,
    state: HomeUiState,
    zone: ZoneId,
    onCallSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val formattedNumber =
        remember(group.number) {
            PhoneNumberFormatter.format(context, group.number, PhoneNumberFormatter.defaultRegion(context))
        }
    val latest = group.calls.first()
    CallRow(
        displayName = group.name ?: formattedNumber,
        number = formattedNumber.takeIf { group.name != null }.orEmpty(),
        timeLabel =
            remember(latest.timestamp, zone) {
                DateTimeFormatter
                    .ofLocalizedTime(
                        FormatStyle.SHORT,
                    ).withZone(zone)
                    .format(Instant.ofEpochMilli(latest.timestamp))
            },
        outcomeLabel = callOutcomeLabel(latest, state.blockedCalls),
        outcomeTone = if (latest.isBlocked(state.blockedCalls)) StatusTone.BLOCKED else StatusTone.UNKNOWN,
        modifier = Modifier.clickable { onCallSelected(group.number) }.padding(horizontal = LocalSpacing.current.large),
    )
}

private fun OnboardingStep.messageRes(): Int =
    when (this) {
        OnboardingStep.ROLE -> R.string.home_missing_role
        OnboardingStep.PERMISSIONS -> R.string.home_missing_permissions
        OnboardingStep.BATTERY -> R.string.home_missing_battery
    }
