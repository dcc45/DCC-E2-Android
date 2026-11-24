package com.dcc.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer

private val ListenNavy = Color(0xFF102A43)
private val ListenBlue = Color(0xFF246BFD)
private val ListenMuted = Color(0xFF68788B)
private val ListenBorder = Color(0xFFE5EBF2)

@Composable
fun ListenScreen(
    player: ExoPlayer,
    listenUrl: String,
    urlInput: String,
    playing: Boolean,
    error: String?,
    onUrlChange: (String) -> Unit,
    onPlay: () -> Unit,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    var protocol by remember { mutableStateOf(if (urlInput.startsWith("https://", true)) "HTTPS" else "HTTP") }
    var receiverIp by remember { mutableStateOf("") }
    var streamPort by remember { mutableStateOf("8001") }
    var streamPath by remember { mutableStateOf("/audio") }

    fun updateUrl(ip: String = receiverIp, port: String = streamPort, path: String = streamPath, scheme: String = protocol) {
        receiverIp = ip
        streamPort = port
        streamPath = path
        protocol = scheme
        val cleanPath = path.trim().let { if (it.isBlank()) "/audio" else if (it.startsWith("/")) it else "/$it" }
        val parsedPort = port.toIntOrNull()
        onUrlChange(if (ip.isBlank() || parsedPort !in 1..65535) "" else "${scheme.lowercase()}://${ip.trim()}:$parsedPort$cleanPath")
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Listen mode", style = MaterialTheme.typography.headlineSmall, color = ListenNavy, fontWeight = FontWeight.Bold)
            Text("Play Dreambox radio or audio streams without video.", color = ListenMuted)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ListenNavy)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Box(
                    modifier = Modifier.size(84.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Headphones, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
                Text(
                    if (playing) "Listening live" else if (listenUrl.isNotBlank()) "Audio ready" else "No audio playing",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (listenUrl.isBlank()) "Enter receiver IP, port, and stream path" else listenUrl,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onToggle, modifier = Modifier.weight(1f), enabled = listenUrl.isNotBlank()) {
                Text(if (playing) "Pause" else "Play")
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f), enabled = listenUrl.isNotBlank()) {
                Text("Stop")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ListenBorder)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Audio stream address", color = ListenNavy, fontWeight = FontWeight.Bold)
                Text("Protocol", color = ListenMuted, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = protocol == "HTTP",
                        onClick = { updateUrl(scheme = "HTTP") },
                        label = { Text("HTTP") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = protocol == "HTTPS",
                        onClick = { updateUrl(scheme = "HTTPS") },
                        label = { Text("HTTPS") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = receiverIp,
                    onValueChange = { updateUrl(ip = it.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Receiver IP or hostname") },
                    placeholder = { Text("192.168.1.104") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = streamPort,
                    onValueChange = { updateUrl(port = it.filter(Char::isDigit).take(5)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Audio stream port") },
                    placeholder = { Text("8001") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = streamPath,
                    onValueChange = { updateUrl(path = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Audio stream path") },
                    placeholder = { Text("/audio") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    if (urlInput.isNotBlank()) "Stream URL: $urlInput" else "Enter the receiver address, port, and audio path.",
                    color = ListenMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), enabled = urlInput.isNotBlank()) {
                    Text(if (listenUrl.isBlank()) "Start listening" else "Listen to this stream")
                }
                Text("Typical Enigma2 audio streaming port: 8001.", color = ListenMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(it, color = Color(0xFF9C2F26), modifier = Modifier.weight(1f))
                    TextButton(onClick = onErrorDismiss) { Text("OK") }
                }
            }
        }
    }
}
