package io.github.peacefulprogram.dy555.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import io.github.peacefulprogram.dy555.compose.screen.HomeScreen
import io.github.peacefulprogram.dy555.compose.theme.Dy555Theme
import io.github.peacefulprogram.dy555.ext.showShortToast

/**
 * Loads [MainFragment].
 */
class MainActivity : ComponentActivity() {

    private var lastClickBackTime: Long = 0L

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Dy555Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    androidx.tv.material3.Surface(modifier = Modifier.fillMaxSize()) {
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

    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastClickBackTime < 2000) {
            super.onBackPressed()
        } else {
            lastClickBackTime = now
            this.showShortToast("再次点击退出应用")
        }
    }
}

