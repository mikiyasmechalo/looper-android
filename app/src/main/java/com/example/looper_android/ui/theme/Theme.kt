package com.example.looper_android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandAccent,
    secondary = BrandAccent,
    tertiary = BrandAccent,
    background = PremiumDeepBlack,
    surface = PremiumDeepBlack,
    surfaceVariant = PremiumContainer,
    onPrimary = OledBlack,
    onSecondary = OledBlack,
    onTertiary = OledBlack,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
)

@Composable
fun LooperandroidTheme(
    darkTheme: Boolean = true,
    dynamicTheming: Boolean = true,
    customAccentColor: androidx.compose.ui.graphics.Color = BrandAccent,
    fontFamilyName: String = "DM Sans",
    fontWeightDelta: Int = 0,
    lyricsFontFamilyName: String = "DM Sans",
    content: @Composable () -> Unit
) {
    val safeAccentColor = if (customAccentColor == androidx.compose.ui.graphics.Color.Transparent || customAccentColor.alpha < 0.1f) {
        BrandAccent
    } else {
        customAccentColor.copy(alpha = 1f)
    }

    val effectiveDarkTheme = darkTheme && !dynamicTheming
    val containerColor = if (effectiveDarkTheme) androidx.compose.ui.graphics.Color(0xFF0A0A0A) else PremiumContainer
    val backgroundColor = if (effectiveDarkTheme) androidx.compose.ui.graphics.Color.Black else PremiumDeepBlack
    
    val colorScheme = DarkColorScheme.copy(
        primary = safeAccentColor,
        secondary = safeAccentColor,
        tertiary = safeAccentColor,
        background = backgroundColor,
        surface = backgroundColor,
        surfaceVariant = containerColor
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    val selectedFontFamily = remember(fontFamilyName) { getAppFontFamily(fontFamilyName) }
    val selectedLyricsFontFamily = remember(lyricsFontFamilyName) { getAppFontFamily(lyricsFontFamilyName) }
    val dynamicTypography = remember(selectedFontFamily) { createAppTypography(selectedFontFamily) }

    CompositionLocalProvider(
        LocalAccentColor provides safeAccentColor,
        LocalAppFontFamily provides selectedFontFamily,
        LocalLyricsFontFamily provides selectedLyricsFontFamily,
        LocalAppFontWeightDelta provides fontWeightDelta
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = dynamicTypography,
            content = content
        )
    }
}
