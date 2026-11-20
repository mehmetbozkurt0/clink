package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.tex_paper
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.imageResource

/** Fixed "torn" offsets (0..1) for the top and bottom edges; the same every time, so the edge never jitters. */
private val TornSteps = floatArrayOf(0.2f, 0.9f, 0.4f, 1f, 0.1f, 0.7f, 0.3f, 0.8f, 0f, 0.6f, 0.95f, 0.35f)

/** A paper sheet with uneven top and bottom edges and straight sides. */
internal val TornEdgeShape = GenericShape { size, _ ->
    val teeth = 24
    val step = size.width / teeth
    val depth = minOf(step * 0.22f, size.height * 0.02f)
    moveTo(0f, depth * TornSteps[0])
    for (i in 1..teeth) lineTo(i * step, depth * TornSteps[i % TornSteps.size])
    lineTo(size.width, size.height - depth * TornSteps[(teeth + 3) % TornSteps.size])
    for (i in teeth - 1 downTo 0) lineTo(i * step, size.height - depth * TornSteps[(i + 3) % TornSteps.size])
    close()
}

/**
 * A paper poster on the wall (design §3.3): paper color with a faint paper texture, torn top and bottom
 * edges, an optional small rotation, and ink as the default content color.
 */
@Composable
fun Poster(
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val paper = rememberTiledBrush(imageResource(Res.drawable.tex_paper))
    CompositionLocalProvider(LocalContentColor provides ClinkColors.Ink) {
        Column(
            modifier = modifier
                .graphicsLayer { rotationZ = rotation }
                .clip(TornEdgeShape)
                .background(ClinkColors.Paper)
                .drawBehind { drawRect(paper, alpha = 0.09f) }
                .padding(contentPadding),
            content = content,
        )
    }
}
