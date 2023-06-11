package io.github.peacefulprogram.dy555.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import io.github.peacefulprogram.dy555.compose.screen.HomeScreen
import io.github.peacefulprogram.dy555.compose.theme.Dy555Theme

/**
 * Loads [MainFragment].
 */
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Dy555Theme {
                Surface(shape = NonInteractiveSurfaceDefaults.shape) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        HomeScreen()
                    }
                }
            }
        }
    }
}
