package com.noto.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun getAppTypography(
    fontFamily: FontFamily = FontFamily.Default,
    scale: Float = 1.0f
): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (34 * scale).sp,
        lineHeight = (40 * scale).sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (24 * scale).sp,
        lineHeight = (30 * scale).sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (20 * scale).sp,
        lineHeight = (26 * scale).sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (18 * scale).sp,
        lineHeight = (24 * scale).sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (16 * scale).sp,
        lineHeight = (22 * scale).sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (16 * scale).sp,
        lineHeight = (24 * scale).sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14 * scale).sp,
        lineHeight = (20 * scale).sp,
        letterSpacing = 0.25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (12 * scale).sp,
        lineHeight = (16 * scale).sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (11 * scale).sp,
        lineHeight = (14 * scale).sp,
        letterSpacing = 0.3.sp
    )
)

val Typography = getAppTypography()
