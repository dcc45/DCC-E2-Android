package com.dcc.android

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DccBlue = Color(0xFF246BFD)
private val DccNavy = Color(0xFF102A43)

@Composable
fun DccTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = context.getSharedPreferences("dcc_receiver", android.content.Context.MODE_PRIVATE)
    val mode = preferences.getString("theme_mode", "system") ?: "system"
    val accent = preferences.getString("theme_accent", "blue") ?: "blue"
    val background = preferences.getString("theme_background", "light") ?: "light"

    val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val useDark = when (mode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    val accentColor = when (accent) {
        "purple" -> Color(0xFF7C4DFF)
        "green" -> Color(0xFF269B75)
        "orange" -> Color(0xFFE76F51)
        "pink" -> Color(0xFFD95B86)
        else -> DccBlue
    }

    val backgroundColor = when (background) {
        "navy" -> if (useDark) Color(0xFF07111F) else Color(0xFF0E2948)
        "blue" -> if (useDark) Color(0xFF101F32) else Color(0xFFEAF1FF)
        "grey" -> if (useDark) Color(0xFF17191C) else Color(0xFFF1F2F4)
        "cream" -> if (useDark) Color(0xFF201B16) else Color(0xFFF8F5EC)
        else -> if (useDark) Color(0xFF07111F) else Color(0xFFF4F7FB)
    }

    val lightColors = lightColorScheme(
        primary = accentColor,
        onPrimary = Color.White,
        primaryContainer = accentColor.copy(alpha = 0.16f),
        onPrimaryContainer = DccNavy,
        secondary = accentColor,
        background = backgroundColor,
        onBackground = DccNavy,
        surface = Color.White,
        onSurface = DccNavy,
        surfaceVariant = Color(0xFFE5EBF2),
        outline = Color(0xFFD7DFE8),
        error = Color(0xFF9C2F26)
    )

    val darkColors = darkColorScheme(
        primary = accentColor,
        onPrimary = if (accent == "green") Color(0xFF06231A) else Color.White,
        primaryContainer = accentColor.copy(alpha = 0.28f),
        onPrimaryContainer = Color(0xFFEAF0F7),
        secondary = accentColor,
        background = backgroundColor,
        onBackground = Color(0xFFEAF0F7),
        surface = Color(0xFF102033),
        onSurface = Color(0xFFEAF0F7),
        surfaceVariant = Color(0xFF24364A),
        outline = Color(0xFF52647A),
        error = Color(0xFFFFB4AA)
    )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )

    MaterialTheme(
        colorScheme = if (useDark) darkColors else lightColors,
        shapes = shapes,
        content = content
    )
}
