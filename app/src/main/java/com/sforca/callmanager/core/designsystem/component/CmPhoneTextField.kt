package com.sforca.callmanager.core.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.telephony.PhoneNumberFormatter

/**
 * A phone-number input that formats as the user types. [value] and [onValueChange] carry the
 * *displayed* (formatted) text; callers normalise with `PhoneNumberFormatter.normalizeForStorage`.
 */
@Composable
fun CmPhoneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    defaultRegion: String? = null,
) {
    val context = LocalContext.current
    val formatter =
        remember(defaultRegion) {
            PhoneNumberFormatter.newAsYouTypeFormatter(
                context,
                defaultRegion ?: PhoneNumberFormatter.defaultRegion(context),
            )
        }
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(PhoneNumberFormatter.formatAsYouType(formatter, it)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = modifier,
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CmPhoneTextFieldPreview() {
    CallManagerTheme {
        CmPhoneTextField(value = "+1 415 555 0100", onValueChange = {}, label = "Phone number", defaultRegion = "US")
    }
}
