package com.dcc.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF102A43)
private val Blue = Color(0xFF246BFD)
private val Green = Color(0xFF168A5B)
private val Canvas = Color(0xFFF4F7FB)
private val Muted = Color(0xFF68788B)
private val Border = Color(0xFFE5EBF2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DccApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DccApp() {
    var selected by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Ready to connect") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoReconnectEnabled by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Blue, secondary = Green, background = Canvas, surface = Color.White)
    ) {
        Scaffold(
            containerColor = Canvas,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_dcc_logo),
                                contentDescription = "DCC logo",
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("DCC", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                                Text("DREAMBOX CONTROL CENTER", fontSize = 9.sp, color = Muted, letterSpacing = 1.sp)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    NavigationBarItem(selected = selected == 0, onClick = { selected = 0 }, icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Home") })
                    NavigationBarItem(selected = selected == 1, onClick = { selected = 1 }, icon = { Icon(Icons.Default.Devices, contentDescription = "Devices") }, label = { Text("Devices") })
                    NavigationBarItem(selected = selected == 2, onClick = { selected = 2 }, icon = { Icon(Icons.Default.Terminal, contentDescription = "Tools") }, label = { Text("Tools") })
                    NavigationBarItem(selected = selected == 3, onClick = { selected = 3 }, icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                when (selected) {
                    0 -> HomeScreen(
                        message = message,
                        onReconnect = { message = "Reconnect requested. Connection protocol is not configured yet." },
                        onFindDevice = { message = "Network discovery is not implemented yet." },
                        onOpenTools = { selected = 2 }
                    )
                    1 -> DevicesScreen()
                    2 -> ToolsScreen(onAction = { message = "$it selected. This operation is not implemented yet."; selected = 0 })
                    3 -> SettingsScreen(
                        notificationsEnabled = notificationsEnabled,
                        onNotificationsChange = { notificationsEnabled = it },
                        autoReconnectEnabled = autoReconnectEnabled,
                        onAutoReconnectChange = { autoReconnectEnabled = it }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HomeScreen(message: String, onReconnect: () -> Unit, onFindDevice: () -> Unit, onOpenTools: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your network", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
            Text("Your receiver setup at a glance", color = Muted)
        }
        ConnectionHero(onReconnect)
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeading("DEVICES", "3 saved")
        }
        DeviceCard("This phone", "192.168.1.12", "Controller", "📱", true)
        DeviceCard("Dreambox", "192.168.1.4", "Receiver", "📺", false)
        DeviceCard("Router", "192.168.1.254", "Gateway", "📡", false)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onReconnect, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text("Reconnect")
            }
            OutlinedButton(onClick = onFindDevice, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text("Find device")
            }
        }
        Text(message, color = Muted, style = MaterialTheme.typography.bodySmall)
        SectionHeading("SERVICES", "Connection status")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ServiceCard("FTP", "Not connected", Modifier.weight(1f))
            ServiceCard("Telnet", "Not connected", Modifier.weight(1f))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFEAF0FF)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Blue)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Quick tools", fontWeight = FontWeight.Bold, color = Navy)
                    Text("Connection info and receiver utilities", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onOpenTools) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Open tools", tint = Blue)
                }
            }
        }
    }
}

@Composable
private fun ConnectionHero(onReconnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF246BFD), Color(0xFF1746B8)))).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Receiver connection", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                    Text("Not connected", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFD166)))
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dreambox · 192.168.1.4", color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onReconnect, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text("Connect")
                }
            }
        }
    }
}

@Composable
private fun DevicesScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Devices", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
            Text("Saved devices on your network", color = Muted)
        }
        DeviceCard("This phone", "192.168.1.12", "Controller", "📱", true)
        DeviceCard("Dreambox", "192.168.1.4", "Receiver", "📺", false)
        DeviceCard("Router", "192.168.1.254", "Gateway", "📡", false)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF0FF)), shape = RoundedCornerShape(18.dp)) {
            Text(
                "These are sample device entries. Network discovery and live connection checks are not configured yet.",
                modifier = Modifier.padding(16.dp), color = Navy, style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ToolsScreen(onAction: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
            Text("Receiver utilities", color = Muted)
        }
        ToolActionCard("Connection details", "View receiver connection information", onClick = { onAction("Connection details") })
        ToolActionCard("FTP browser", "Browse files on your Dreambox", onClick = { onAction("FTP browser") })
        ToolActionCard("Telnet console", "Open a receiver terminal session", onClick = { onAction("Telnet console") })
        ToolActionCard("Run script", "Run a receiver maintenance script", onClick = { onAction("Run script") })
    }
}

@Composable
private fun SettingsScreen(
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    autoReconnectEnabled: Boolean,
    onAutoReconnectChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
            Text("Personalize your DCC experience", color = Muted)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column {
                SettingSwitch("Notifications", "Show connection status notifications", notificationsEnabled, onNotificationsChange)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Canvas)
                SettingSwitch("Auto reconnect", "Reconnect when the receiver is available", autoReconnectEnabled, onAutoReconnectChange)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About DCC", fontWeight = FontWeight.Bold, color = Navy)
                Text("Dreambox Control Center · Version 2.0", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, detail: String) {
    Text(title, color = Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
    Text(detail, color = Muted, style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Navy)
            Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ToolActionCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFEAF0FF)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = Blue, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = Navy)
                Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DeviceCard(name: String, address: String, type: String, emoji: String, primary: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(if (primary) Color(0xFFEAF0FF) else Color(0xFFEAF7F1)), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 23.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, fontWeight = FontWeight.Bold, color = Navy)
                Text("$type  ·  $address", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Surface(color = if (primary) Color(0xFFEAF0FF) else Color(0xFFF0F3F7), shape = RoundedCornerShape(50)) {
                Text(if (primary) "LOCAL" else "SAVED", modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = if (primary) Blue else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
            }
        }
    }
}

@Composable
private fun ServiceCard(title: String, detail: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (title == "FTP") Icons.Default.Tv else Icons.Default.Router, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f))
                Box(Modifier.size(8.dp).clip(CircleShape).background(Muted.copy(alpha = 0.55f)))
            }
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
