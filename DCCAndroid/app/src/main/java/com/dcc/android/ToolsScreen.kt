package com.dcc.android

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private val ProtocolBlue = Color(0xFF246BFD)
private val ProtocolNavy = Color(0xFF102A43)

@Composable
fun ToolsScreen(language: AppLanguage) {
    val context = LocalContext.current
    var protocol by remember { mutableStateOf("menu") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text(language.tools), style = MaterialTheme.typography.headlineSmall, color = ProtocolNavy, fontWeight = FontWeight.Bold)
            Text(text(language.toolsSubtitle), color = Color(0xFF68788B))
        }

        if (protocol == "menu") {
            ProtocolCard(Icons.Default.Download, text(language.ftpProtocol), text(language.ftpSubtitle), text(language.open)) { protocol = "ftp" }
            ProtocolCard(Icons.Default.Terminal, text(language.telnetProtocol), text(language.telnetSubtitle), text(language.open)) { protocol = "telnet" }
            ProtocolCard(Icons.Default.Storage, text(language.rescueMode), text(language.rescueSubtitle), text(language.open)) { protocol = "rescue" }
        } else {
            OutlinedButton(onClick = { protocol = "menu" }) { Text("← ${text(language.tools)}") }
            when (protocol) {
                "ftp" -> FtpScreen(language)
                "telnet" -> TelnetScreen(language)
                "rescue" -> RescueScreen(language) { uri ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                }
            }
        }
    }
}

@Composable
private fun ProtocolCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, action: String, onOpen: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE5EBF2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(46.dp), shape = RoundedCornerShape(14.dp), color = ProtocolBlue.copy(alpha = .10f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = ProtocolBlue)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ProtocolNavy, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFF68788B), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onOpen) { Text(action) }
        }
    }
}

@Composable
private fun FtpScreen(language: AppLanguage) {
    var host by remember { mutableStateOf("192.168.1.4") }
    var username by remember { mutableStateOf("root") }
    var password by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFE5EBF2))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text(language.ftpProtocol), color = ProtocolNavy, fontWeight = FontWeight.Bold)
            OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), label = { Text(text(language.host)) }, singleLine = true)
            OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text(text(language.username)) }, singleLine = true)
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(text(language.password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            Button(onClick = { connected = host.isNotBlank() && username.isNotBlank() }, Modifier.fillMaxWidth()) {
                Text(if (connected) text(language.connected) else text(language.connect))
            }
            if (connected) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7FB)), border = BorderStroke(1.dp, Color(0xFFE5EBF2))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FtpRow("/", true)
                        FtpRow("media", true)
                        FtpRow("etc", true)
                        FtpRow("tmp", true)
                        FtpRow("bootlogo.mvi", false)
                    }
                }
                Text(text(language.ftpDemo), color = Color(0xFF68788B), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FtpRow(name: String, folder: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (folder) Icons.Default.Folder else Icons.Default.Download, null, tint = if (folder) ProtocolBlue else Color(0xFF68788B), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(name, color = ProtocolNavy)
    }
}

@Composable
private fun TelnetScreen(language: AppLanguage) {
    var host by remember { mutableStateOf("192.168.1.4") }
    var port by remember { mutableStateOf("23") }
    var log by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFE5EBF2))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text(language.telnetProtocol), color = ProtocolNavy, fontWeight = FontWeight.Bold)
            OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), label = { Text(text(language.host)) }, singleLine = true)
            OutlinedTextField(port, { port = it }, Modifier.fillMaxWidth(), label = { Text(text(language.port)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                Text(if (log.isBlank()) "Dreambox Telnet Console\n" else log, Modifier.fillMaxWidth().heightIn(min = 150.dp).padding(12.dp), color = Color(0xFFBFFFC2), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(command, { command = it }, Modifier.weight(1f), label = { Text(text(language.command)) }, singleLine = true)
                Button(onClick = {
                    if (command.isNotBlank()) {
                        log += "\nroot@$host:~# $command\n"
                        log += "${text(language.commandQueued)}\n"
                        command = ""
                    }
                }) { Text(text(language.send)) }
            }
            Text(text(language.telnetSecurity), color = Color(0xFF68788B), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RescueScreen(language: AppLanguage, openUri: (String) -> Unit) {
    var host by remember { mutableStateOf("192.168.1.4") }
    var confirmed by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7F5)), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFFFCFC7))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text(language.rescueMode), color = Color(0xFF9C2F26), fontWeight = FontWeight.Bold)
            Text(text(language.rescueWarning), color = Color(0xFF6D423D))
            OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), label = { Text(text(language.rescueAddress)) }, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(confirmed, { confirmed = it })
                Text(text(language.rescueConfirm), color = ProtocolNavy)
            }
            Button(onClick = { openUri("http://$host") }, enabled = confirmed, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC83C32)), modifier = Modifier.fillMaxWidth()) {
                Text(text(language.openRescueLoader))
            }
        }
    }
}

