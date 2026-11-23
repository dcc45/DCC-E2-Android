package com.dcc.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val HomeNavy = Color(0xFF102A43)
private val HomeBlue = Color(0xFF246BFD)
private val HomeMuted = Color(0xFF68788B)
private val HomeBorder = Color(0xFFE5EBF2)

@Composable
fun HomeScreen(
    receiverIp: String,
    onOpenSettings: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenStream: () -> Unit,
    onOpenTools: () -> Unit
) {
    val configured = receiverIp.isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Welcome to DCC", style = MaterialTheme.typography.headlineSmall, color = HomeNavy, fontWeight = FontWeight.Bold)
            Text("Your Dreambox control center", color = HomeMuted)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = HomeNavy)
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Devices, contentDescription = null, tint = Color.White) }
                    Column {
                        Text("RECEIVER STATUS", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(if (configured) "Receiver configured" else "Set up your receiver", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    if (configured) "Dreambox  ·  $receiverIp" else "Add your Dreambox IP address and login in Settings to get started.",
                    color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.bodyMedium
                )
                if (!configured) {
                    Button(onClick = onOpenSettings, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Configure receiver")
                    }
                } else {
                    Text("Use Receiver tools to check status and manage your DM900.", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("Quick access", style = MaterialTheme.typography.titleMedium, color = HomeNavy, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionCard("Devices", "Receiver details", { Icon(Icons.Default.Devices, contentDescription = null, tint = HomeBlue) }, Modifier.weight(1f), onOpenDevices)
            QuickActionCard("Live TV", "Watch a stream", { Icon(Icons.Default.LiveTv, contentDescription = null, tint = HomeBlue) }, Modifier.weight(1f), onOpenStream)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionCard("Tools", "Receiver utilities", { Icon(Icons.Default.Terminal, contentDescription = null, tint = HomeBlue) }, Modifier.weight(1f), onOpenTools)
            QuickActionCard("Settings", "Connection details", { Icon(Icons.Default.Settings, contentDescription = null, tint = HomeBlue) }, Modifier.weight(1f), onOpenSettings)
        }
        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, HomeBorder)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Getting started", color = HomeNavy, fontWeight = FontWeight.Bold)
                Text(
                    if (configured) "Receiver details are saved on this device. Open Tools to check signal, browse files, capture a screenshot, or perform maintenance."
                    else "Configure your receiver to get started. Then use Live TV to enter a stream URL or explore the other tabs.",
                    color = HomeMuted, style = MaterialTheme.typography.bodyMedium
                )
                if (configured) OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 4.dp)) { Text("Edit receiver settings") }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, HomeBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEAF1FF)), contentAlignment = Alignment.Center
            ) { icon() }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = HomeNavy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(description, color = HomeMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
