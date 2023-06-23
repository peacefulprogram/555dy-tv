package io.github.peacefulprogram.dy555.compose.screen

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.peacefulprogram.dy555.Constants
import io.github.peacefulprogram.dy555.R
import io.github.peacefulprogram.dy555.activity.DetailActivity
import io.github.peacefulprogram.dy555.compose.common.ErrorTip
import io.github.peacefulprogram.dy555.compose.common.Loading
import io.github.peacefulprogram.dy555.compose.common.VideoCard
import io.github.peacefulprogram.dy555.compose.theme.Dy555Theme
import io.github.peacefulprogram.dy555.compose.util.FocusGroup
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.viewmodel.CategoriesViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

typealias VideoFilter = List<Triple<Int, String, List<Pair<String, String>>>>

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel
) {
    val pagingItems = viewModel.pager.collectAsLazyPagingItems()
    var showFilterDialog by remember {
        mutableStateOf(false)
    }

    val state = rememberTvLazyGridState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val videoCardContainerWidth = Constants.VideoCardWidth * 1.1f
    val videoCardContainerHeight = Constants.VideoCardHeight * 1.1f

    val refreshState = pagingItems.loadState.refresh
    val titleFocusRequester = remember {
        FocusRequester()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TvLazyVerticalGrid(columns = TvGridCells.Adaptive(videoCardContainerWidth),
            horizontalArrangement = Arrangement.SpaceEvenly,
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent {
                    if (it.key == Key.Back && it.type == KeyEventType.KeyUp && state.firstVisibleItemIndex > 0) {
                        coroutineScope.launch {
                            state.scrollToItem(0)
                            titleFocusRequester.requestFocus()
                        }
                        true
                    } else if (it.key == Key.Menu && it.type == KeyEventType.KeyUp) {
                        pagingItems.refresh()
                        titleFocusRequester.requestFocus()
                        true
                    } else {
                        false
                    }

                },
            content = {
                item(span = {
                    TvGridItemSpan(maxLineSpan)
                }) {
                    Row(
                        verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(
                            start = 15.dp,
                            top = 15.dp
                        )
                    ) {
                        Surface(
                            onClick = { },
                            onLongClick = { showFilterDialog = true },
                            colors = ClickableSurfaceDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface),
                            scale = ClickableSurfaceScale.None,
                            modifier = Modifier.focusRequester(titleFocusRequester)
                        ) {
                            Text(
                                text = stringResource(R.string.video_category_title),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text = stringResource(R.string.category_change_filter_tip),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    LaunchedEffect(pagingItems.itemCount) {
                        if (pagingItems.itemCount == 0) {
                            titleFocusRequester.requestFocus()
                        }
                    }
                }
                items(count = pagingItems.itemCount) { videoIndex ->
                    val video = pagingItems[videoIndex]!!
                    Box(
                        modifier = Modifier.size(videoCardContainerWidth, videoCardContainerHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoCard(width = Constants.VideoCardWidth,
                            height = Constants.VideoCardHeight,
                            video = video,
                            onVideoClick = {
                                DetailActivity.startActivity(it.id, context)
                            },
                            onVideoLongClick = {
                                showFilterDialog = true
                            })
                    }
                }

                if (refreshState is LoadState.Error) {
                    item(span = { TvGridItemSpan(maxLineSpan) }) {
                        ErrorTip(message = "加载失败:${refreshState.error.message}") {
                            pagingItems.retry()
                        }
                    }
                } else if (pagingItems.itemCount == 0 && refreshState != LoadState.Loading) {
                    item(span = { TvGridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.grid_no_data_tip),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            })
        if (refreshState == LoadState.Loading) {
            Loading()
        }

    }

    val filters by viewModel.availableFilters.collectAsState()
    AnimatedVisibility(
        visible = showFilterDialog, enter = fadeIn(), exit = fadeOut()
    ) {
        VideoFilterDialog(filters = filters, currentFilter = viewModel.paramArray, onApply = {
            viewModel.applyNewFilter(it)
            pagingItems.refresh()
            coroutineScope.launch {
                state.scrollToItem(0)
                titleFocusRequester.requestFocus()
            }
            showFilterDialog = false
        }) {
            showFilterDialog = false
        }
    }
}

@OptIn(
    ExperimentalTvFoundationApi::class
)
@Composable
fun VideoFilterDialog(
    filters: Resource<VideoFilter>,
    currentFilter: List<String>,
    onApply: (List<String>) -> Unit,
    onCancel: () -> Unit
) {
    val newFilters = remember(currentFilter) {
        mutableStateListOf(*(currentFilter.toTypedArray()))
    }
    val closeDialog = {
        if (currentFilter == newFilters) {
            onCancel()
        } else {
            onApply(newFilters.toList())
        }
    }
    AlertDialog(onDismissRequest = closeDialog,
        confirmButton = {},
        modifier = Modifier.fillMaxWidth(0.8f),
        title = {
            Text(
                text = stringResource(id = R.string.video_filter_dialog_title),
                style = MaterialTheme.typography.headlineMedium
            )
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        text = {

            if (filters is Resource.Loading) {
                Loading()
                return@AlertDialog
            }
            if (filters is Resource.Error) {
                ErrorTip(message = filters.message)
                return@AlertDialog
            }

            val filtersData = (filters as Resource.Success<VideoFilter>).data
            val focusRequester = remember {
                FocusRequester()
            }
            TvLazyColumn(content = {
                items(filtersData.size) { groupIndex ->
                    val group = filtersData[groupIndex]
                    val groupValue = newFilters[group.first]
                    val rowState = rememberTvLazyListState()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = group.second + ':')
                        Spacer(modifier = Modifier.width(20.dp))
                        FocusGroup {
                            TvLazyRow(
                                content = {
                                    items(group.third.size) { optionIndex ->
                                        val option = group.third[optionIndex]
                                        val selected = option.second == groupValue
                                        val modifier =
                                            if (groupIndex == 0 && selected) Modifier.focusRequester(
                                                focusRequester
                                            )
                                            else Modifier
                                        FocusableFilterChip(
                                            text = option.first,
                                            selected = selected,
                                            modifier = modifier.restorableFocus()
                                        ) {
                                            newFilters[group.first] = option.second
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = spacedBy(8.dp),
                                state = rowState
                            )
                        }
                    }
                    LaunchedEffect(Unit) {
                        launch {
                            val idx = group.third.indexOfFirst { it.second == groupValue }
                            if (idx > 0) {
                                try {
                                    rowState.scrollToItem(idx)
                                } catch (ex: Exception) {
                                    if (ex is CancellationException) {
                                        throw ex
                                    }
                                    Log.e(
                                        "CategoriesScreen",
                                        "VideoFilterDialog: scroll to row $groupIndex item $idx",
                                    )
                                }
                            }
                            if (groupIndex == 0) {
                                try {
                                    focusRequester.requestFocus()
                                } catch (ex: Exception) {
                                    Log.e(
                                        "CategoriesScreen",
                                        "first cell request focus error:${ex.message}",
                                        ex
                                    )
                                }
                            }
                        }

                    }

                }
            }, verticalArrangement = spacedBy(8.dp), modifier = Modifier.onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    closeDialog()
                    true
                } else {
                    false
                }
            })

        })


}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTvMaterial3Api::class)
@Composable
fun FocusableFilterChip(
    text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}
) {
    var focused by remember {
        mutableStateOf(false)
    }
    FilterChip(selected = selected, onClick = onClick, label = {
        Text(text = text)
    }, border = if (focused) FilterChipDefaults.filterChipBorder(
        borderColor = MaterialTheme.colorScheme.border,
        selectedBorderColor = MaterialTheme.colorScheme.border,
        borderWidth = 2.dp,
        selectedBorderWidth = 2.dp
    ) else FilterChipDefaults.filterChipBorder(
        borderColor = Color.Transparent, selectedBorderColor = Color.Transparent
    ), modifier = modifier.onFocusChanged {
        focused = it.isFocused || it.hasFocus
    })
}

@Preview
@Composable
fun VideoFilterChipPreview() {
    Dy555Theme {
        Row {
            FocusableFilterChip(
                text = "2023", selected = false
            )
            FocusableFilterChip(
                text = "2021", selected = true
            )
        }
    }
}
