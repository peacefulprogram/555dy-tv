package io.github.peacefulprogram.dy555.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import cn.hutool.core.net.URLEncodeUtil
import io.github.peacefulprogram.dy555.compose.screen.SearchResultScreen
import io.github.peacefulprogram.dy555.compose.theme.Dy555Theme
import io.github.peacefulprogram.dy555.viewmodel.SearchResultViewModel
import org.koin.android.ext.android.get
import org.koin.core.parameter.parametersOf

class SearchResultActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val param = intent.getStringExtra("p")!!
        val viewModel = get<SearchResultViewModel> { parametersOf(param) }
        setContent {
            Dy555Theme {
                Surface {
                    androidx.tv.material3.Surface(
                        modifier = Modifier.padding(48.dp, 27.dp)
                    ) {
                        SearchResultScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    companion object {
        fun startActivityByKeyword(context: Context, keyword: String) {
            startActivity(context, "${URLEncodeUtil.encode(keyword, Charsets.UTF_8)}-------------")
        }

        fun startActivity(context: Context, param: String) {
            Intent(context, SearchResultActivity::class.java).apply {
                putExtra("p", param)
                context.startActivity(this)
            }
        }
    }
}