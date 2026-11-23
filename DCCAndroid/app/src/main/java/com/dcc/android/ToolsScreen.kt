package com.dcc.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.time.Duration

private val ToolsNavy = Color(0xFF102A43)
private val ToolsBlue = Color(0xFF246BFD)
private val ToolsMuted = Color(0xFF68788B)
private val ToolsBorder = Color(0xFFE5EBF2)
private val ToolsRed = Color(0xFF9C2F26)

private data class ReceiverSnapshot(val model: String, val service: String, val signal: String)
private data class RemoteFile(val name: String, val directory: Boolean, val size: Long)
private enum class ConfirmAction(val title: String, val description: String, val state: Int) {
    RESTART_GUI("Restart Enigma2", "Restart the receiver interface.", 3),
    REBOOT("Reboot receiver", "Restart the DM900 completely.", 2)
}

@Composable
fun ToolsScreen(language: AppLanguage, receiverIp: String, username: String, password: String) {
    var host by remember(receiverIp) { mutableStateOf(receiverIp) }
    var webPort by remember { mutableStateOf("80") }
    var ftpPort by remember { mutableStateOf("21") }
    var telnetPort by remember { mutableStateOf("23") }
    var snapshot by remember { mutableStateOf<ReceiverSnapshot?>(null) }
    var status by remember { mutableStateOf("Not connected") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var screenshot by remember { mutableStateOf<Bitmap?>(null) }
    var pendingAction by remember { mutableStateOf<ConfirmAction?>(null) }
    var ftpItems by remember { mutableStateOf<List<RemoteFile>>(emptyList()) }
    var ftpPath by remember { mutableStateOf("/") }
    var ftpConnected by remember { mutableStateOf(false) }
    var ftpStatus by remember { mutableStateOf("FTP not connected") }
    var telnetStatus by remember { mutableStateOf("Telnet not checked") }
    val scope = rememberCoroutineScope()

    fun launchIo(action: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                action()
            } catch (e: Exception) {
                error = e.message ?: "Operation failed"
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("DM900 receiver tools", style = MaterialTheme.typography.headlineSmall, color = ToolsNavy, fontWeight = FontWeight.Bold)
            Text("Diagnostics, read-only FTP browsing, screenshots, and maintenance.", color = ToolsMuted)
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ToolsBorder)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Receiver connection", color = ToolsNavy, fontWeight = FontWeight.Bold)
                OutlinedTextField(host, { host = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Receiver IP or hostname") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(webPort, { webPort = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), label = { Text("Web port") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(ftpPort, { ftpPort = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), label = { Text("FTP port") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(enabled = !busy && host.isNotBlank(), onClick = {
                        launchIo {
                            snapshot = withContext(Dispatchers.IO) { Enigma2Client.snapshot(host, webPort.toIntOrNull() ?: 80, username, password) }
                            status = "Connected to ${host.trim()}"
                        }
                    }) { Text(if (busy) "Working…" else "Check receiver") }
                    Text(status, color = if (snapshot != null) ToolsBlue else ToolsMuted, style = MaterialTheme.typography.bodySmall)
                }
                snapshot?.let { info ->
                    HorizontalDivider(color = ToolsBorder)
                    Text("Model: ${info.model}", color = ToolsNavy)
                    Text("Current service: ${info.service}", color = ToolsNavy)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ToolsBorder)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.SettingsInputAntenna, null, tint = ToolsBlue)
                    Text("Signal meter", color = ToolsNavy, fontWeight = FontWeight.Bold)
                }
                Text("Reads service and tuner metrics from the receiver Web Interface.", color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(enabled = !busy && host.isNotBlank(), onClick = {
                    launchIo { snapshot = withContext(Dispatchers.IO) { Enigma2Client.snapshot(host, webPort.toIntOrNull() ?: 80, username, password) } }
                }, modifier = Modifier.fillMaxWidth()) { Text("Refresh signal metrics") }
                snapshot?.let { Text(it.signal, color = ToolsNavy, fontWeight = FontWeight.SemiBold) }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ToolsBorder)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Terminal, null, tint = ToolsBlue)
                    Text("Receiver screenshot", color = ToolsNavy, fontWeight = FontWeight.Bold)
                }
                Text("Capture the receiver on-screen image via the Web Interface.", color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(enabled = !busy && host.isNotBlank(), onClick = {
                    launchIo { screenshot = withContext(Dispatchers.IO) { Enigma2Client.screenshot(host, webPort.toIntOrNull() ?: 80, username, password) } }
                }, modifier = Modifier.fillMaxWidth()) { Text("Capture screenshot") }
                screenshot?.let { Image(it.asImageBitmap(), contentDescription = "Receiver screenshot", modifier = Modifier.fillMaxWidth().height(220.dp)) }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ToolsBorder)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Telnet diagnostics", color = ToolsNavy, fontWeight = FontWeight.Bold)
                Text("Checks only whether the port is reachable. No login or commands are sent.", color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(telnetPort, { telnetPort = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), label = { Text("Telnet port") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedButton(enabled = !busy && host.isNotBlank(), onClick = {
                    launchIo { telnetStatus = withContext(Dispatchers.IO) { TelnetDiagnostics.check(host, telnetPort.toIntOrNull() ?: 23) } }
                }) { Text("Test Telnet port") }
                Text(telnetStatus, color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ToolsBorder)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Build, null, tint = ToolsBlue)
                    Text("Maintenance and recovery", color = ToolsNavy, fontWeight = FontWeight.Bold)
                }
                Text("Restart or reboot is available only for receivers you own or administer.", color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy && host.isNotBlank(), onClick = { pendingAction = ConfirmAction.RESTART_GUI }, modifier = Modifier.weight(1f)) { Text("Restart Enigma2") }
                    Button(enabled = !busy && host.isNotBlank(), onClick = { pendingAction = ConfirmAction.REBOOT }, modifier = Modifier.weight(1f)) { Text("Reboot") }
                }
                HorizontalDivider(color = ToolsBorder)
                Text("For rescue/recovery, use the DM900's physical recovery controls and official verified firmware procedure. This app does not flash firmware or enter bootloader mode.", color = ToolsRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ToolsBorder)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("FTP file browser (read only)", color = ToolsNavy, fontWeight = FontWeight.Bold)
                Text("Lists remote files and folders only.", color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !busy && host.isNotBlank() && !ftpConnected, onClick = {
                        launchIo {
                            ftpItems = withContext(Dispatchers.IO) { FtpBrowser.list(host, ftpPort.toIntOrNull() ?: 21, username, password, "/") }
                            ftpPath = "/"
                            ftpConnected = true
                            ftpStatus = "Listed ${ftpItems.size} entries"
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Connect and list /") }
                    OutlinedButton(enabled = ftpConnected && !busy, onClick = { ftpConnected = false; ftpItems = emptyList(); ftpPath = "/"; ftpStatus = "FTP disconnected" }, modifier = Modifier.weight(1f)) { Text("Disconnect") }
                }
                Text(ftpStatus, color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
                if (ftpConnected) {
                    Text(ftpPath, color = ToolsNavy, fontWeight = FontWeight.SemiBold)
                    if (ftpPath != "/") TextButton(onClick = {
                        val parent = ftpPath.trimEnd('/').substringBeforeLast('/', "")
                        val newPath = if (parent.isBlank()) "/" else "$parent/"
                        launchIo { ftpItems = withContext(Dispatchers.IO) { FtpBrowser.list(host, ftpPort.toIntOrNull() ?: 21, username, password, newPath) }; ftpPath = newPath }
                    }) { Text(".. Parent folder") }
                    ftpItems.take(100).forEach { item ->
                        TextButton(enabled = item.directory && !busy, onClick = {
                            val child = if (ftpPath.endsWith('/')) ftpPath + item.name else "$ftpPath/${item.name}"
                            launchIo { ftpItems = withContext(Dispatchers.IO) { FtpBrowser.list(host, ftpPort.toIntOrNull() ?: 21, username, password, child) }; ftpPath = child.trimEnd('/') + "/" }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text((if (item.directory) "📁  " else "📄  ") + item.name + if (item.directory) "/" else " · ${item.size} bytes", modifier = Modifier.fillMaxWidth(), color = ToolsNavy)
                        }
                    }
                }
            }
        }

        error?.let { Text("Error: $it", color = ToolsRed, modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF1F0), RoundedCornerShape(12.dp)).padding(12.dp)) }
        Text("HTTP, FTP, and Telnet are unencrypted. Use only on a trusted local network.", color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(action.title) },
            text = { Text(action.description) },
            confirmButton = {
                TextButton(onClick = {
                    pendingAction = null
                    launchIo {
                        withContext(Dispatchers.IO) { Enigma2Client.maintenance(host, webPort.toIntOrNull() ?: 80, username, password, action.state) }
                        status = "${action.title} request sent"
                    }
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null }) { Text("Cancel") } }
        )
    }
}

private object TelnetDiagnostics {
    fun check(host: String, port: Int): String {
        if (host.isBlank() || port !in 1..65535) return "Enter a valid host and port"
        return try {
            Socket().use { it.connect(InetSocketAddress(host.trim(), port), 3000) }
            "Port $port is reachable; no login or commands sent."
        } catch (_: Exception) {
            "Port $port is not reachable."
        }
    }
}

private object Enigma2Client {
    private const val MAX_RESPONSE_BYTES = 5 * 1024 * 1024

    fun snapshot(host: String, port: Int, username: String, password: String): ReceiverSnapshot {
        val about = request(host, port, username, password, "/web/about")
        val current = request(host, port, username, password, "/web/getcurrent")
        val signal = runCatching { request(host, port, username, password, "/web/signal") }.getOrDefault("")
        val model = field(about, "e2model") ?: field(about, "e2machine") ?: field(about, "e2image") ?: "Enigma2 receiver"
        val service = field(current, "e2servicename") ?: field(current, "e2name") ?: "No service information returned"
        val names = listOf("e2snrdb" to "SNR", "e2snr" to "SNR", "e2agc" to "AGC", "e2ber" to "BER", "e2lock" to "Lock")
        val metrics = names.mapNotNull { (tag, label) -> field(signal, tag)?.let { "$label: $it" } }.distinct()
        val signalText = metrics.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "No tuner metrics returned"
        return ReceiverSnapshot(model, service, signalText)
    }

    fun screenshot(host: String, port: Int, username: String, password: String): Bitmap {
        val bytes = requestBytes(host, port, username, password, "/grab?format=jpg")
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("Receiver did not return a readable screenshot")
    }

    fun maintenance(host: String, port: Int, username: String, password: String, state: Int) {
        require(state == 2 || state == 3) { "Unsupported maintenance action" }
        request(host, port, username, password, "/web/powerstate?newstate=$state")
    }

    private fun field(xml: String, tag: String): String? {
        val pattern = Regex("<$tag(?:\\s[^>]*)?>(.*?)</$tag>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return pattern.find(xml)?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), "")?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun request(host: String, port: Int, username: String, password: String, path: String): String =
        String(requestBytes(host, port, username, password, path), Charsets.UTF_8)

    private fun requestBytes(host: String, port: Int, username: String, password: String, path: String): ByteArray {
        require(host.isNotBlank() && port in 1..65535)
        val connection = (URL("http://${host.trim()}:$port$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/xml, image/jpeg, */*")
            if (username.isNotBlank()) {
                val credentials = Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                setRequestProperty("Authorization", "Basic $credentials")
            }
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Receiver returned HTTP $code")
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) throw IOException("Receiver response is too large")
                    output.write(buffer, 0, count)
                }
            }
            return output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }
}

private object FtpBrowser {
    fun list(host: String, port: Int, username: String, password: String, path: String): List<RemoteFile> {
        require(host.isNotBlank() && port in 1..65535)
        require(path.startsWith("/") && !path.split('/').any { it == ".." }) { "Invalid FTP path" }
        val client = FTPClient().apply {
            connectTimeout = 5000
            defaultTimeout = 5000
            dataTimeout = Duration.ofSeconds(7)
        }
        try {
            client.connect(host.trim(), port)
            if (!client.login(username.ifBlank { "anonymous" }, password)) throw IOException("FTP login failed: ${client.replyString}")
            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            return client.listFiles(path).filter { it.name != "." && it.name != ".." }
                .map { RemoteFile(it.name, it.isDirectory, it.size) }
                .sortedWith(compareByDescending<RemoteFile> { it.directory }.thenBy { it.name.lowercase() })
        } finally {
            runCatching { if (client.isConnected) { client.logout(); client.disconnect() } }
        }
    }
}
