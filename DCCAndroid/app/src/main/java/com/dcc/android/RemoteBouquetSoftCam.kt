package com.dcc.android

import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

private val RemoteNavy = Color(0xFF102A43)
private val RemoteBlue = Color(0xFF246BFD)
private val RemoteMuted = Color(0xFF68788B)
private val RemoteBorder = Color(0xFFE5EBF2)
private val RemoteRed = Color(0xFF9C2F26)

private data class RemoteBouquet(val reference: String, val name: String)
private data class RemoteService(val reference: String, val name: String)

@Composable
fun RemoteBouquetSoftCamCard(receiverIp: String, username: String, password: String) {
    var webPort by remember { mutableStateOf("80") }
    var status by remember { mutableStateOf("Not checked") }
    var busy by remember { mutableStateOf(false) }
    var bouquets by remember { mutableStateOf<List<RemoteBouquet>>(emptyList()) }
    var selectedBouquet by remember { mutableStateOf<RemoteBouquet?>(null) }
    var services by remember { mutableStateOf<List<RemoteService>>(emptyList()) }
    var softCamStatus by remember { mutableStateOf("SoftCam status unknown") }
    val scope = rememberCoroutineScope()

    fun runAction(action: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            status = "Working…"
            try {
                action()
            } catch (e: Exception) {
                status = e.message ?: "Operation failed"
            } finally {
                busy = false
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, RemoteBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Remote bouquets and SoftCam", color = RemoteNavy, fontWeight = FontWeight.Bold)
            Text("Browse TV bouquets and check or restart supported SoftCam services.", color = RemoteMuted, style = MaterialTheme.typography.bodySmall)

            OutlinedTextField(
                value = webPort,
                onValueChange = { webPort = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Web/SoftCam API port") },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    enabled = !busy && receiverIp.isNotBlank(),
                    onClick = {
                        runAction {
                            val port = webPort.toIntOrNull() ?: 80
                            val result = withContext(Dispatchers.IO) {
                                RemoteReceiverApi.getBouquets(receiverIp, port, username, password)
                            }
                            bouquets = result
                            selectedBouquet = result.firstOrNull()
                            services = selectedBouquet?.let {
                                RemoteReceiverApi.getServices(receiverIp, port, username, password, it.reference)
                            } ?: emptyList()
                            status = "Loaded ${result.size} bouquets"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Load bouquets") }

                OutlinedButton(
                    enabled = !busy && receiverIp.isNotBlank(),
                    onClick = {
                        runAction {
                            softCamStatus = withContext(Dispatchers.IO) {
                                RemoteReceiverApi.checkSoftCam(receiverIp, webPort.toIntOrNull() ?: 80, username, password)
                            }
                            status = "SoftCam checked"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Check SoftCam") }
            }

            Text(status, color = RemoteMuted, style = MaterialTheme.typography.bodySmall)

            bouquets.forEach { bouquet ->
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        runAction {
                            val port = webPort.toIntOrNull() ?: 80
                            selectedBouquet = bouquet
                            services = withContext(Dispatchers.IO) {
                                RemoteReceiverApi.getServices(receiverIp, port, username, password, bouquet.reference)
                            }
                            status = "Loaded ${services.size} services"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(bouquet.name) }
            }

            selectedBouquet?.let { bouquet ->
                HorizontalDivider(color = RemoteBorder)
                Text("${bouquet.name} · ${services.size} services", color = RemoteNavy, fontWeight = FontWeight.SemiBold)
                services.take(100).forEach { service ->
                    Text(service.name, color = RemoteNavy, modifier = Modifier.fillMaxWidth())
                }
            }

            HorizontalDivider(color = RemoteBorder)
            Text(softCamStatus, color = if (softCamStatus.startsWith("Available")) RemoteBlue else RemoteMuted)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = !busy && receiverIp.isNotBlank(),
                    onClick = {
                        runAction {
                            softCamStatus = withContext(Dispatchers.IO) {
                                RemoteReceiverApi.restartSoftCam(receiverIp, webPort.toIntOrNull() ?: 80, username, password)
                            }
                            status = "SoftCam restart request sent"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Restart") }

                OutlinedButton(
                    enabled = !busy && receiverIp.isNotBlank(),
                    onClick = {
                        runAction {
                            softCamStatus = withContext(Dispatchers.IO) {
                                RemoteReceiverApi.reloadSoftCam(receiverIp, webPort.toIntOrNull() ?: 80, username, password)
                            }
                            status = "SoftCam reload request sent"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Reload") }
            }

            Text(
                "Remote editing and restart actions depend on the receiver image and enabled Web Interface APIs.",
                color = RemoteRed,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private object RemoteReceiverApi {
    private const val MAX_RESPONSE_BYTES = 5 * 1024 * 1024

    fun getBouquets(host: String, port: Int, username: String, password: String): List<RemoteBouquet> {
        val xml = request(host, port, username, password, "/web/getbouquets?stype=tv")
        val references = Regex("<e2servicereference(?:\\s[^>]*)?>(.*?)</e2servicereference>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { it.groupValues[1].trim() }.toList()
        val names = Regex("<e2servicename(?:\\s[^>]*)?>(.*?)</e2servicename>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { decodeXml(it.groupValues[1]) }.toList()
        return references.indices.mapNotNull { index ->
            val name = names.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() } ?: "Bouquet ${index + 1}"
            RemoteBouquet(references[index], name)
        }.distinctBy { it.reference }
    }

    fun getServices(host: String, port: Int, username: String, password: String, bouquetReference: String): List<RemoteService> {
        val xml = request(host, port, username, password, "/web/getservices?sRef=" + java.net.URLEncoder.encode(bouquetReference, Charsets.UTF_8.name()))
        val references = Regex("<e2servicereference(?:\\s[^>]*)?>(.*?)</e2servicereference>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { it.groupValues[1].trim() }.toList()
        val names = Regex("<e2servicename(?:\\s[^>]*)?>(.*?)</e2servicename>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { decodeXml(it.groupValues[1]) }.toList()
        return references.indices.mapNotNull { index ->
            val reference = references[index]
            val name = names.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() } ?: reference
            RemoteService(reference, name)
        }.distinctBy { it.reference }
    }

    fun checkSoftCam(host: String, port: Int, username: String, password: String): String {
        val endpoints = listOf("/api/softcam/status", "/web/softcamstatus", "/api/status")
        for (endpoint in endpoints) {
            val result = runCatching { request(host, port, username, password, endpoint) }.getOrNull()
            if (result != null) {
                val cleaned = stripTags(result).replace("  ", " ").trim()
                if (cleaned.isNotBlank()) return "Available: ${cleaned.take(500)}"
            }
        }
        return "SoftCam status is not exposed by this receiver API"
    }

    fun restartSoftCam(host: String, port: Int, username: String, password: String): String {
        val endpoints = listOf("/api/softcam/restart", "/web/softcamrestart")
        for (endpoint in endpoints) {
            val result = runCatching { request(host, port, username, password, endpoint) }.getOrNull()
            if (result != null) return "Available: SoftCam restart accepted"
        }
        return "SoftCam restart API is not exposed by this receiver"
    }

    fun reloadSoftCam(host: String, port: Int, username: String, password: String): String {
        val endpoints = listOf("/api/softcam/reload", "/web/softcamreload")
        for (endpoint in endpoints) {
            val result = runCatching { request(host, port, username, password, endpoint) }.getOrNull()
            if (result != null) return "Available: SoftCam reload accepted"
        }
        return "SoftCam reload API is not exposed by this receiver"
    }

    private fun request(host: String, port: Int, username: String, password: String, path: String): String {
        require(host.isNotBlank() && port in 1..65535)
        val connection = (URL("http://${host.trim()}:$port$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/xml, application/json, text/plain, */*")
            if (username.isNotBlank()) {
                val credentials = Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                setRequestProperty("Authorization", "Basic $credentials")
            }
        }
        try {
            if (connection.responseCode !in 200..299) throw java.io.IOException("Receiver returned HTTP ${connection.responseCode}")
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) throw java.io.IOException("Receiver response is too large")
                    output.write(buffer, 0, count)
                }
            }
            return output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }

    private fun decodeXml(value: String): String = value
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")

    private fun stripTags(value: String): String = value.replace(Regex("<[^>]+>"), " ")
}
