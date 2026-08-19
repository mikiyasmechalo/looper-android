package com.example.looper_android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.looper_android.R

// Static Font Families for reference
val StaticDmSansFontFamily = FontFamily(
    Font(R.font.dmsans_light, FontWeight.Light),
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_semibold, FontWeight.SemiBold),
    Font(R.font.dmsans_bold, FontWeight.Bold)
)

val StaticJostFontFamily = FontFamily(
    Font(R.font.jost_light, FontWeight.Light),
    Font(R.font.jost_regular, FontWeight.Normal),
    Font(R.font.jost_medium, FontWeight.Medium),
    Font(R.font.jost_semibold, FontWeight.SemiBold),
    Font(R.font.jost_bold, FontWeight.Bold)
)

val StaticGoogleSansFontFamily = FontFamily(
    Font(R.font.googlesans_regular, FontWeight.Normal),
    Font(R.font.googlesans_medium, FontWeight.Medium),
    Font(R.font.googlesans_semibold, FontWeight.SemiBold),
    Font(R.font.googlesans_bold, FontWeight.Bold)
)

val StaticPlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plusjakartasans_light, FontWeight.Light),
    Font(R.font.plusjakartasans_regular, FontWeight.Normal),
    Font(R.font.plusjakartasans_medium, FontWeight.Medium),
    Font(R.font.plusjakartasans_semibold, FontWeight.SemiBold),
    Font(R.font.plusjakartasans_bold, FontWeight.Bold)
)

val StaticPoppinsFontFamily = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

val StaticSoraFontFamily = FontFamily(
    Font(R.font.sora_light, FontWeight.Light),
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_medium, FontWeight.Medium),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold)
)

val StaticSpaceGroteskFontFamily = FontFamily(
    Font(R.font.spacegrotesk_light, FontWeight.Light),
    Font(R.font.spacegrotesk_regular, FontWeight.Normal),
    Font(R.font.spacegrotesk_medium, FontWeight.Medium),
    Font(R.font.spacegrotesk_semibold, FontWeight.SemiBold),
    Font(R.font.spacegrotesk_bold, FontWeight.Bold)
)

// Dynamic Composable Font accessors so all UI components automatically use user-selected font!
val DmSansFontFamily: FontFamily
    @Composable get() = LocalAppFontFamily.current

val JostFontFamily: FontFamily
    @Composable get() = LocalAppFontFamily.current

fun getAppFontFamily(name: String?): FontFamily {
    return when (name?.trim()) {
        "Jost" -> StaticJostFontFamily
        "Google Sans" -> StaticGoogleSansFontFamily
        "Plus Jakarta Sans" -> StaticPlusJakartaSansFontFamily
        "Poppins" -> StaticPoppinsFontFamily
        "Sora" -> StaticSoraFontFamily
        "Space Grotesk" -> StaticSpaceGroteskFontFamily
        "DM Sans" -> StaticDmSansFontFamily
        else -> StaticDmSansFontFamily
    }
}

val LocalAppFontFamily = compositionLocalOf { StaticDmSansFontFamily }
val LocalLyricsFontFamily = compositionLocalOf { StaticDmSansFontFamily }
val LocalAppFontWeightDelta = compositionLocalOf { 0 }

// Dynamic Typography with disabled font padding for tight, consistent cross-font metrics
@Suppress("DEPRECATION")
fun createAppTypography(fontFamily: FontFamily = StaticDmSansFontFamily): Typography {
    val platformStyle = androidx.compose.ui.text.PlatformTextStyle(
        includeFontPadding = false
    )
    return Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
            platformStyle = platformStyle
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
            platformStyle = platformStyle
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            platformStyle = platformStyle
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            platformStyle = platformStyle
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            platformStyle = platformStyle
        )
    )
}

val Typography = createAppTypography(StaticDmSansFontFamily)
