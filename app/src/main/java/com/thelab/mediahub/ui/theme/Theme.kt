package com.thelab.mediahub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class AppThemeStyle {
    NEUMORPHIC_LIGHT, MIDNIGHT_DARK
}

@Composable
fun TheLabTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.NEUMORPHIC_LIGHT,
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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
