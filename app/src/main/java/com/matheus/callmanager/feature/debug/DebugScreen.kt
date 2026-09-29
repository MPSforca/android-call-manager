package com.matheus.callmanager.feature.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Debug-only: runs the same rule evaluation the real CallScreeningService uses, without a real call. */
@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel()) {
    var number by remember { mutableStateOf("") }
    var callerName by remember { mutableStateOf("") }
    val result by viewModel.simulationResult.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Simulate an incoming call to test your rules without dialing your device.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text("Phone number") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        OutlinedTextField(
            value = callerName,
            onValueChange = { callerName = it },
            label = { Text("Caller ID name (optional)") },
            placeholder = { Text("e.g. PagerDuty Outgoing Numbers") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Button(
            onClick = { viewModel.simulateIncomingCall(number, callerName.ifBlank { null }) },
            enabled = number.isNotBlank(),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text("Simulate call")
        }
        result?.let {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (it.startsWith("ALLOWED")) Icons.Filled.CheckCircle else Icons.Filled.Block,
                    contentDescription = null,
                )
                Text(it, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
