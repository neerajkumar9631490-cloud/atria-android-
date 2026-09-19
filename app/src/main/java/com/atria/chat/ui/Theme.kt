package com.atria.chat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.atria.chat.R

// ---------------------------------------------------------------------------
// Atria design tokens — ported from server.py web UI (aicss.dev palette)
// ---------------------------------------------------------------------------

val AtriaBgDark = Color(0xFF0B0E13)
val AtriaSurface1Dark = Color(0xFF0F1319)
val AtriaSurface2Dark = Color(0xFF161B23)
val AtriaSurface3Dark = Color(0xFF1D242F)
val AtriaBorderDark = Color(0xFF232B36)
val AtriaBorderStrongDark = Color(0xFF2F3947)
val AtriaTextDark = Color(0xFFE9EDF3)
val AtriaDimDark = Color(0xFF9AA5B4)
val AtriaFaintDark = Color(0xFF5F6B7A)
val AtriaAccentDark = Color(0xFFE2A45C)
val AtriaAccentStrongDark = Color(0xFFEFBD7F)
val AtriaOnAccentDark = Color(0xFF1A1106)

val AtriaBgLight = Color(0xFFF8FAFC)
val AtriaSurface1Light = Color(0xFFFFFFFF)
val AtriaSurface2Light = Color(0xFFF1F5F9)
val AtriaSurface3Light = Color(0xFFE2E8F0)
val AtriaBorderLight = Color(0xFFE2E8F0)
val AtriaBorderStrongLight = Color(0xFFCBD5E1)
val AtriaTextLight = Color(0xFF1E293B)
val AtriaDimLight = Color(0xFF475569)
val AtriaFaintLight = Color(0xFF94A3B8)
val AtriaAccentLight = Color(0xFF8B5CF6)
val AtriaAccentStrongLight = Color(0xFF7C3AED)
val AtriaOnAccentLight = Color(0xFFFFFFFF)

val AtriaSuccess = Color(0xFF10B981)
val AtriaWarning = Color(0xFFF59E0B)
val AtriaDanger = Color(0xFFEF4444)
val AtriaInfo = Color(0xFF3B82F6)

@Immutable
data class AtriaPalette(
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val dim: Color,
    val faint: Color,
    val accent: Color,
    val accentStrong: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val codeBg: Color
)

val DarkPalette = AtriaPalette(
    bg = AtriaBgDark,
    surface1 = AtriaSurface1Dark,
    surface2 = AtriaSurface2Dark,
    surface3 = AtriaSurface3Dark,
    border = AtriaBorderDark,
    borderStrong = AtriaBorderStrongDark,
    text = AtriaTextDark,
    dim = AtriaDimDark,
    faint = AtriaFaintDark,
    accent = AtriaAccentDark,
    accentStrong = AtriaAccentStrongDark,
    onAccent = AtriaOnAccentDark,
    accentSoft = Color(0x21E2A45C),
    codeBg = Color(0xFF0D1117)
)

val LightPalette = AtriaPalette(
    bg = AtriaBgLight,
    surface1 = AtriaSurface1Light,
    surface2 = AtriaSurface2Light,
    surface3 = AtriaSurface3Light,
    border = AtriaBorderLight,
    borderStrong = AtriaBorderStrongLight,
    text = AtriaTextLight,
    dim = AtriaDimLight,
    faint = AtriaFaintLight,
    accent = AtriaAccentLight,
    accentStrong = AtriaAccentStrongLight,
    onAccent = AtriaOnAccentLight,
    accentSoft = Color(0x148B5CF6),
    codeBg = Color(0xFF0D1117) // code blocks stay dark in both themes (like GitHub)
)

private fun AtriaPalette.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = accentStrong,
        background = bg,
        onBackground = text,
        surface = surface1,
        onSurface = text,
        surfaceVariant = surface2,
        onSurfaceVariant = dim,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface1,
        surfaceContainer = surface2,
        surfaceContainerHigh = surface3,
        outline = border,
        outlineVariant = borderStrong,
        error = AtriaDanger,
        secondary = dim,
        tertiary = accentStrong
    )
}

// ---------------------------------------------------------------------------
// Premium typography — Space Grotesk (display) + Inter (body) via
// downloadable Google Fonts. Falls back to system fonts offline /
// without Play Services. JetBrains Mono for code.
// ---------------------------------------------------------------------------

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val DisplayFamily = FontFamily(
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = fontProvider),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = fontProvider, weight = FontWeight.Bold)
)

val BodyFamily = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider),
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider, weight = FontWeight.Bold)
)

val MonoFamily = FontFamily.Monospace

private val AtriaTypography = Typography(
    displaySmall = TextStyle(fontFamily = DisplayFamily, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = DisplayFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = BodyFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = BodyFamily, fontSize = 15.5.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = BodyFamily, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = BodyFamily, fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = BodyFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontFamily = BodyFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
)

@Composable
fun AtriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = (if (darkTheme) DarkPalette else LightPalette).toColorScheme(darkTheme),
        typography = AtriaTypography,
        content = content
    )
}

@Composable
fun atriaPalette(darkTheme: Boolean): AtriaPalette = if (darkTheme) DarkPalette else LightPalette
