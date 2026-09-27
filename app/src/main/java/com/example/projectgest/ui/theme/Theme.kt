package com.example.projectgest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colores de la marca que Material 3 no cubre directamente. */
@Immutable
data class ColoresExtra(
    val textoSecundario: Color,
    val exito: Color,
    val exitoFondo: Color,
    val peligro: Color,
    val peligroFondo: Color,
    val acento: Color,
    val acentoFondo: Color,
    val acentoBorde: Color,
    val chip: Color,
    val borde: Color,
)

private val ExtraLight = ColoresExtra(
    textoSecundario = TextMutedLight,
    exito = SuccessTextLight,
    exitoFondo = SuccessBgLight,
    peligro = DangerTextLight,
    peligroFondo = DangerBgLight,
    acento = AccentTextLight,
    acentoFondo = AccentBgLight,
    acentoBorde = AccentBorderLight,
    chip = ChipLight,
    borde = BorderLight,
)

private val ExtraDark = ColoresExtra(
    textoSecundario = TextMutedDark,
    exito = SuccessTextDark,
    exitoFondo = SuccessBgDark,
    peligro = DangerTextDark,
    peligroFondo = DangerBgDark,
    acento = AccentTextDark,
    acentoFondo = AccentBgDark,
    acentoBorde = AccentBorderDark,
    chip = ChipDark,
    borde = BorderDark,
)

val LocalColoresExtra = staticCompositionLocalOf { ExtraLight }

private val LightColorScheme = lightColorScheme(
    primary = TextLight,
    onPrimary = SurfaceLight,
    primaryContainer = AccentBgLight,
    onPrimaryContainer = AccentTextLight,
    secondary = AccentTextLight,
    onSecondary = SurfaceLight,
    secondaryContainer = AccentBgLight,
    onSecondaryContainer = AccentTextLight,
    background = BgLight,
    onBackground = TextLight,
    surface = BgLight,
    onSurface = TextLight,
    onSurfaceVariant = TextMutedLight,
    surfaceVariant = Color(0xFFEFF3E6),
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = Color(0xFFFBFCF7),
    surfaceContainerHighest = Color(0xFFF0F4E8),
    outline = Color(0xFFD6D6D2),
    outlineVariant = BorderLight,
    error = DangerTextLight,
    onError = SurfaceLight,
    errorContainer = DangerBgLight,
    onErrorContainer = DangerTextLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = TextDark,
    onPrimary = BgDark,
    primaryContainer = AccentBgDark,
    onPrimaryContainer = AccentTextDark,
    secondary = AccentTextDark,
    onSecondary = BgDark,
    secondaryContainer = AccentBgDark,
    onSecondaryContainer = AccentTextDark,
    background = BgDark,
    onBackground = TextDark,
    surface = BgDark,
    onSurface = TextDark,
    onSurfaceVariant = TextMutedDark,
    surfaceVariant = Color(0xFF22221F),
    surfaceContainerLowest = BgDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF20201D),
    surfaceContainerHighest = Color(0xFF262623),
    outline = Color(0xFF3A3A36),
    outlineVariant = Color(0xFF2A2A27),
    error = DangerTextDark,
    onError = BgDark,
    errorContainer = DangerBgDark,
    onErrorContainer = DangerTextDark,
)

@Composable
fun ProjectGestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Sin dynamic color: la app mantiene la identidad visual de la web.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extra = if (darkTheme) ExtraDark else ExtraLight

    CompositionLocalProvider(LocalColoresExtra provides extra) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

object ProjectGestTheme {
    val extra: ColoresExtra
        @Composable get() = LocalColoresExtra.current
}
