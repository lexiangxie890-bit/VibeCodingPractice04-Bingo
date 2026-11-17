package com.example.bingopractice04.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
/** Applies the Material 3 theme used by every screen in the Bingo app. */
fun BingoTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
