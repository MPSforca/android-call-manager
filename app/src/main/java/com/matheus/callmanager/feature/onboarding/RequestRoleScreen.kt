package com.matheus.callmanager.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RequestRoleScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Call Manager needs to be your call-screening app to block calls silently and log them.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRequest) {
            Text("Set as call-screening app")
        }
    }
}
