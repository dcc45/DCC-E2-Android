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
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun DevicesScreen(receiverIp: String) {
    var scanStatus by remember { mutableStateOf("Ready to scan") }
    var foundReceiver by remember { mutableStateOf<String?>(null) }
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
                    onClick = {
                        foundReceiver = null
                        scanStatus = "Scanning local network…"
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val prefix = receiverIp.substringBeforeLast('.', "192.168.1")
                            for (i in 1..254) {
                                val ip = "$prefix.$i"
                                if (probeReceiver(ip)) {
                                    foundReceiver = ip
                                    scanStatus = "Dreambox found: $ip"
                                    break
                                }
                            }
                            if (foundReceiver == null) scanStatus = "No Dreambox found on this network"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Scan local network") }
            }
        }
    }
}

private fun probeReceiver(host: String): Boolean = try {
    val connection = (URL("http://$host:80/web/about").openConnection() as HttpURLConnection).apply {
        connectTimeout = 350
        readTimeout = 500
    }
    val code = connection.responseCode
    connection.disconnect()
    code in 200..499
} catch (_: Exception) {
    false
}

@Composable
fun PvrScreen(receiverIp: String, username: String, password: String) {
    var status by remember { mutableStateOf("Ready") }
    var timers by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PVR / Timers", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = Color(0xFF102A43), fontWeight = FontWeight.Bold)
        Text("View and manage Dreambox recordings and timers.", color = Color(0xFF68788B))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFFE5EBF2))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        status = "Loading timers…"
                        kotlinx.coroutines.MainScope().launch(kotlinx.coroutines.Dispatchers.IO) {
                            timers = PvrApi.listTimers(receiverIp, username, password)
                            status = "Loaded ${timers.size} timers"
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Load timers") }
                    OutlinedButton(onClick = {
                        status = "Loading recordings…"
                        kotlinx.coroutines.MainScope().launch(kotlinx.coroutines.Dispatchers.IO) {
                            timers = PvrApi.listRecordings(receiverIp, username, password)
                            status = "Loaded ${timers.size} recordings"
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Load recordings") }
                }
                Text(status, color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                timers.take(100).forEach { Text(it, color = Color(0xFF102A43), modifier = Modifier.fillMaxWidth()) }
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
