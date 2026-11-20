package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts

/** A strip of masking tape: straight long edges, zig-zag torn ends. */
private val RaggedEnds = GenericShape { size, _ ->
    val tooth = size.height / 4f
    val bite = minOf(size.width * 0.03f, 6f)
    moveTo(bite, 0f)
    lineTo(size.width - bite, 0f)
    for (i in 1..4) lineTo(size.width - if (i % 2 == 1) 0f else bite, i * tooth)
    lineTo(bite, size.height)
    for (i in 3 downTo 0) lineTo(if (i % 2 == 1) 0f else bite, i * tooth)
    close()
}

/** A short handwritten label on tape (design §3.3); keep it to a few words. */
@Composable
fun TapeLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .rotate(-2.5f)
            .clip(RaggedEnds)
            .background(ClinkColors.Tape)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        color = ClinkColors.Ink,
        style = TextStyle(fontFamily = ClinkFonts.marker, fontSize = 18.sp),
    )
}
