package com.dcc.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun DevicesScreen(receiverIp: String) {
    var scanStatus by remember { mutableStateOf("Ready to scan") }
    var foundReceiver by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Devices", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = Color(0xFF102A43), fontWeight = FontWeight.Bold)
        Text("Discover Dreamboxes on the local network.", color = Color(0xFF68788B))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFFE5EBF2))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Network receiver", color = Color(0xFF102A43), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = foundReceiver ?: receiverIp,
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Receiver IP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Text(scanStatus, color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                Button(
                    enabled = !scanning,
                    onClick = {
                        scanning = true
                        foundReceiver = null
                        scanStatus = "Scanning local network…"
                        scope.launch {
                            val found = withContext(Dispatchers.IO) {
                                val prefix = receiverIp.substringBeforeLast('.', "192.168.1")
                                (1..254).firstNotNullOfOrNull { suffix ->
                                    val candidate = "$prefix.$suffix"
                                    candidate.takeIf { probeReceiver(candidate) }
                                }
                            }
                            foundReceiver = found
                            scanStatus = found?.let { "Dreambox found: $it" } ?: "No Dreambox found on this network"
                            scanning = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (scanning) "Scanning…" else "Scan local network") }
            }
        }
    }
}

private fun probeReceiver(host: String): Boolean = try {
    val connection = (URL("http://$host:80/web/about").openConnection() as HttpURLConnection).apply {
        connectTimeout = 350
        readTimeout = 500
    }
    try {
        connection.responseCode in 200..499
    } finally {
        connection.disconnect()
    }
} catch (_: Exception) {
    false
}

@Composable
fun PvrScreen(receiverIp: String, username: String, password: String) {
    var status by remember { mutableStateOf("Ready") }
    var items by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PVR / Timers", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = Color(0xFF102A43), fontWeight = FontWeight.Bold)
        Text("View Dreambox recordings and timers.", color = Color(0xFF68788B))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFFE5EBF2))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        enabled = !busy && receiverIp.isNotBlank(),
                        onClick = {
                            busy = true
                            status = "Loading timers…"
                            scope.launch {
                                try {
                                    items = withContext(Dispatchers.IO) { PvrApi.listTimers(receiverIp, username, password) }
                                    status = "Loaded ${items.size} timers"
                                } catch (e: Exception) {
                                    status = e.message ?: "Unable to load timers"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Load timers") }
                    OutlinedButton(
                        enabled = !busy && receiverIp.isNotBlank(),
                        onClick = {
                            busy = true
                            status = "Loading recordings…"
                            scope.launch {
                                try {
                                    items = withContext(Dispatchers.IO) { PvrApi.listRecordings(receiverIp, username, password) }
                                    status = "Loaded ${items.size} recordings"
                                } catch (e: Exception) {
                                    status = e.message ?: "Unable to load recordings"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Load recordings") }
                }
                Text(status, color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                items.take(100).forEach { Text(it, color = Color(0xFF102A43), modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

private object PvrApi {
    fun listTimers(host: String, username: String, password: String): List<String> {
        val xml = request(host, 80, username, password, "/web/timerlist")
        return Regex("<e2name(?:\\s[^>]*)?>(.*?)</e2name>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }
            .filter { it.isNotBlank() }.toList()
    }

    fun listRecordings(host: String, username: String, password: String): List<String> {
        val xml = request(host, 80, username, password, "/web/movielist")
        return Regex("<e2title(?:\\s[^>]*)?>(.*?)</e2title>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }
            .filter { it.isNotBlank() }.toList()
    }

    private fun request(host: String, port: Int, username: String, password: String, path: String): String {
        require(host.isNotBlank()) { "Configure a receiver IP in Settings" }
        val connection = (URL("http://${host.trim()}:$port$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 8000
            if (username.isNotBlank()) {
                val credentials = android.util.Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                setRequestProperty("Authorization", "Basic $credentials")
            }
        }
        return try {
            if (connection.responseCode !in 200..299) throw java.io.IOException("Receiver returned HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
