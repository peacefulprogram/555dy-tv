package io.github.peacefulprogram.dy555.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import io.github.peacefulprogram.dy555.compose.screen.DetailScreen
import io.github.peacefulprogram.dy555.compose.theme.Dy555Theme
import io.github.peacefulprogram.dy555.viewmodel.VideoDetailViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DetailActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoId = intent.getStringExtra("id")
        setContent {
            Dy555Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: VideoDetailViewModel by viewModel { parametersOf(videoId) }
                    Box(modifier = Modifier.padding(20.dp)) {
                        DetailScreen(viewModel)
                    }
                }
            }
        }
    }

    companion object {
        fun startActivity(videoId: String, context: Context) {
            Intent(context, DetailActivity::class.java).apply {
                putExtra("id", videoId)
                context.startActivity(this)
            }
        }
    }
}