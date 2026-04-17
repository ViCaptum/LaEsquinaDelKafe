package com.brain.laesquinadelkafe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CoffeeLight,
    secondary = CoffeeMedium,
    tertiary = AccentGold,
    background = CoffeeDark,
    surface = CoffeeMedium,
    onPrimary = CreamBackground,
    onSecondary = CreamBackground,
    onBackground = CreamBackground,
    onSurface = CreamBackground
)

private val LightColorScheme = lightColorScheme(
    primary = CoffeeMedium,
    secondary = CoffeeLight,
    tertiary = AccentGold,
    background = CreamBackground,
    surface = CardWhite,
    onPrimary = CardWhite,
    onSecondary = CoffeeDark,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun LaEsquinaDelKafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
