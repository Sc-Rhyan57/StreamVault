package com.streamvault.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object StreamColors {
    val Background     = Color(0xFF000000)
    val Surface        = Color(0xFF141414)
    val SurfaceVar     = Color(0xFF1F1F1F)
    val Card           = Color(0xFF1A1A1A)
    val Primary        = Color(0xFFE50914)
    val PrimaryVariant = Color(0xFFB20710)
    val OnPrimary      = Color(0xFFFFFFFF)
    val TextPrimary    = Color(0xFFFFFFFF)
    val TextSecondary  = Color(0xFFB3B3B3)
    val TextMuted      = Color(0xFF757575)
    val Divider        = Color(0xFF2A2A2A)
    val Overlay        = Color(0x99000000)
    val OverlayLight   = Color(0x55000000)
    val TopBadge       = Color(0xFFE50914)
    val GoldRating     = Color(0xFFFFD700)
    val Success        = Color(0xFF46D369)
    val Warning        = Color(0xFFFFA500)
}

private val DarkColorScheme = darkColorScheme(
    primary          = StreamColors.Primary,
    onPrimary        = StreamColors.OnPrimary,
    primaryContainer = StreamColors.PrimaryVariant,
    background       = StreamColors.Background,
    surface          = StreamColors.Surface,
    surfaceVariant   = StreamColors.SurfaceVar,
    onBackground     = StreamColors.TextPrimary,
    onSurface        = StreamColors.TextPrimary,
    onSurfaceVariant = StreamColors.TextSecondary,
    error            = StreamColors.Primary
)

private val AppTypography = Typography(
    displayLarge   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black,   fontSize = 32.sp),
    displayMedium  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,    fontSize = 28.sp),
    headlineLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,    fontSize = 22.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 18.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 16.sp),
    titleLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,    fontSize = 20.sp),
    titleMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 16.sp),
    titleSmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 14.sp),
    bodyLarge      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 16.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 14.sp),
    bodySmall      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 12.sp),
    labelLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 14.sp),
    labelSmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 10.sp)
)

@Composable
fun StreamVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
