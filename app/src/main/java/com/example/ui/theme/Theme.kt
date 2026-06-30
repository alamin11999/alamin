package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisPrimary,
    secondary = JarvisSecondary,
    tertiary = JarvisTertiary,
    background = JarvisBackground,
    surface = JarvisSurface,
    onPrimary = Color(0xFF002E35),
    onSecondary = Color(0xFF003024),
    onTertiary = Color(0xFF332F00),
    onBackground = IceBlueText,
    onSurface = IceBlueText,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = JarvisBackground.toArgb()
            window.navigationBarColor = JarvisBackground.toArgb()
            
            // Set light status/navigation bar icons since background is extremely dark
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
