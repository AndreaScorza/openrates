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

/**
 * A monochrome palette: no accent colour at all, so the only things with weight
 * on screen are the numbers. `Ink` is the launcher icon's background and `Paper`
 * its coins, which is what ties the app to its icon here — not a shared hue.
 */
private val Ink = Color(0xFF14181A)
private val Paper = Color(0xFFFFFFFF)

/**
 * Container roles are set explicitly, not just `primary`/`secondary`/`tertiary`:
 * the currency pills are `FilledTonalButton`s painted by `secondaryContainer`,
 * and the cards by the `surfaceContainer*` roles. Left unset, those fall back to
 * Material's baseline, which is a purple-grey — the one colour this palette must
 * not have.
 */
private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Color(0xFFE8EBEB),
    onPrimaryContainer = Ink,
    secondary = Color(0xFF444949),
    onSecondary = Paper,
    secondaryContainer = Color(0xFFE8EBEB),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF444949),
    onTertiary = Paper,
    tertiaryContainer = Color(0xFFE8EBEB),
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE4E7E7),
    onSurfaceVariant = Color(0xFF444949),
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Color(0xFFF6F8F8),
    surfaceContainer = Color(0xFFF1F3F3),
    surfaceContainerHigh = Color(0xFFEBEEEE),
    surfaceContainerHighest = Color(0xFFE5E8E8),
    outline = Color(0xFF767B7B),
    outlineVariant = Color(0xFFC6CBCB),
)

private val DarkColors = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    primaryContainer = Color(0xFF2E3234),
    onPrimaryContainer = Paper,
    secondary = Color(0xFFC9CECE),
    onSecondary = Color(0xFF1B1F20),
    // Controls sit one clear step above every card tone below, which is the only
    // signal they get: with no accent colour, a tonal button that matches its card
    // is invisible.
    secondaryContainer = Color(0xFF323739),
    onSecondaryContainer = Paper,
    tertiary = Color(0xFFC9CECE),
    onTertiary = Color(0xFF1B1F20),
    tertiaryContainer = Color(0xFF323739),
    onTertiaryContainer = Paper,
    background = Color(0xFF0A0C0D),
    onBackground = Color(0xFFF2F4F4),
    surface = Color(0xFF0A0C0D),
    onSurface = Color(0xFFF2F4F4),
    surfaceVariant = Color(0xFF2E3234),
    onSurfaceVariant = Color(0xFFAEB4B4),
    // A deliberately compressed scale: every card stays close to the icon's own
    // background, so the cards read as one surface and only the controls lift off it.
    surfaceContainerLowest = Color(0xFF050607),
    surfaceContainerLow = Ink,
    surfaceContainer = Color(0xFF171B1D),
    surfaceContainerHigh = Color(0xFF191D1F),
    surfaceContainerHighest = Color(0xFF1B2022),
    outline = Color(0xFF797F7F),
    outlineVariant = Color(0xFF2E3234),
)

/**
 * Dynamic colour is off: with it on, Android generates the whole scheme from the
 * wallpaper and none of the colours above are used. Pass `dynamicColor = true`
 * to opt back in.
 */
@Composable
fun OpenRatesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
