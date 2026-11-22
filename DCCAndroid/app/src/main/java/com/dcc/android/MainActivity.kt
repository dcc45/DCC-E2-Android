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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Green, background = Canvas, surface = Color.White)) {
        Scaffold(
            containerColor = Canvas,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.ic_dcc_logo), contentDescription = "DCC logo", modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("DCC", fontWeight = FontWeight.Bold, color = Navy)
                                Text("Dreambox Control Center", fontSize = 12.sp, color = Muted)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = selected == 0, onClick = { selected = 0 }, icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Home") })
                    NavigationBarItem(selected = selected == 1, onClick = { selected = 1 }, icon = { Icon(Icons.Default.Devices, contentDescription = "Devices") }, label = { Text("Devices") })
                    NavigationBarItem(selected = selected == 2, onClick = { selected = 2 }, icon = { Icon(Icons.Default.Terminal, contentDescription = "Tools") }, label = { Text("Tools") })
                    NavigationBarItem(selected = selected == 3, onClick = { selected = 3 }, icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                when (selected) {
                    0 -> HomeScreen(message = message, onReconnect = { message = "Reconnect requested. Connection protocol is not configured yet." }, onFindDevice = { message = "Network discovery is not implemented yet." }, onOpenTools = { selected = 2 })
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your network", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
                Text("Manage your Dreambox connection", color = Muted)
            }
            StatusPill("NETWORK OK")
        }
        DeviceCard("This phone", "192.168.1.12", "Controller", "📱", true)
        DeviceCard("Dreambox", "192.168.1.4", "Receiver", "📺", false)
        DeviceCard("Router", "192.168.1.254", "Gateway", "📡", false)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onReconnect, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Devices, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Reconnect")
            }
            OutlinedButton(onClick = onFindDevice, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("⌕", fontSize = 22.sp); Spacer(Modifier.width(6.dp)); Text("Find device")
            }
        }
        Text(message, color = Muted, style = MaterialTheme.typography.bodySmall)
        Text("SERVICES", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ServiceCard("FTP", "Not connected", false, Modifier.weight(1f))
            ServiceCard("Telnet", "Not connected", false, Modifier.weight(1f))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick tools", fontWeight = FontWeight.Bold, color = Navy, style = MaterialTheme.typography.titleMedium)
                Text("Open scripts, inspect connection details, or configure your receiver.", color = Muted)
                OutlinedButton(onClick = onOpenTools, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Terminal, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Open tools")
                }
            }
        }
    }
}

@Composable
private fun DevicesScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Devices", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
        Text("Devices configured for your network", color = Muted)
        DeviceCard("This phone", "192.168.1.12", "Controller", "📱", true)
        DeviceCard("Dreambox", "192.168.1.4", "Receiver", "📺", false)
        DeviceCard("Router", "192.168.1.254", "Gateway", "📡", false)
        Text("Tap Find device on Home to discover receivers. Network discovery is not implemented yet.", color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ToolsScreen(onAction: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
        Text("Receiver utilities", color = Muted)
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
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
        Text("App preferences", color = Muted)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column {
                SettingSwitch("Notifications", "Show connection status notifications", notificationsEnabled, onNotificationsChange)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Canvas)
                SettingSwitch("Auto reconnect", "Reconnect when the receiver is available", autoReconnectEnabled, onAutoReconnectChange)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About DCC", fontWeight = FontWeight.Bold, color = Navy)
                Text("Dreambox Control Center · Version 2.0", color = Muted)
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Navy)
            Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ToolActionCard(title: String, description: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Navy)
                Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = Blue, fontSize = 28.sp)
        }
    }
}

@Composable
private fun DeviceCard(name: String, address: String, type: String, emoji: String, primary: Boolean) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (primary) Color(0xFFEAF0FF) else Color(0xFFEAF7F1)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 25.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, color = Navy); Text("$type  ·  $address", color = Muted, style = MaterialTheme.typography.bodySmall) }
            Box(Modifier.size(9.dp).clip(CircleShape).background(Green))
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(color = Color(0xFFE6F5ED), shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Green))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ServiceCard(title: String, detail: String, active: Boolean, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f))
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (active) Green else Muted))
            }
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
