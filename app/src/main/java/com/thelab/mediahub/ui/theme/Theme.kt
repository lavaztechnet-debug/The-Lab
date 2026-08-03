package com.thelab.mediahub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeStyle {
    CHARCOAL, CYBERPUNK, EMERALD, NORDIC
}

@Composable
fun TheLabTheme(
    darkTheme: Boolean = true,
    themeStyle: AppThemeStyle = AppThemeStyle.CHARCOAL,
    content: @Composable () -> Unit
) {
    val primaryColor = when (themeStyle) {
        AppThemeStyle.CHARCOAL -> ElectricBlue
        AppThemeStyle.CYBERPUNK -> NeonPink
        AppThemeStyle.EMERALD -> EmeraldGreen
        AppThemeStyle.NORDIC -> FrostCyan
    }

    val bgColor = if (darkTheme) {
        when (themeStyle) {
            AppThemeStyle.CHARCOAL -> CharcoalBg
            AppThemeStyle.CYBERPUNK -> CyberBg
            AppThemeStyle.EMERALD -> EmeraldBg
            AppThemeStyle.NORDIC -> NordicBg
        }
    } else {
        Color(0xFFF0F2F5)
    }

    val cardColor = if (darkTheme) {
        when (themeStyle) {
            AppThemeStyle.CHARCOAL -> CharcoalCard
            AppThemeStyle.CYBERPUNK -> CyberCard
            AppThemeStyle.EMERALD -> EmeraldCard
            AppThemeStyle.NORDIC -> NordicCard
        }
    } else {
        Color(0xFFFFFFFF)
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            background = bgColor,
            surface = cardColor,
            onPrimary = TextWhite,
            onBackground = TextWhite,
            onSurface = TextWhite
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            background = bgColor,
            surface = cardColor,
            onPrimary = Color.White,
            onBackground = Color(0xFF111111),
            onSurface = Color(0xFF111111)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
