package io.github.peacefulprogram.dy555.compose.screen

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.peacefulprogram.dy555.Constants
import io.github.peacefulprogram.dy555.R
import io.github.peacefulprogram.dy555.activity.DetailActivity
import io.github.peacefulprogram.dy555.compose.common.ErrorTip
import io.github.peacefulprogram.dy555.compose.common.Loading
import io.github.peacefulprogram.dy555.compose.common.VideoCard
import io.github.peacefulprogram.dy555.viewmodel.SearchResultViewModel
import kotlinx.coroutines.launch

@Composable
fun SearchResultScreen(viewModel: SearchResultViewModel) {
    val pagingItems = viewModel.pager.collectAsLazyPagingItems()
    val refreshState = pagingItems.loadState.refresh
    if (refreshState is LoadState.Loading) {
        Loading()
        return
    }
    if (refreshState is LoadState.Error) {
        ErrorTip(message = "加载错误:${refreshState.error.message}") {
            pagingItems.refresh()
        }
    }
    val containerWidth = Constants.VideoCardWidth * 1.1f
    val containerHeight = Constants.VideoCardHeight * 1.1f
    val context = LocalContext.current
    val gridState = rememberTvLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val titleFocusRequester = remember {
        FocusRequester()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        TvLazyVerticalGrid(
            columns = TvGridCells.Adaptive(containerWidth),
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            content = {
                item(span = { TvGridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.title_search_result),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .focusRequester(titleFocusRequester)
                            .focusable()
                    )
                }
                items(count = pagingItems.itemCount) { videoIndex ->
                    val video = pagingItems[videoIndex]!!
                    Box(
                        modifier = Modifier.size(containerWidth, containerHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoCard(
                            width = Constants.VideoCardWidth,
                            height = Constants.VideoCardHeight,
                            video = video,
                            onVideoClick = {
                                DetailActivity.startActivity(video.id, context)
                            },
                            onVideoKeyEvent = { _, keyEvent ->
                                if (keyEvent.key == Key.Menu && keyEvent.type == KeyEventType.KeyUp) {
                                    pagingItems.refresh()
                                    coroutineScope.launch {
                                        gridState.scrollToItem(0)
                                        titleFocusRequester.requestFocus()
                                    }
                                    true
                                } else if (keyEvent.key == Key.Back) {
                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                        coroutineScope.launch {
                                            gridState.scrollToItem(0)
                                            titleFocusRequester.requestFocus()
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                    }
                }
            }
        )

        if (pagingItems.itemCount == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(id = R.string.grid_no_data_tip),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }


    LaunchedEffect(Unit) {
        try {
            titleFocusRequester.requestFocus()
        } catch (e: Exception) {
            Log.w("SearchResultScreen", "request focus error: ${e.message}", e)
        }
    }
}
