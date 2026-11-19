package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.ui.theme.ClinkColors

@Composable
fun ClinkCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .background(ClinkColors.Surface, RoundedCornerShape(16.dp))
            .padding(20.dp),
        content = content,
    )
}
