package com.thelab.mediahub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeStyle {
    NEUMORPHIC_LIGHT, MIDNIGHT_DARK, CRIMSON_NOIR
}

@Composable
fun TheLabTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.CRIMSON_NOIR,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeStyle) {
        AppThemeStyle.NEUMORPHIC_LIGHT -> lightColorScheme(
            primary = NeuLightDarkText,
            background = NeuLightBg,
            surface = NeuLightSurface,
            onPrimary = NeuLightHighlight,
            onBackground = NeuLightDarkText,
            onSurface = NeuLightDarkText
        )
        AppThemeStyle.MIDNIGHT_DARK -> darkColorScheme(
            primary = NeuDarkText,
            background = NeuDarkBg,
            surface = NeuDarkSurface,
            onPrimary = NeuDarkText,
            onBackground = NeuDarkText,
            onSurface = NeuDarkText
        )
        AppThemeStyle.CRIMSON_NOIR -> darkColorScheme(
            primary = CrimsonAccent,
            background = CrimsonBg,
            surface = CrimsonCard,
            onPrimary = TextWhite,
            onBackground = TextWhite,
            onSurface = TextWhite
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
