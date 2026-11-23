package com.dcc.android

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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

    fun applyTheme() {
        preferences.edit()
            .putString("theme_mode", mode)
            .putString("theme_accent", accent)
            .putString("theme_background", background)
            .apply()
        (context as? Activity)?.recreate()
    }

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
                    FilterChip(
                        selected = mode == value,
                        onClick = { mode = value; applyTheme() },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Accent color", color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("blue" to "Blue", "purple" to "Purple", "green" to "Green").forEach { (value, label) ->
                    FilterChip(
                        selected = accent == value,
                        onClick = { accent = value; applyTheme() },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("orange" to "Orange", "pink" to "Pink").forEach { (value, label) ->
                    FilterChip(
                        selected = accent == value,
                        onClick = { accent = value; applyTheme() },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Background", color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("light" to "Light", "blue" to "Blue", "grey" to "Grey").forEach { (value, label) ->
                    FilterChip(
                        selected = background == value,
                        onClick = { background = value; applyTheme() },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("cream" to "Cream", "navy" to "Navy").forEach { (value, label) ->
                    FilterChip(
                        selected = background == value,
                        onClick = { background = value; applyTheme() },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Theme changes apply instantly.", color = Color(0xFF68788B), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
