package com.mehmetbozkurt.clink.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors

private val VirtualWidth = 360.dp
private val VirtualHeight = 720.dp
private val FrameShape = RoundedCornerShape(24.dp)
private val ScreenShape = RoundedCornerShape(16.dp)

/**
 * A phone-shaped frame showing [content] as a non-interactive preview. The content is laid out in a fixed
 * 360 x 720 dp box (so whole screens that fill their size look as on a phone) and scaled down to fit the frame;
 * it ignores touches and is hidden from screen readers.
 */
@Composable
fun PhoneFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(FrameShape)
            .background(ClinkColors.WallRaised)
            .border(2.dp, ClinkColors.Paper.copy(alpha = 0.14f), FrameShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val scale = minOf(maxWidth / VirtualWidth, maxHeight / VirtualHeight)
            Box(
                modifier = Modifier
                    .requiredSize(VirtualWidth, VirtualHeight)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(ScreenShape)
                    .background(ClinkColors.Wall)
                    .clearAndSetSemantics { },
            ) {
                content()
                // Sits on top so the preview never gets a touch; the parent pager still sees swipes.
                Box(Modifier.matchParentSize().pointerInput(Unit) { })
            }
        }
    }
}
