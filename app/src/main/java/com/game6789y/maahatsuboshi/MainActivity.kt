package com.game6789y.maahatsuboshi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("maahatsuboshi")
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { Dashboard() } } }
    }
}

private data class ConnectionInput(
    val host: String,
    val connectPort: String,
    val pairingPort: String,
    val pairingCode: String
)

private fun validateConnection(input: ConnectionInput): String? {
    val octets = input.host.trim().split(".")
    if (octets.size != 4 || octets.any { part ->
        part.isEmpty() || part.length > 3 || part.any { !it.isDigit() } ||
            (part.length > 1 && part.startsWith("0")) || (part.toIntOrNull() == null || part.toIntOrNull() !in 0..255)
    }) return "Enter a valid IPv4 address."
    val connect = input.connectPort.toIntOrNull()
    if (connect == null || connect !in 1..65535) return "ADB connect port must be between 1 and 65535."
    val pairing = input.pairingPort.toIntOrNull()
    if (pairing == null || pairing !in 1..65535) return "Pairing port must be between 1 and 65535."
    if (!input.pairingCode.matches(Regex("\\d{6}"))) return "Pairing code must contain exactly 6 digits."
    return null
}

@Composable
private fun Dashboard() {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5555") }
    var pairingPort by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Not connected") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var taskStatus by remember { mutableStateOf("No task selected") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("MaaHatsuboshiTA", style = MaterialTheme.typography.headlineMedium)
        Text("Android controller · Native engine foundation")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remote Android device", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(host, { host = it; validationError = null }, Modifier.fillMaxWidth(), label = { Text("Device IPv4 address") }, singleLine = true)
                OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5); validationError = null }, Modifier.fillMaxWidth(), label = { Text("ADB connect port") }, singleLine = true)
                OutlinedTextField(pairingPort, { pairingPort = it.filter(Char::isDigit).take(5); validationError = null }, Modifier.fillMaxWidth(), label = { Text("Wireless debugging pairing port") }, singleLine = true)
                OutlinedTextField(pairingCode, { pairingCode = it.filter(Char::isDigit).take(6); validationError = null }, Modifier.fillMaxWidth(), label = { Text("6-digit pairing code") }, singleLine = true)
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val error = validateConnection(ConnectionInput(host, port, pairingPort, pairingCode))
                        validationError = error
                        status = if (error == null) {
                            "Settings valid. ADB pairing/transport is not implemented yet."
                        } else "Check connection settings."
                    }) { Text("Validate settings") }
                    OutlinedButton(onClick = {
                        status = "Disconnected"
                        validationError = null
                    }) { Text("Reset status") }
                }
                Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Automation tasks", style = MaterialTheme.typography.titleMedium)
                Text("Daily · Contest · Farm New Account")
                Text("Task execution is disabled until authenticated ADB transport, screen capture, and recognition are implemented.")
                OutlinedButton(onClick = { taskStatus = "Task engine scaffold only — execution is not connected." }) {
                    Text("Check task engine")
                }
                Text(taskStatus, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Native engine", style = MaterialTheme.typography.titleMedium)
                Text(nativeEngineVersion())
                Text("Rust task core is tested independently and is not yet linked into the Android app.")
            }
        }
        Text(
            "This build validates connection settings only. It does not pair with or control another device.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private external fun nativeEngineVersion(): String
