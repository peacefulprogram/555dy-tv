package io.github.peacefulprogram.dy555.compose.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme


@OptIn(ExperimentalTvMaterial3Api::class)
val DarkColors = darkColorScheme(

)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Dy555Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}