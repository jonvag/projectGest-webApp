package com.example.projectgest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Tipografía sobria como la web: pesos medios y tracking negativo en títulos
// (equivalente a `font-medium tracking-tight`), cifras tabulares en montos.
private val base = Typography()

val Typography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.015).em),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.01).em),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    bodyLarge = base.bodyLarge.copy(letterSpacing = 0.sp),
    bodyMedium = base.bodyMedium.copy(letterSpacing = 0.sp),
    bodySmall = base.bodySmall.copy(letterSpacing = 0.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    labelMedium = base.labelMedium.copy(letterSpacing = 0.sp),
    labelSmall = base.labelSmall.copy(letterSpacing = 0.2.sp),
)

val EstiloCifra = TextStyle(fontFeatureSettings = "tnum")
