package com.andrea.openrates.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Mint = Color(0xFF00696D)
private val MintDark = Color(0xFF4FD9DE)

private val LightColors = lightColorScheme(
    primary = Mint,
    secondary = Color(0xFF4A6365),
    tertiary = Color(0xFF4F5F7E),
)

private val DarkColors = darkColorScheme(
    primary = MintDark,
    secondary = Color(0xFFB1CBCD),
    tertiary = Color(0xFFB7C7EA),
)

@Composable
fun OpenRatesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
