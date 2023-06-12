package io.github.peacefulprogram.dy555.compose.screen

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import io.github.peacefulprogram.dy555.Constants.VideoCardHeight
import io.github.peacefulprogram.dy555.Constants.VideoCardWidth
import io.github.peacefulprogram.dy555.R
import io.github.peacefulprogram.dy555.activity.DetailActivity
import io.github.peacefulprogram.dy555.compose.common.ErrorTip
import io.github.peacefulprogram.dy555.compose.common.Loading
import io.github.peacefulprogram.dy555.compose.common.VideoCard
import io.github.peacefulprogram.dy555.compose.util.FocusGroup
import io.github.peacefulprogram.dy555.http.MediaCardData
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val TabItems = HomeNavTabItem.values()
private const val DefaultSelectedTabIndex = 0

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
) {
    var hasInitTabFocus by rememberSaveable {
        mutableStateOf(false)
    }
    val tabFocusRequester = remember {
        FocusRequester()
    }
    val context = LocalContext.current
    val navigateToDetail = { video: MediaCardData ->
        DetailActivity.startActivity(video.id, context)
    }
    HomeTopNav(modifier = Modifier.focusRequester(tabFocusRequester)) { tab ->
        AnimatedContent(
            targetState = tab,
        ) { curTab ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (curTab) {
                    HomeNavTabItem.RECOMMEND -> VideoCategories(
                        dataProvider = viewModel::recommend,
                        onRequestRefresh = viewModel::refreshRecommend,
                        onRequestTabFocus = tabFocusRequester::requestFocus,
                        onVideoClick = navigateToDetail
                    )

                    HomeNavTabItem.NETFLIX -> NetflixVideos(
                        viewModel = viewModel,
                        onRequestTabFocus = tabFocusRequester::requestFocus,
                        onVideoClick = navigateToDetail
                    )

                    HomeNavTabItem.ANIME -> VideoCategories(
                        dataProvider = viewModel::anime,
                        onRequestRefresh = viewModel::refreshAnime,
                        onRequestTabFocus = tabFocusRequester::requestFocus,
                        onVideoClick = navigateToDetail
                    )

                    HomeNavTabItem.SERIAL_DRAMA -> VideoCategories(
                        dataProvider = viewModel::serialDrama,
                        onRequestRefresh = viewModel::refreshSerialDrama,
                        onRequestTabFocus = tabFocusRequester::requestFocus,
                        onVideoClick = navigateToDetail
                    )

                    HomeNavTabItem.VARIETY_SHOW -> VideoCategories(
                        dataProvider = viewModel::varietyShow,
                        onRequestRefresh = viewModel::refreshVarietyShow,
                        onRequestTabFocus = tabFocusRequester::requestFocus,
                        onVideoClick = navigateToDetail
                    )

                    HomeNavTabItem.MOVIE -> VideoCategories(
                        dataProvider = viewModel::movies,
                        onRequestRefresh = viewModel::refreshMovies,
                        onRequestTabFocus = tabFocusRequester::requestFocus,
                        onVideoClick = navigateToDetail
                    )
                }
            }
        }


    }

    LaunchedEffect(Unit) {
        if (!hasInitTabFocus) {
            hasInitTabFocus = true
            tabFocusRequester.requestFocus()
        }
    }

}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvFoundationApi::class)
@Composable
fun HomeTopNav(
    modifier: Modifier = Modifier,
    tabContent: @Composable (HomeNavTabItem) -> Unit
) {
    val context = LocalContext.current
    val tabItems = TabItems
    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(DefaultSelectedTabIndex)
    }
    Column(Modifier.fillMaxSize()) {
        FocusGroup(modifier = modifier) {
            Row(Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    indicator = {
                        TabRowDefaults.PillIndicator(currentTabPosition = it[selectedTabIndex])
                    }
                ) {
                    tabItems.forEachIndexed { tabIndex, tab ->
                        Tab(
                            selected = selectedTabIndex == tabIndex,
                            modifier = Modifier.restorableFocus(),
                            onFocus = { selectedTabIndex = tabIndex }) {
                            Text(
                                text = context.getString(tab.tabName),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = context.getString(R.string.app_name))
            }

        }
        tabContent(tabItems[selectedTabIndex])
    }


}


enum class HomeNavTabItem(@StringRes val tabName: Int) {
    RECOMMEND(R.string.home_nav_recommend),
    NETFLIX(R.string.home_nav_netflix),
    MOVIE(R.string.home_nav_movie),
    ANIME(R.string.home_nav_anime),
    SERIAL_DRAMA(R.string.home_nav_serial_drama),
    VARIETY_SHOW(R.string.home_nav_variety_show)
}


@Composable
fun VideoCategories(
    dataProvider: () -> StateFlow<Resource<List<Pair<String, List<MediaCardData>>>>>,
    onRequestRefresh: (autoRefresh: Boolean) -> Unit,
    onRequestTabFocus: () -> Unit,
    onVideoClick: (MediaCardData) -> Unit
) {
    LaunchedEffect(Unit) {
        onRequestRefresh(true)
    }
    val recommend by dataProvider().collectAsState()
    if (recommend == Resource.Loading) {
        Loading()
        return
    }
    if (recommend is Resource.Error) {
        ErrorTip(message = (recommend as Resource.Error<List<Pair<String, List<MediaCardData>>>>).message) {
            println("重试重试")
        }
        return
    }
    val videoGroups = (recommend as Resource.Success).data
    val state = rememberTvLazyListState()
    val coroutineScope = rememberCoroutineScope()
    TvLazyColumn(
        content = {
            item { Spacer(modifier = Modifier.height(20.dp)) }
            items(
                items = videoGroups,
                key = { it.first }
            ) {
                VideoRow(
                    title = it.first,
                    videos = it.second,
                    onVideoClick = onVideoClick
                ) { _, keyEvent ->
                    when (keyEvent.key) {
                        Key.Back -> {
                            coroutineScope.launch {
                                state.animateScrollToItem(0)
                            }
                            onRequestTabFocus()
                            true
                        }

                        Key.Menu -> {
                            onRequestRefresh(false)
                            onRequestTabFocus()
                            true
                        }

                        else -> {
                            false
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        },
        state = state
    )
}

@Composable
fun VideoRow(
    title: String,
    videos: List<MediaCardData>,
    onVideoClick: (MediaCardData) -> Unit = {},
    onVideoKeyEvent: ((MediaCardData, KeyEvent) -> Boolean)? = null
) {
    val focusedScale = 1.1f
    val scaleWidth = VideoCardWidth * (focusedScale - 1f) / 2 + 5.dp
    val scaleHeight = VideoCardHeight * (focusedScale - 1f) / 2 + 5.dp
    Column(Modifier.fillMaxWidth()) {
        Text(text = title)
        Spacer(modifier = Modifier.height(scaleHeight))
        TvLazyRow(
            content = {
                item {
                    Spacer(modifier = Modifier.width(scaleWidth))
                }
                items(
                    items = videos,
                    key = { it.id }
                ) { video ->
                    VideoCard(
                        width = VideoCardWidth,
                        height = VideoCardHeight,
                        video = video,
                        focusedScale = focusedScale,
                        onVideoClick = onVideoClick,
                        onVideoKeyEvent = onVideoKeyEvent
                    )
                }
                item {
                    Spacer(modifier = Modifier.width(scaleWidth))
                }
            }
        )
        Spacer(modifier = Modifier.height(scaleHeight))
    }
}

@Composable
fun NetflixVideos(
    viewModel: HomeViewModel,
    onRequestTabFocus: () -> Unit,
    onVideoClick: (MediaCardData) -> Unit
) {
    val pagingItems = viewModel.netflixPager.collectAsLazyPagingItems()
    if (pagingItems.loadState.refresh is LoadState.Loading) {
        Loading()
        return
    }
    if (pagingItems.loadState.refresh is LoadState.Error) {
        val error = (pagingItems.loadState.refresh as LoadState.Error).error
        ErrorTip(message = "加载失败:${error.message}") {
            pagingItems.retry()
        }
        return
    }
    val gridState = rememberTvLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    TvLazyVerticalGrid(
        columns = TvGridCells.Adaptive(VideoCardWidth),
        state = gridState,
        contentPadding = PaddingValues(20.dp),
        content = {
            items(count = pagingItems.itemCount) {
                VideoCard(
                    width = VideoCardWidth,
                    height = VideoCardHeight,
                    video = pagingItems[it]!!,
                    onVideoClick = onVideoClick,
                    onVideoKeyEvent = { _, event ->
                        when (event.key) {
                            Key.Back -> {
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(0)
                                }
                                onRequestTabFocus()
                                true
                            }

                            Key.Menu -> {
                                pagingItems.refresh()
                                onRequestTabFocus()
                                true
                            }

                            else -> {
                                false
                            }
                        }

                    }
                )
            }
        })

}