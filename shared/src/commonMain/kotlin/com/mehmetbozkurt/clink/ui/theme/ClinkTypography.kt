package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.manrope_bold
import com.mehmetbozkurt.clink.resources.manrope_medium
import com.mehmetbozkurt.clink.resources.unbounded_extrabold
import com.mehmetbozkurt.clink.resources.unbounded_semibold
import org.jetbrains.compose.resources.Font

/** Headings, titles and labels in Unbounded (600/800); body text in Manrope (500/700). */
@Composable
internal fun clinkTypography(): Typography {
    val unbounded = FontFamily(
        Font(Res.font.unbounded_semibold, FontWeight.SemiBold),
        Font(Res.font.unbounded_extrabold, FontWeight.ExtraBold),
    )
    val manrope = FontFamily(
        Font(Res.font.manrope_medium, FontWeight.Medium),
        Font(Res.font.manrope_bold, FontWeight.Bold),
    )
    fun TextStyle.unbounded(weight: FontWeight) = copy(fontFamily = unbounded, fontWeight = weight)
    fun TextStyle.manrope() = copy(fontFamily = manrope, fontWeight = FontWeight.Medium)

    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.unbounded(FontWeight.ExtraBold),
        displayMedium = base.displayMedium.unbounded(FontWeight.ExtraBold),
        displaySmall = base.displaySmall.unbounded(FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.unbounded(FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.unbounded(FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.unbounded(FontWeight.ExtraBold),
        titleLarge = base.titleLarge.unbounded(FontWeight.SemiBold),
        titleMedium = base.titleMedium.unbounded(FontWeight.SemiBold),
        titleSmall = base.titleSmall.unbounded(FontWeight.SemiBold),
        labelLarge = base.labelLarge.unbounded(FontWeight.SemiBold),
        labelMedium = base.labelMedium.unbounded(FontWeight.SemiBold),
        labelSmall = base.labelSmall.unbounded(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.manrope(),
        bodyMedium = base.bodyMedium.manrope(),
        bodySmall = base.bodySmall.manrope(),
    )
}
