package io.github.peacefulprogram.dy555.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import io.github.peacefulprogram.dy555.compose.screen.CategoriesScreen
import io.github.peacefulprogram.dy555.compose.theme.Dy555Theme
import io.github.peacefulprogram.dy555.viewmodel.CategoriesViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class CategoriesActivity : ComponentActivity() {


    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultParam = intent.getStringExtra("p")!!
        val viewModel by viewModel<CategoriesViewModel> { parametersOf(defaultParam) }
        setContent {
            Dy555Theme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    androidx.tv.material3.Surface(modifier = Modifier.fillMaxSize()) {
                        CategoriesScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    companion object {
        fun startActivity(defaultParam: String, context: Context) {
            Intent(context, CategoriesActivity::class.java).apply {
                putExtra("p", defaultParam)
                context.startActivity(this)
            }
        }
    }
}