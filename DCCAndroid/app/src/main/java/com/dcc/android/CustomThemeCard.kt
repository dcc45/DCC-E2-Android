package com.dcc.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CustomThemeCard(context: Context, preferences: SharedPreferences) {
    var mode by remember { mutableStateOf(preferences.getString("theme_mode", "system") ?: "system") }
    var accent by remember { mutableStateOf(preferences.getString("theme_accent", "blue") ?: "blue") }
    var background by remember { mutableStateOf(preferences.getString("theme_background", "light") ?: "light") }
    var wallpaperStatus by remember {
        mutableStateOf(preferences.getString("theme_wallpaper", "")?.let {
            if (it.isBlank()) "No wallpaper selected" else "Custom wallpaper selected"
        } ?: "No wallpaper selected")
    }

    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                preferences.edit().putString("theme_wallpaper", uri.toString()).apply()
                wallpaperStatus = "Custom wallpaper selected"
                (context as? Activity)?.recreate()
            } catch (_: Exception) {
                wallpaperStatus = "Unable to save this wallpaper"
            }
        }
    }

    fun applyTheme() {
        preferences.edit()
            .putString("theme_mode", mode)
            .putString("theme_accent", accent)
            .putString("theme_background", background)
            .apply()
        (context as? Activity)?.recreate()
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFFE5EBF2))
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Custom Material theme", color = Color(0xFF102A43), fontWeight = FontWeight.Bold)

                Text("Appearance", color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                        FilterChip(selected = mode == value, onClick = { mode = value; applyTheme() }, label = { Text(label) }, modifier = Modifier.weight(1f))
                    }
                }

                Text("Accent color", color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("blue" to "Blue", "purple" to "Purple", "green" to "Green").forEach { (value, label) ->
                        FilterChip(selected = accent == value, onClick = { accent = value; applyTheme() }, label = { Text(label) }, modifier = Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("orange" to "Orange", "pink" to "Pink").forEach { (value, label) ->
                        FilterChip(selected = accent == value, onClick = { accent = value; applyTheme() }, label = { Text(label) }, modifier = Modifier.weight(1f))
                    }
                }

                Text("Background", color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("light" to "Light", "blue" to "Blue", "grey" to "Grey").forEach { (value, label) ->
                        FilterChip(selected = background == value, onClick = { background = value; applyTheme() }, label = { Text(label) }, modifier = Modifier.weight(1f))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cream" to "Cream", "navy" to "Navy").forEach { (value, label) ->
                        FilterChip(selected = background == value, onClick = { background = value; applyTheme() }, label = { Text(label) }, modifier = Modifier.weight(1f))
                    }
                }

                Text("Own wallpaper", color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Button(
                    onClick = { wallpaperPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Choose wallpaper") }
                if (wallpaperStatus == "Custom wallpaper selected") {
                    OutlinedButton(
                        onClick = {
                            preferences.edit().remove("theme_wallpaper").apply()
                            wallpaperStatus = "No wallpaper selected"
                            (context as? Activity)?.recreate()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Remove wallpaper") }
                }
                Text(wallpaperStatus, color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFFE5EBF2))
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("About DCC", color = Color(0xFF102A43), fontWeight = FontWeight.Bold)
                Text("View the source code and project updates on GitHub.", color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/dcc45/DCC-E2-Android")))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open GitHub repository") }
            }
        }
    }
}
