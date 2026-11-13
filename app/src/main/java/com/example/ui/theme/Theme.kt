package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JevanMantraDarkColorScheme = darkColorScheme(
    primary = SaffronGoldDark,
    onPrimary = OnSaffronDark,
    primaryContainer = SaffronContainerDark,
    onPrimaryContainer = SaffronGoldDark,
    secondary = MysticRoseDark,
    onSecondary = Color(0xFF3B0014),
    secondaryContainer = Color(0xFF591429),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = CelestialTealDark,
    onTertiary = Color(0xFF003734),
    background = CosmicBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CosmicSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CosmicSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF5D487A)
)

private val JevanMantraLightColorScheme = lightColorScheme(
    primary = KumkumSaffronLight,
    onPrimary = OnKumkumLight,
    primaryContainer = KumkumContainerLight,
    onPrimaryContainer = Color(0xFF451A03),
    secondary = RoyalMaroonLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF4C0519),
    tertiary = SacredEmeraldLight,
    onTertiary = Color.White,
    background = ParchmentBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = ParchmentSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = ParchmentSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFC5B4D6)
)

@Composable
fun JevanMantraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) JevanMantraDarkColorScheme else JevanMantraLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
