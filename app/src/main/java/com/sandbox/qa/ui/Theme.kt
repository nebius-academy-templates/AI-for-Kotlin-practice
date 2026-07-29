package com.sandbox.qa.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Lime = Color(0xFFC9F73A)
val WarningOrange = Color(0xFFF5A623)

private val SandboxColors =
    darkColorScheme(
        primary = Lime,
        onPrimary = Color.Black,
        background = Color(0xFF141414),
        onBackground = Color.White,
        surface = Color(0xFF1F1F1F),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF2B2B2B),
        onSurfaceVariant = Color(0xFFB8B8B8),
        error = Color(0xFFFF6B6B),
    )

@Composable
fun SandboxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SandboxColors,
        content = content,
    )
}
