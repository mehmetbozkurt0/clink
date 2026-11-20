package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.anton_regular
import com.mehmetbozkurt.clink.resources.archivo_variable
import com.mehmetbozkurt.clink.resources.caveat_brush
import com.mehmetbozkurt.clink.resources.rubik_spray_paint
import org.jetbrains.compose.resources.Font

/** The two accent fonts (design §3.2): spray for the logo only, marker for tape labels and short notes. */
object ClinkFonts {
    val spray: FontFamily
        @Composable get() = FontFamily(Font(Res.font.rubik_spray_paint))

    val marker: FontFamily
        @Composable get() = FontFamily(Font(Res.font.caveat_brush))
}

/**
 * Headings, titles and labels in Anton (one heavy weight; no faux bold), everything people read in
 * Archivo (variable font: 500 / 700 / 800).
 */
@Composable
internal fun clinkTypography(): Typography {
    val anton = FontFamily(Font(Res.font.anton_regular, FontWeight.Normal))
    val archivo = FontFamily(
        Font(Res.font.archivo_variable, FontWeight.Medium),
        Font(Res.font.archivo_variable, FontWeight.Bold),
        Font(Res.font.archivo_variable, FontWeight.ExtraBold),
    )
    fun TextStyle.anton() = copy(fontFamily = anton, fontWeight = FontWeight.Normal, fontSynthesis = FontSynthesis.None)
    fun TextStyle.archivo() = copy(fontFamily = archivo, fontWeight = FontWeight.Medium)

    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.anton(),
        displayMedium = base.displayMedium.anton(),
        displaySmall = base.displaySmall.anton(),
        headlineLarge = base.headlineLarge.anton(),
        headlineMedium = base.headlineMedium.anton(),
        headlineSmall = base.headlineSmall.anton(),
        titleLarge = base.titleLarge.anton(),
        titleMedium = base.titleMedium.anton(),
        titleSmall = base.titleSmall.anton(),
        labelLarge = base.labelLarge.anton(),
        labelMedium = base.labelMedium.anton(),
        labelSmall = base.labelSmall.anton(),
        bodyLarge = base.bodyLarge.archivo(),
        bodyMedium = base.bodyMedium.archivo(),
        bodySmall = base.bodySmall.archivo(),
    )
}
