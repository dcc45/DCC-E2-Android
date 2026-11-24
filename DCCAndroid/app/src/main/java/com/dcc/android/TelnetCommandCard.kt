package com.dcc.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket

private val TelnetNavy = Color(0xFF102A43)
private val TelnetMuted = Color(0xFF68788B)
private val TelnetBorder = Color(0xFFE5EBF2)

@Composable
fun TelnetCommandCard(host: String, username: String, password: String) {
    var port by remember { mutableStateOf("23") }
    var command by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("Not connected") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, TelnetBorder)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Telnet command console", color = TelnetNavy, fontWeight = FontWeight.Bold)
            Text(
                "Connects using the receiver login details, sends one command, and displays the result.",
                color = TelnetMuted
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Telnet port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Shell command") },
                placeholder = { Text("uptime") },
                singleLine = true
            )
            OutlinedButton(
                enabled = !busy && host.isNotBlank() && command.isNotBlank(),
                onClick = {
                    busy = true
                    output = "Connecting…"
                    scope.launch {
                        output = withContext(Dispatchers.IO) {
                            TelnetCommander.execute(
                                host = host,
                                port = port.toIntOrNull() ?: 23,
                                username = username,
                                password = password,
                                command = command.trim()
                            )
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Running…" else "Send command") }
            Text(
                output,
                color = TelnetMuted,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            )
        }
    }
}

private object TelnetCommander {
    private const val IAC: Int = 255
    private const val DONT: Int = 254
    private const val DO: Int = 253
    private const val WONT: Int = 252
    private const val WILL: Int = 251
    private const val SB: Int = 250
    private const val SE: Int = 240

    fun execute(host: String, port: Int, username: String, password: String, command: String): String {
        if (host.isBlank() || port !in 1..65535) return "Enter a valid host and port"
        if (command.isBlank()) return "Enter a command"

        return try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.soTimeout = 4000
                socket.connect(InetSocketAddress(host.trim(), port), 5000)

                val input = socket.getInputStream().buffered()
                val output = BufferedOutputStream(socket.getOutputStream())

                val sessionText = StringBuilder()
                sessionText.append(readAvailable(input, 1500))

                if (username.isNotBlank()) {
                    val loginPrompt = sessionText.toString().lowercase()
                    if (Regex("login|username").containsMatchIn(loginPrompt)) {
                        output.write((username + "\r\n").toByteArray(Charsets.UTF_8)); output.flush()
                        sessionText.append(readAvailable(input, 1000))
                        val passwordPrompt = sessionText.toString().lowercase()
                        if (Regex("password").containsMatchIn(passwordPrompt)) {
                            output.write((password + "\r\n").toByteArray(Charsets.UTF_8)); output.flush()
                        }
                    }
                }

                output.write((command + "\r\n").toByteArray(Charsets.UTF_8)); output.flush()
                val commandOutput = readAvailable(input, 3500)

                cleanTelnet("$sessionText$commandOutput").trim().ifBlank {
                    "Command sent, but no text response was received."
                }
            }
        } catch (e: Exception) {
            "Telnet failed: ${e.message ?: "Unable to connect or execute command"}"
        }
    }

    private fun readAvailable(input: java.io.InputStream, waitMs: Long): String {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        val end = System.currentTimeMillis() + waitMs

        while (System.currentTimeMillis() < end) {
            try {
                if (input.available() > 0) {
                    val count = input.read(buffer)
                    if (count > 0) {
                        bytes.write(buffer, 0, count)
                        continue
                    }
                }
            } catch (_: java.net.SocketTimeoutException) {
                break
            }
            Thread.sleep(30)
        }

        return bytes.toByteArray().toString(Charsets.UTF_8)
    }

    private fun cleanTelnet(value: String): String {
        val bytes = value.toByteArray(Charsets.ISO_8859_1)
        val cleaned = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xff
            if (b == IAC && i + 1 < bytes.size) {
                val command = bytes[i + 1].toInt() and 0xff
                if (command == SB) {
                    i += 2
                    while (i + 1 < bytes.size) {
                        val first = bytes[i].toInt() and 0xff
                        val second = bytes[i + 1].toInt() and 0xff
                        i += 2
                        if (first == IAC && second == SE) break
                    }
                } else if (command in listOf(WILL, WONT, DO, DONT)) {
                    i += 3
                } else {
                    i += 2
                }
            } else {
                cleaned.append(b.toChar())
                i += 1
            }
        }
        return Regex("\\u001B\\[[0-?]*[ -/]*[@-~]").replace(cleaned.toString(), "")
            .replace("\u0000", "")
            .replace("\r", "")
    }
}
