package com.dcc.android

import android.net.Uri
import android.os.Bundle
import android.util.Base64
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

private val Navy = Color(0xFF102A43)
private val Blue = Color(0xFF246BFD)
private val Muted = Color(0xFF68788B)
private val Border = Color(0xFFE5EBF2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DccTheme { DccApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DccApp() {
    var selected by remember { mutableIntStateOf(0) }
    var streamUrl by remember { mutableStateOf("") }
    var listenUrl by remember { mutableStateOf("") }
    var listenUrlInput by remember { mutableStateOf("") }
    var listenError by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("dcc_receiver", android.content.Context.MODE_PRIVATE) }
    var receiverIp by remember { mutableStateOf(preferences.getString("ip", "") ?: "") }
    var receiverUser by remember { mutableStateOf(preferences.getString("user", "root") ?: "root") }
    var receiverPassword by remember { mutableStateOf(preferences.getString("password", "") ?: "") }
    var urlInput by remember(receiverIp) {
        mutableStateOf(if (receiverIp.isBlank()) "" else "http://${receiverIp.trim()}:8001/")
    }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    val httpFactory = remember(receiverUser, receiverPassword) {
        val factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
        if (receiverUser.isNotBlank()) {
            val credentials = Base64.encodeToString(
                "$receiverUser:$receiverPassword".toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            factory.setDefaultRequestProperties(mapOf("Authorization" to "Basic $credentials"))
        }
        factory
    }
    val mediaSourceFactory = remember(httpFactory) {
        DefaultMediaSourceFactory(context).setDataSourceFactory(httpFactory)
    }
    val player = remember(mediaSourceFactory) {
        ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build()
    }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(playbackError: androidx.media3.common.PlaybackException) {
                val detail = playbackError.localizedMessage
                    ?: playbackError.cause?.message
                    ?: "Playback error ${playbackError.errorCode}"
                val message = if (selected == 6) {
                    "Unable to play this audio stream: $detail"
                } else {
                    "Unable to play this stream: $detail"
                }
                if (selected == 6) listenError = message else error = message
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

    BackHandler(enabled = fullscreen) { fullscreen = false }
    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (streamUrl.isNotBlank()) PlayerSurface(player, Modifier.fillMaxSize(), resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
            IconButton(onClick = { fullscreen = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clip(CircleShape).background(Color.Black.copy(alpha = .55f))) {
                Text("×", color = Color.White, fontSize = 28.sp)
            }
            Text("${if (isPlaying) "LIVE" else "STREAM"} · Dreambox", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
            TextButton(onClick = { fullscreen = false; selected = 0 }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Text("MINIMIZE", color = Color.White) }
        }
    } else {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val title = when (selected) { 2 -> "Live Stream"; 5 -> "PVR / Recordings"; 6 -> "Listen Mode"; else -> "DCC · Dreambox Control Center" }
                TopAppBar(title = { Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(selected == 0, { selected = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(selected == 1, { selected = 1 }, icon = { Icon(Icons.Default.Devices, null) }, label = { Text("Devices") })
                    NavigationBarItem(selected == 2, { selected = 2 }, icon = { Icon(Icons.Default.LiveTv, null) }, label = { Text("Stream") })
                    NavigationBarItem(selected == 3, { selected = 3 }, icon = { Icon(Icons.Default.Terminal, null) }, label = { Text("Tools") })
                    NavigationBarItem(selected == 4, { selected = 4 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                    NavigationBarItem(selected == 5, { selected = 5 }, icon = { Icon(Icons.Default.VideoLibrary, null) }, label = { Text("PVR") })
                    NavigationBarItem(selected == 6, { selected = 6 }, icon = { Icon(Icons.Default.Headphones, null) }, label = { Text("Listen") })
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (streamUrl.isNotBlank() && selected != 2) {
                    MiniPlayer(player, isPlaying, onExpand = { selected = 2 }, onClose = { player.stop(); streamUrl = ""; isPlaying = false })
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (selected) {
                        0 -> HomeScreen(receiverIp, onOpenSettings = { selected = 4 }, onOpenDevices = { selected = 1 }, onOpenStream = { selected = 2 }, onOpenTools = { selected = 3 })
                        1 -> DevicesScreen(receiverIp)
                        2 -> StreamScreen(
                            player = player,
                            streamUrl = streamUrl,
                            urlInput = urlInput,
                            onUrlChange = { urlInput = it },
                            playing = isPlaying,
                            error = error,
                            onErrorDismiss = { error = null },
                            onPlay = {
                                val candidate = urlInput.trim()
                                val uri = runCatching { Uri.parse(candidate) }.getOrNull()
                                if (uri == null || uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) error = "Enter a valid HTTP or HTTPS stream URL from your Dreambox."
                                else {
                                    error = null
                                    streamUrl = candidate
                                    listenUrl = ""
                                    player.stop()
                                    player.setMediaItem(MediaItem.fromUri(uri))
                                    player.prepare()
                                    player.play()
                                }
                            },
                            onToggle = { if (isPlaying) player.pause() else if (streamUrl.isNotBlank()) player.play() else error = "Enter a stream URL to start playback." },
                            onFullscreen = { fullscreen = true },
                            onMinimize = { selected = 0 }
                        )
                        3 -> ToolsScreen(AppLanguage.ENGLISH, receiverIp, receiverUser, receiverPassword)
                        4 -> ReceiverSettingsScreen(ip = receiverIp, onIpChange = { receiverIp = it; savedMessage = null }, username = receiverUser, onUsernameChange = { receiverUser = it; savedMessage = null }, password = receiverPassword, onPasswordChange = { receiverPassword = it; savedMessage = null }, savedMessage = savedMessage, context = context, preferences = preferences, onSave = {
                            preferences.edit().putString("ip", receiverIp.trim()).putString("user", receiverUser.trim()).putString("password", receiverPassword).apply()
                            receiverIp = receiverIp.trim(); receiverUser = receiverUser.trim(); savedMessage = "Receiver details saved on this device."
                        })
                        5 -> PvrScreen(receiverIp, receiverUser, receiverPassword)
                        6 -> ListenScreen(player = player, listenUrl = listenUrl, urlInput = listenUrlInput, playing = isPlaying, error = listenError, onUrlChange = { listenUrlInput = it }, onPlay = {
                            val candidate = listenUrlInput.trim()
                            val uri = runCatching { Uri.parse(candidate) }.getOrNull()
                            if (uri == null || uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) listenError = "Enter a valid HTTP or HTTPS audio stream URL."
                            else {
                                listenError = null; listenUrl = candidate; streamUrl = ""
                                player.stop(); player.setMediaItem(MediaItem.fromUri(uri)); player.prepare(); player.play()
                            }
                        }, onToggle = { if (isPlaying) player.pause() else if (listenUrl.isNotBlank()) player.play() else listenError = "Enter an audio stream URL to start listening." }, onStop = { player.stop(); listenUrl = ""; isPlaying = false }, onErrorDismiss = { listenError = null })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiverSettingsScreen(ip: String, onIpChange: (String) -> Unit, username: String, onUsernameChange: (String) -> Unit, password: String, onPasswordChange: (String) -> Unit, savedMessage: String?, context: android.content.Context, preferences: android.content.SharedPreferences, onSave: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Receiver settings", style = MaterialTheme.typography.headlineSmall, color = Navy, fontWeight = FontWeight.Bold)
            Text("Enter the Dreambox IP address and login details.", color = Muted)
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Border)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = ip, onValueChange = onIpChange, modifier = Modifier.fillMaxWidth(), label = { Text("Receiver IP address or hostname") }, placeholder = { Text("192.168.1.104") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = onUsernameChange, modifier = Modifier.fillMaxWidth(), label = { Text("Username") }, placeholder = { Text("root") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = onPasswordChange, modifier = Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("Save receiver settings") }
                savedMessage?.let { Text(it, color = Blue, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        CustomThemeCard(context, preferences)
        BackupRestoreCard(context, preferences)
    }
}

@Composable
private fun StreamScreen(player: ExoPlayer, streamUrl: String, urlInput: String, onUrlChange: (String) -> Unit, playing: Boolean, error: String?, onErrorDismiss: () -> Unit, onPlay: () -> Unit, onToggle: () -> Unit, onFullscreen: () -> Unit, onMinimize: () -> Unit) {
    var protocol by remember { mutableStateOf(if (urlInput.startsWith("https://", true)) "HTTPS" else "HTTP") }
    fun rawUrl(value: String): String = value.removePrefix("http://").removePrefix("HTTP://").removePrefix("https://").removePrefix("HTTPS://")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Watch live TV", style = MaterialTheme.typography.headlineSmall, color = Navy, fontWeight = FontWeight.Bold)
            Text("Play a Dreambox stream inside DCC", color = Muted)
        }
        if (streamUrl.isNotBlank()) {
            Box {
                PlayerSurface(player, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)))
                TextButton(onClick = onFullscreen, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) { Text("FULL SCREEN", color = Color.White) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onToggle, modifier = Modifier.weight(1f)) { Text(if (playing) "Pause" else "Play") }
                OutlinedButton(onClick = onMinimize) { Text("Mini player") }
                OutlinedButton(onClick = onFullscreen) { Text("Expand") }
            }
            Text(if (playing) "LIVE · Streaming from your receiver" else "Stream ready", color = if (playing) Blue else Muted, style = MaterialTheme.typography.bodySmall)
            Text(streamUrl, color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)).background(Color(0xFF101820)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
                    Text("Your live stream will appear here", color = Color.White)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Border)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dreambox stream URL", color = Navy, fontWeight = FontWeight.Bold)
                Text("Protocol", color = Muted, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = protocol == "HTTP", onClick = { protocol = "HTTP"; val raw = rawUrl(urlInput); onUrlChange(if (raw.isBlank()) "" else "http://$raw") }, label = { Text("HTTP") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = protocol == "HTTPS", onClick = { protocol = "HTTPS"; val raw = rawUrl(urlInput); onUrlChange(if (raw.isBlank()) "" else "https://$raw") }, label = { Text("HTTPS") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = rawUrl(urlInput),
                    onValueChange = { raw -> val clean = rawUrl(raw.trim()); onUrlChange(if (clean.isBlank()) "" else "${protocol.lowercase()}://$clean") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("192.168.31.181:8001/<service-reference>") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), shape = RoundedCornerShape(14.dp)
                )
                Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) { Text(if (streamUrl.isBlank()) "Connect and play" else "Play stream") }
                Text("Enigma2 service streaming port is usually 8001.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        error?.let { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0))) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(it, color = Color(0xFF9C2F26), modifier = Modifier.weight(1f)); TextButton(onClick = onErrorDismiss) { Text("OK") } } } }
    }
}

@Composable
private fun PlayerSurface(player: ExoPlayer, modifier: Modifier, resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AndroidView(modifier = modifier.background(Color.Black), factory = { PlayerView(context).apply { this.player = player; useController = false; this.resizeMode = resizeMode; setShutterBackgroundColor(android.graphics.Color.BLACK) } }, update = { it.player = player; it.resizeMode = resizeMode })
}

@Composable
private fun MiniPlayer(player: ExoPlayer, playing: Boolean, onExpand: () -> Unit, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF102A43)).clickable(onClick = onExpand).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayerSurface(player, Modifier.width(112.dp).height(64.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(if (playing) "LIVE · Dreambox" else "Stream paused", color = Color.White, fontWeight = FontWeight.Bold); Text("Tap to return to player", color = Color.White.copy(alpha = .72f)) }
        TextButton(onClick = onClose) { Text("×", color = Color.White, fontSize = 22.sp) }
    }
}
