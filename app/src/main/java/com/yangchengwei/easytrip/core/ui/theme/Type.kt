package com.yangchengwei.easytrip.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.R

private val HeadingFont = FontFamily(
    Font(R.font.funnel_sans, FontWeight.Normal),
    Font(R.font.funnel_sans, FontWeight.Medium),
    Font(R.font.funnel_sans, FontWeight.SemiBold),
    Font(R.font.funnel_sans, FontWeight.Bold),
)
private val BodyFont = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
)

val easyTripTypography = Typography(
    displaySmall = TextStyle(fontFamily = HeadingFont, fontSize = 32.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = HeadingFont, fontSize = 26.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = HeadingFont, fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = HeadingFont, fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = HeadingFont, fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontFamily = HeadingFont, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = BodyFont, fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = BodyFont, fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = BodyFont, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontFamily = BodyFont, fontSize = 11.sp, fontWeight = FontWeight.Medium),
)
