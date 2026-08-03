package com.thelab.mediahub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeStyle {
    MIDNIGHT_BLUE, CRIMSON_NOIR, WALNUT_NOIR, NEUMORPHIC_LIGHT
}

@Composable
fun TheLabTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.CRIMSON_NOIR,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeStyle) {
        AppThemeStyle.MIDNIGHT_BLUE -> darkColorScheme(
            primary = MidnightAccent,
            background = MidnightBg,
            surface = MidnightCard,
            onPrimary = TextWhite,
            onBackground = TextWhite,
            onSurface = TextWhite
        )
        AppThemeStyle.CRIMSON_NOIR -> darkColorScheme(
            primary = CrimsonAccent,
            background = CrimsonBg,
            surface = CrimsonCard,
            onPrimary = TextWhite,
            onBackground = TextWhite,
            onSurface = TextWhite
        )
        AppThemeStyle.WALNUT_NOIR -> darkColorScheme(
            primary = WalnutAccent,
            background = WalnutBg,
            surface = WalnutCard,
            onPrimary = TextWhite,
            onBackground = TextWhite,
            onSurface = TextWhite
        )
        AppThemeStyle.NEUMORPHIC_LIGHT -> lightColorScheme(
            primary = NeumorphicAccent,
            background = NeumorphicBg,
            surface = NeumorphicCard,
            onPrimary = Color.White,
            onBackground = Color(0xFF1A202C),
            onSurface = Color(0xFF1A202C)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
