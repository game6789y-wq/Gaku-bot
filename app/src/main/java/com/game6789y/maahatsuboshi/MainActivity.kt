package com.game6789y.maahatsuboshi

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdbEndpoint(
    val serviceType: String,
    val serviceName: String,
    val host: String,
    val port: Int
)

class MainActivity : ComponentActivity() {
    private lateinit var discovery: AdbNsdDiscovery
    private val endpoints = mutableStateListOf<AdbEndpoint>()
    private val discoveryStatus = mutableStateOf("Discovery stopped")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("maahatsuboshi")
        discovery = AdbNsdDiscovery(
            context = applicationContext,
            onEndpoint = { endpoint ->
                runOnUiThread {
                    endpoints.removeAll { it.serviceName == endpoint.serviceName && it.serviceType == endpoint.serviceType }
                    endpoints.add(endpoint)
                }
            },
            onLost = { serviceName, kind ->
                runOnUiThread { endpoints.removeAll { it.serviceName == serviceName && it.serviceType == kind } }
            },
            onStatus = { message -> runOnUiThread { discoveryStatus.value = message } }
        )
        setContent {
            var selectedHost by remember { mutableStateOf("") }
            var selectedConnectPort by remember { mutableStateOf("5555") }
            var selectedPairingPort by remember { mutableStateOf("") }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Dashboard(
                        endpoints = endpoints,
                        discoveryStatus = discoveryStatus.value,
                        onStartDiscovery = { endpoints.clear(); discovery.start() },
                        onStopDiscovery = { discovery.stop() },
                        initialHost = selectedHost,
                        initialConnectPort = selectedConnectPort,
                        initialPairingPort = selectedPairingPort,
                        onSelectEndpoint = { endpoint ->
                            selectedHost = endpoint.host
                            if (endpoint.serviceType == "pairing") selectedPairingPort = endpoint.port.toString()
                            else selectedConnectPort = endpoint.port.toString()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (::discovery.isInitialized) discovery.stop()
        super.onDestroy()
    }
}

private class AdbNsdDiscovery(
    context: Context,
    private val onEndpoint: (AdbEndpoint) -> Unit,
    private val onLost: (String, String) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceTypes = listOf("_adb-tls-pairing._tcp.", "_adb-tls-connect._tcp.")
    private val listeners = mutableMapOf<String, NsdManager.DiscoveryListener>()
    @Volatile private var running = false

    @Suppress("DEPRECATION")
    fun start() {
        if (running) return
        running = true
        onStatus("Searching local network for ADB Wireless Debugging services…")
        serviceTypes.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (serviceInfo.serviceType.contains("_adb-tls-")) {
                        try {
                            manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                                    onStatus("Found service, but could not resolve it (error $errorCode).")
                                }
                                override fun onServiceResolved(info: NsdServiceInfo) {
                                    val address = info.host?.hostAddress ?: return
                                    val kind = if (info.serviceType.contains("pairing")) "pairing" else "connect"
                                    onEndpoint(AdbEndpoint(kind, info.serviceName, address, info.port))
                                }
                            })
                        } catch (_: IllegalArgumentException) {
                            onStatus("ADB service resolution is busy; keep scanning.")
                        }
                    }
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    val kind = if (serviceInfo.serviceType.contains("pairing")) "pairing" else "connect"
                    onLost(serviceInfo.serviceName, kind)
                    onStatus("An ADB service disappeared from the network.")
                }
                override fun onDiscoveryStopped(serviceType: String) = Unit
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    onStatus("Discovery failed for $serviceType (error $errorCode).")
                    try { manager.stopServiceDiscovery(this) } catch (_: Exception) { }
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    onStatus("Could not stop discovery cleanly (error $errorCode).")
                }
            }
            listeners[type] = listener
            try {
                manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (_: Exception) {
                onStatus("Could not start local network discovery for $type.")
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        listeners.values.forEach { listener ->
            try { manager.stopServiceDiscovery(listener) } catch (_: Exception) { }
        }
        listeners.clear()
        onStatus("Discovery stopped")
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
            (part.length > 1 && part.startsWith("0")) ||
            (part.toIntOrNull() == null || part.toIntOrNull() !in 0..255)
    }) return "Enter a valid IPv4 address."
    val connect = input.connectPort.toIntOrNull()
    if (connect == null || connect !in 1..65535) return "ADB connect port must be between 1 and 65535."
    val pairing = input.pairingPort.toIntOrNull()
    if (pairing == null || pairing !in 1..65535) return "Pairing port must be between 1 and 65535."
    if (!input.pairingCode.matches(Regex("\\d{6}"))) return "Pairing code must contain exactly 6 digits."
    return null
}

@Composable
private fun Dashboard(
    endpoints: List<AdbEndpoint>,
    discoveryStatus: String,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    initialHost: String,
    initialConnectPort: String,
    initialPairingPort: String,
    onSelectEndpoint: (AdbEndpoint) -> Unit
) {
    var host by remember(initialHost) { mutableStateOf(initialHost) }
    var port by remember(initialConnectPort) { mutableStateOf(initialConnectPort) }
    var pairingPort by remember(initialPairingPort) { mutableStateOf(initialPairingPort) }
    var pairingCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Not connected") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var taskStatus by remember { mutableStateOf("No task selected") }
    var identityFingerprint by remember { mutableStateOf<String?>(null) }
    var identityStatus by remember { mutableStateOf("No local identity loaded") }
    var identityBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("MaaHatsuboshiTA", style = MaterialTheme.typography.headlineMedium)
        Text("Android controller · Native engine foundation")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ADB Wireless Debugging discovery", style = MaterialTheme.typography.titleMedium)
                Text("Connect both Android devices to the same Wi-Fi. On the target device, enable Developer options → Wireless debugging.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStartDiscovery) { Text("Scan network") }
                    OutlinedButton(onClick = onStopDiscovery) { Text("Stop scan") }
                }
                Text(discoveryStatus, style = MaterialTheme.typography.bodySmall)
                if (endpoints.isEmpty()) {
                    Text("No ADB services discovered yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    endpoints.forEach { endpoint ->
                        Card(
                            onClick = { onSelectEndpoint(endpoint) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("ADB ${endpoint.serviceType} service · Tap to use", style = MaterialTheme.typography.labelLarge)
                                Text("${endpoint.host}:${endpoint.port}")
                                Text(endpoint.serviceName, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    if (endpoint.serviceType == "pairing") "Fills the pairing port only."
                                    else "Fills the ADB connect port only.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Manual device details", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(host, { host = it; validationError = null }, Modifier.fillMaxWidth(), label = { Text("Device IPv4 address") }, singleLine = true)
                OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5); validationError = null }, Modifier.fillMaxWidth(), label = { Text("ADB connect port") }, singleLine = true)
                OutlinedTextField(pairingPort, { pairingPort = it.filter(Char::isDigit).take(5); validationError = null }, Modifier.fillMaxWidth(), label = { Text("Wireless debugging pairing port") }, singleLine = true)
                OutlinedTextField(pairingCode, { pairingCode = it.filter(Char::isDigit).take(6); validationError = null }, Modifier.fillMaxWidth(), label = { Text("6-digit pairing code") }, singleLine = true)
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    val error = validateConnection(ConnectionInput(host, port, pairingPort, pairingCode))
                    validationError = error
                    status = if (error == null) "Settings valid. ADB authentication and transport are not implemented yet." else "Check connection settings."
                }) { Text("Validate settings") }
                Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Local ADB identity", style = MaterialTheme.typography.titleMedium)
                Text("Create or load a device-local RSA-2048 key in Android Keystore. The private key is non-exportable through this app.")
                Button(enabled = !identityBusy, onClick = {
                    identityBusy = true
                    identityStatus = "Working with Android Keystore…" 
                    scope.launch {
                        try {
                            val fingerprint = withContext(Dispatchers.IO) { LocalAdbIdentity.loadOrCreate() }
                            identityFingerprint = fingerprint
                            identityStatus = "Local identity is ready on this device."
                        } catch (error: Exception) {
                            identityStatus = "Keystore error: " + error.javaClass.simpleName
                        } finally { identityBusy = false }
                    }
                }) { Text(if (identityBusy) "Please wait…" else "Create / load local identity") }
                Text(identityStatus, style = MaterialTheme.typography.bodySmall)
                identityFingerprint?.let {
                    Text("Public-key SHA-256 fingerprint", style = MaterialTheme.typography.labelMedium)
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text("This identity is not yet in ADB wire format and is not used for pairing or authentication.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Automation tasks", style = MaterialTheme.typography.titleMedium)
                Text("Daily · Contest · Farm New Account")
                Text("Task execution remains disabled until authenticated ADB transport, screen capture, and recognition are implemented.")
                OutlinedButton(onClick = { taskStatus = "Task engine scaffold only — execution is not connected." }) { Text("Check task engine") }
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
        Text("Network scanning only discovers ADB service advertisements. It does not reveal pairing codes or bypass Android authorization.", style = MaterialTheme.typography.bodySmall)
    }
}

private external fun nativeEngineVersion(): String
