package com.mehmetbozkurt.clink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun App() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF16142B)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "clink.", color = Color(0xFFF3F0FF))
    }
}
