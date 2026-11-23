package com.dcc.android

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

private val Navy = Color(0xFF102A43)
private val Blue = Color(0xFF246BFD)
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
    var streamUrl by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("receiver_settings", Context.MODE_PRIVATE) }
    var receiverIp by remember { mutableStateOf(preferences.getString("receiver_ip", "") ?: "") }
    var receiverUsername by remember { mutableStateOf(preferences.getString("receiver_username", "") ?: "") }
    var settingsSaved by remember { mutableStateOf(false) }
    val player = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(playbackError: androidx.media3.common.PlaybackException) {
                error = "Unable to play this stream. Check the URL and receiver connection."
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    BackHandler(enabled = fullscreen) { fullscreen = false }
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Canvas, surface = Color.White)) {
        if (fullscreen) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (streamUrl.isNotBlank()) PlayerSurface(player, Modifier.fillMaxSize(), resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
                IconButton(
                    onClick = { fullscreen = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clip(CircleShape).background(Color.Black.copy(alpha = .55f))
                ) { Text("×", color = Color.White, fontSize = 28.sp) }
                Text(
                    "${if (isPlaying) "LIVE" else "STREAM"}  ·  Dreambox",
                    color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
                )
                TextButton(onClick = { fullscreen = false; selected = 0 }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    Text("MINIMIZE", color = Color.White)
                }
            }
        } else {
            Scaffold(
                containerColor = Canvas,
                topBar = {
                    TopAppBar(
                        title = { Text(if (selected == 2) "Live Stream" else "DCC · Dreambox Control Center", color = Navy, fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(selected == 0, { selected = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                        NavigationBarItem(selected == 1, { selected = 1 }, icon = { Icon(Icons.Default.Devices, null) }, label = { Text("Devices") })
                        NavigationBarItem(selected == 2, { selected = 2 }, icon = { Icon(Icons.Default.LiveTv, null) }, label = { Text("Stream") })
                        NavigationBarItem(selected == 3, { selected = 3 }, icon = { Icon(Icons.Default.Terminal, null) }, label = { Text("Tools") })
                        NavigationBarItem(selected == 4, { selected = 4 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if (streamUrl.isNotBlank() && selected != 2) {
                        MiniPlayer(
                            player = player,
                            playing = isPlaying,
                            onExpand = { selected = 2 },
                            onClose = { player.stop(); streamUrl = ""; isPlaying = false }
                        )
                    }
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (selected) {
                            0 -> SimpleScreen(
                                "Your network",
                                "Connect to your Dreambox to browse and control your receiver.",
                                if (receiverIp.isBlank()) "Receiver IP not set · Not connected" else "Dreambox · $receiverIp · Not connected"
                            )
                            1 -> SimpleScreen(
                                "Devices",
                                "Saved devices on your network",
                                if (receiverIp.isBlank()) "Add your receiver IP in Settings" else "Dreambox · $receiverIp"
                            )
                            2 -> StreamScreen(
                                player = player, streamUrl = streamUrl, urlInput = urlInput,
                                onUrlChange = { urlInput = it }, playing = isPlaying,
                                error = error, onErrorDismiss = { error = null },
                                onPlay = {
                                    val candidate = urlInput.trim()
                                    val uri = runCatching { Uri.parse(candidate) }.getOrNull()
                                    if (uri == null || uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
                                        error = "Enter a valid HTTP or HTTPS stream URL from your Dreambox."
                                    } else {
                                        error = null
                                        streamUrl = candidate
                                        player.setMediaItem(MediaItem.fromUri(uri))
                                        player.prepare()
                                        player.play()
                                    }
                                },
                                onToggle = { if (isPlaying) player.pause() else if (streamUrl.isNotBlank()) player.play() else error = "Enter a stream URL to start playback." },
                                onFullscreen = { fullscreen = true },
                                onMinimize = { selected = 0 }
                            )
                            3 -> SimpleScreen("Tools", "Receiver utilities", "FTP browser · Telnet console · Connection details")
                            else -> ReceiverSettingsScreen(
                                receiverIp = receiverIp,
                                onReceiverIpChange = { receiverIp = it; settingsSaved = false },
                                receiverUsername = receiverUsername,
                                onReceiverUsernameChange = { receiverUsername = it; settingsSaved = false },
                                settingsSaved = settingsSaved,
                                onSave = {
                                    preferences.edit()
                                        .putString("receiver_ip", receiverIp.trim())
                                        .putString("receiver_username", receiverUsername.trim())
                                        .apply()
                                    receiverIp = receiverIp.trim()
                                    receiverUsername = receiverUsername.trim()
                                    settingsSaved = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiverSettingsScreen(
    receiverIp: String,
    onReceiverIpChange: (String) -> Unit,
    receiverUsername: String,
    onReceiverUsernameChange: (String) -> Unit,
    settingsSaved: Boolean,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Receiver connection", style = MaterialTheme.typography.headlineSmall, color = Navy, fontWeight = FontWeight.Bold)
            Text("Enter the network address and login name for your Dreambox.", color = Muted)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Border)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = receiverIp,
                    onValueChange = onReceiverIpChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Receiver IP address") },
                    placeholder = { Text("192.168.1.4") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = receiverUsername,
                    onValueChange = onReceiverUsernameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Username") },
                    placeholder = { Text("root") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(14.dp)
                )
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("Save receiver settings")
                }
                if (settingsSaved) Text("Receiver settings saved on this device.", color = Blue, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("These details are saved locally. Receiver connection and control features are not implemented yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StreamScreen(
    player: ExoPlayer,
    streamUrl: String,
    urlInput: String,
    onUrlChange: (String) -> Unit,
    playing: Boolean,
    error: String?,
    onErrorDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggle: () -> Unit,
    onFullscreen: () -> Unit,
    onMinimize: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Watch live TV", style = MaterialTheme.typography.headlineSmall, color = Navy, fontWeight = FontWeight.Bold)
            Text("Play a Dreambox stream inside DCC", color = Muted)
        }
        if (streamUrl.isNotBlank()) {
            Box {
                PlayerSurface(player, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)))
                TextButton(onClick = onFullscreen, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
                    Text("FULL SCREEN", color = Color.White)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onToggle, modifier = Modifier.weight(1f)) { Text(if (playing) "Pause" else "Play") }
                OutlinedButton(onClick = onMinimize) { Text("Mini player") }
                OutlinedButton(onClick = onFullscreen) { Text("Expand") }
            }
            Text(if (playing) "LIVE  ·  Streaming from your receiver" else "Stream ready", color = if (playing) Blue else Muted, style = MaterialTheme.typography.bodySmall)
            Text(streamUrl, color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)).background(Color(0xFF101820)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
                    Text("Your live stream will appear here", color = Color.White)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Border)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dreambox stream URL", color = Navy, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("http://receiver:8001/...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(14.dp)
                )
                Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(if (streamUrl.isBlank()) "Connect and play" else "Play stream")
                }
                Text("Enter the service stream URL provided by your receiver. HLS and common receiver stream formats are supported.", color = Muted, style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun PlayerSurface(player: ExoPlayer, modifier: Modifier, resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = {
            PlayerView(context).apply {
                this.player = player
                useController = false
                this.resizeMode = resizeMode
                setShutterBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { it.player = player; it.resizeMode = resizeMode }
    )
}

@Composable
private fun MiniPlayer(player: ExoPlayer, playing: Boolean, onExpand: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF102A43)).clickable(onClick = onExpand).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerSurface(player, Modifier.width(112.dp).height(64.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (playing) "LIVE · Dreambox" else "Stream paused", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Tap to return to player", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onClose) { Text("×", color = Color.White, fontSize = 22.sp) }
    }
}

@Composable
private fun SimpleScreen(title: String, subtitle: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Navy, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Border)) {
            Text(detail, Modifier.fillMaxWidth().padding(18.dp), color = Navy)
        }
    }
}
