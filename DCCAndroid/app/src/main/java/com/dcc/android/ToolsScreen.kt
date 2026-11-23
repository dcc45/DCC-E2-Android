package com.dcc.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

private val ToolsNavy = Color(0xFF102A43)
private val ToolsBlue = Color(0xFF246BFD)
private val ToolsMuted = Color(0xFF68788B)
private val ToolsBorder = Color(0xFFE5EBF2)
private val ConsoleBackground = Color(0xFF101820)

private data class DiagnosticCommand(val title: String, val command: String)

private val diagnosticCommands = listOf(
    DiagnosticCommand("Uptime", "uptime"),
    DiagnosticCommand("Disk space", "df -h"),
    DiagnosticCommand("Memory", "free -m"),
    DiagnosticCommand("System load", "cat /proc/loadavg")
)

/** Receiver utilities. Telnet is intentionally limited to read-only diagnostics. */
@Composable
fun ToolsScreen(
    language: AppLanguage,
    receiverIp: String,
    username: String,
    password: String
) {
    var host by remember(receiverIp) { mutableStateOf(receiverIp) }
    var port by remember { mutableStateOf("23") }
    var output by remember { mutableStateOf("Telnet diagnostics are limited to read-only system information.") }
    var status by remember { mutableStateOf("Not connected") }
    var busy by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<TelnetSession?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(session) {
        onDispose { session?.close() }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Receiver tools", style = MaterialTheme.typography.headlineSmall, color = ToolsNavy, fontWeight = FontWeight.Bold)
            Text("Diagnostics and receiver maintenance", color = ToolsMuted)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ToolsBorder)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Terminal, contentDescription = null, tint = ToolsBlue)
                    Text("Telnet diagnostics", color = ToolsNavy, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(status, color = if (session != null) ToolsBlue else ToolsMuted, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Receiver IP or hostname") },
                    placeholder = { Text("192.168.1.4") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit).take(5) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Telnet port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (session == null) {
                        Button(
                            onClick = {
                                val targetHost = host.trim()
                                val targetPort = port.toIntOrNull()
                                if (targetHost.isBlank() || targetPort !in 1..65535) {
                                    status = "Enter a valid receiver and port"
                                } else {
                                    busy = true
                                    status = "Connecting…"
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching { TelnetSession.connect(targetHost, targetPort!!, username, password) }
                                        }
                                        busy = false
                                        result.onSuccess {
                                            session = it
                                            status = "Connected to $targetHost"
                                            output = "Connected. Select a read-only diagnostic below."
                                        }.onFailure {
                                            status = "Connection failed"
                                            output = "Could not connect or authenticate: ${it.message ?: "check receiver address, credentials, and Telnet service"}"
                                        }
                                    }
                                }
                            },
                            enabled = !busy,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Connect")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                session?.close()
                                session = null
                                status = "Disconnected"
                            },
                            enabled = !busy,
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Disconnect") }
                    }
                    Text("Uses the username and password saved in Settings.", color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ToolsBorder)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Read-only checks", color = ToolsNavy, fontWeight = FontWeight.Bold)
                Text("These shortcuts only read basic system status; they do not change receiver settings or files.", color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
                diagnosticCommands.forEach { diagnostic ->
                    OutlinedButton(
                        onClick = {
                            val activeSession = session ?: return@OutlinedButton
                            busy = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { runCatching { activeSession.runDiagnostic(diagnostic.command) } }
                                busy = false
                                result.onSuccess { output = "$ ${diagnostic.command}\n$it" }
                                    .onFailure {
                                        output = "Diagnostic failed: ${it.message ?: "connection lost"}"
                                        status = "Disconnected"
                                        activeSession.close()
                                        session = null
                                    }
                            }
                        },
                        enabled = session != null && !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(diagnostic.title) }
                }
                HorizontalDivider(color = ToolsBorder)
                Text("Console output", color = ToolsNavy, fontWeight = FontWeight.SemiBold)
                Text(
                    output,
                    modifier = Modifier.fillMaxWidth().background(ConsoleBackground, RoundedCornerShape(12.dp)).padding(14.dp),
                    color = Color(0xFFE6EDF3),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text("More receiver tools", style = MaterialTheme.typography.titleMedium, color = ToolsNavy, fontWeight = FontWeight.Bold)
        ToolInfoCard(
            icon = { androidx.compose.material3.Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, tint = ToolsBlue) },
            title = "Signal meter",
            detail = "Signal strength and quality need a receiver-model-specific API. No readings are shown until that integration is available."
        )
        ToolInfoCard(
            icon = { androidx.compose.material3.Icon(Icons.Default.Build, contentDescription = null, tint = ToolsBlue) },
            title = "Rescue, recovery & maintenance",
            detail = "Recovery and rescue procedures vary by receiver model and firmware. This build does not trigger reboot, flash, reset, or recovery operations."
        )
        ToolInfoCard(
            icon = { androidx.compose.material3.Icon(Icons.Default.Refresh, contentDescription = null, tint = ToolsBlue) },
            title = "Screenshot",
            detail = "Receiver screenshots require a compatible Web Interface endpoint. Configure the receiver's model and Web Interface before capture can be enabled."
        )
        Text("Telnet sends credentials over an unencrypted connection. Use it only on a trusted local network and only with a receiver you own or administer.", color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ToolInfoCard(icon: @Composable () -> Unit, title: String, detail: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ToolsBorder)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = ToolsNavy, fontWeight = FontWeight.Bold)
                Text(detail, color = ToolsMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** A bounded Telnet session which accepts only the diagnostic commands declared above. */
private class TelnetSession private constructor(private val socket: Socket) : Closeable {
    private val input: InputStream = socket.getInputStream()
    private val output = socket.getOutputStream()

    @Synchronized
    fun runDiagnostic(command: String): String {
        require(diagnosticCommands.any { it.command == command }) { "Command is not allowed" }
        socket.soTimeout = 4000
        output.write((command + "\n").toByteArray(Charsets.UTF_8))
        output.flush()
        return readUntilPrompt(6000).trim().ifBlank { "No output received." }
    }

    private fun readUntilPrompt(timeoutMs: Long): String {
        val started = android.os.SystemClock.elapsedRealtime()
        val bytes = StringBuilder()
        while (android.os.SystemClock.elapsedRealtime() - started < timeoutMs) {
            try {
                val value = input.read()
                if (value < 0) throw IllegalStateException("Receiver closed the Telnet connection")
                if (value == IAC) {
                    handleTelnetControl()
                    continue
                }
                if (value == '\r'.code || value == 0) continue
                bytes.append(value.toChar())
                val text = bytes.toString()
                if (isPrompt(text)) return text
            } catch (_: java.net.SocketTimeoutException) {
                if (bytes.isNotEmpty()) return bytes.toString()
            }
        }
        if (bytes.isNotEmpty()) return bytes.toString()
        throw IllegalStateException("Timed out waiting for the receiver")
    }

    private fun handleTelnetControl() {
        val command = input.read()
        if (command < 0) return
        when (command) {
            WILL, WONT, DO, DONT -> {
                val option = input.read()
                if (option >= 0) {
                    val response = when (command) {
                        WILL -> DONT
                        DO -> WONT
                        else -> return
                    }
                    output.write(byteArrayOf(IAC.toByte(), response.toByte(), option.toByte()))
                    output.flush()
                }
            }
            SB -> {
                var previous = -1
                while (true) {
                    val next = input.read()
                    if (next < 0 || (previous == IAC && next == SE)) break
                    previous = next
                }
            }
        }
    }

    override fun close() {
        runCatching { socket.close() }
    }

    companion object {
        private const val IAC = 255
        private const val WILL = 251
        private const val WONT = 252
        private const val DO = 253
        private const val DONT = 254
        private const val SB = 250
        private const val SE = 240

        fun connect(host: String, port: Int, username: String, password: String): TelnetSession {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(host, port), 5000)
                socket.soTimeout = 1000
                val session = TelnetSession(socket)
                var prompt = session.readUntilPrompt(8000)
                if (containsAny(prompt, "login:", "username:")) {
                    session.writeLine(username)
                    prompt = session.readUntilPrompt(8000)
                }
                if (containsAny(prompt, "password:")) {
                    session.writeLine(password)
                    prompt = session.readUntilPrompt(8000)
                }
                if (!isPrompt(prompt)) throw IllegalStateException("Login prompt was not recognized; check Telnet credentials and receiver settings")
                return session
            } catch (error: Exception) {
                runCatching { socket.close() }
                throw error
            }
        }

        private fun containsAny(text: String, vararg values: String): Boolean {
            val normalized = text.lowercase()
            return values.any(normalized::contains)
        }

        private fun isPrompt(text: String): Boolean {
            val lastLine = text.trimEnd().substringAfterLast('\n').trim()
            return lastLine.endsWith("#") || lastLine.endsWith("$") || lastLine.endsWith(">")
        }
    }

    private fun writeLine(value: String) {
        output.write((value + "\n").toByteArray(Charsets.UTF_8))
        output.flush()
    }
}
