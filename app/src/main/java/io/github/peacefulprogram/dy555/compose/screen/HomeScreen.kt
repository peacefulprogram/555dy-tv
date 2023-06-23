package io.github.peacefulprogram.dy555.compose.screen

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
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
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import io.github.peacefulprogram.dy555.Constants.VideoCardHeight
import io.github.peacefulprogram.dy555.Constants.VideoCardWidth
import io.github.peacefulprogram.dy555.R
import io.github.peacefulprogram.dy555.activity.CategoriesActivity
import io.github.peacefulprogram.dy555.activity.DetailActivity
import io.github.peacefulprogram.dy555.activity.PlayHistoryActivity
import io.github.peacefulprogram.dy555.activity.SearchActivity
import io.github.peacefulprogram.dy555.compose.common.ErrorTip
import io.github.peacefulprogram.dy555.compose.common.Loading
import io.github.peacefulprogram.dy555.compose.common.VideoCard
import io.github.peacefulprogram.dy555.compose.util.FocusGroup
import io.github.peacefulprogram.dy555.http.MediaCardData
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val TabItems = HomeNavTabItem.values()

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
    var selectedTabIndex by remember {
        mutableIntStateOf(0)
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedTabIndex) {
        val tab = TabItems[selectedTabIndex]
        coroutineScope.launch(Dispatchers.Default) {
            delay(200L)
            when (tab) {
                HomeNavTabItem.RECOMMEND -> viewModel.refreshRecommend(true)
                HomeNavTabItem.NETFLIX -> {}
                HomeNavTabItem.MOVIE -> viewModel.refreshMovies(true)
                HomeNavTabItem.ANIME -> viewModel.refreshAnime(true)
                HomeNavTabItem.SERIAL_DRAMA -> viewModel.refreshSerialDrama(true)
                HomeNavTabItem.VARIETY_SHOW -> viewModel.refreshVarietyShow(true)
            }
        }
    }
    HomeTopNav(
        selectedTabIndex = selectedTabIndex,
        onTabFocus = { selectedTabIndex = it },
        tabItems = TabItems,
        modifier = Modifier.focusRequester(tabFocusRequester)
    ) { tab ->
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
                        videoTypeId = "4",
                        dataProvider = viewModel::anime,
                        onRequestRefresh = viewModel::refreshAnime,
                        onRequestTabFocus = tabFocusRequester::requestFocus,
                        onVideoClick = navigateToDetail,
                    )

                    HomeNavTabItem.SERIAL_DRAMA -> VideoCategories(
                        videoTypeId = "2",
                        dataProvider = viewModel::serialDrama,
                        onRequestRefresh = viewModel::refreshSerialDrama,
                        onRequestTabFocus = tabFocusRequester::requestFocus,
                        onVideoClick = navigateToDetail
                    )

                    HomeNavTabItem.VARIETY_SHOW -> VideoCategories(
                        videoTypeId = "3",
                        dataProvider = viewModel::varietyShow,
                        onRequestRefresh = viewModel::refreshVarietyShow,
                        onRequestTabFocus = tabFocusRequester::requestFocus,
                        onVideoClick = navigateToDetail
                    )

                    HomeNavTabItem.MOVIE -> VideoCategories(
                        videoTypeId = "1",
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
    selectedTabIndex: Int,
    onTabFocus: (Int) -> Unit,
    tabItems: Array<HomeNavTabItem>,
    modifier: Modifier = Modifier,
    tabContent: @Composable (HomeNavTabItem) -> Unit
) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.fillMaxSize()) {
            FocusGroup {
                Row(horizontalArrangement = spacedBy(10.dp)) {
                    IconButton(
                        onClick = {
                            SearchActivity.startActivity(context)
                        },
                        modifier = Modifier.restorableFocus()
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "search")
                    }
                    IconButton(
                        onClick = {
                            PlayHistoryActivity.startActivity(context)
                        },
                        modifier = Modifier.restorableFocus()
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "history")
                    }

                }

            }

            FocusGroup(modifier = modifier) {
                Row(Modifier.fillMaxWidth()) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        indicator = {
                            TabRowDefaults.UnderlinedIndicator(
                                currentTabPosition = it[selectedTabIndex],
                                activeColor = MaterialTheme.colorScheme.border
                            )
                        }
                    ) {
                        tabItems.forEachIndexed { tabIndex, tab ->
                            Tab(
                                selected = selectedTabIndex == tabIndex,
                                modifier = Modifier.restorableFocus(),
                                onFocus = { onTabFocus(tabIndex) },
                                colors = TabDefaults.underlinedIndicatorTabColors(
                                    selectedContentColor = colorResource(id = R.color.rose200),
                                    focusedSelectedContentColor = colorResource(id = R.color.rose400)
                                )
                            ) {
                                Text(
                                    text = stringResource(tab.tabName),
                                    modifier = Modifier.padding(8.dp, 4.dp),
                                )
                            }
                        }
                    }
                }

            }
            tabContent(tabItems[selectedTabIndex])
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )
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
    videoTypeId: String? = null,
    dataProvider: () -> StateFlow<Resource<List<Pair<String, List<MediaCardData>>>>>,
    onRequestRefresh: (autoRefresh: Boolean) -> Unit,
    onRequestTabFocus: () -> Unit,
    onVideoClick: (MediaCardData) -> Unit
) {
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
    val context = LocalContext.current
    TvLazyColumn(
        content = {
            item { Spacer(modifier = Modifier.height(20.dp)) }
            items(count = videoGroups.size, key = { videoGroups[it].first }) { groupIndex ->
                val typeId = if (groupIndex == 0) {
                    videoTypeId
                } else {
                    null
                }
                val onVideoTypeClick = {
                    if (videoTypeId != null) {
                        CategoriesActivity.startActivity(
                            "$videoTypeId-----------", context = context
                        )
                    }
                }
                val group = videoGroups[groupIndex]
                VideoRow(
                    videoTypeId = typeId,
                    onVideoTypeClick = onVideoTypeClick,
                    title = group.first,
                    videos = group.second,
                    onVideoClick = onVideoClick
                ) { _, keyEvent ->
                    when (keyEvent.key) {
                        Key.Back -> {
                            coroutineScope.launch {
                                state.scrollToItem(0)
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

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvFoundationApi::class)
@Composable
fun VideoRow(
    title: String,
    videos: List<MediaCardData>,
    videoTypeId: String? = null,
    onVideoTypeClick: (() -> Unit)? = null,
    onVideoClick: (MediaCardData) -> Unit = {},
    onVideoKeyEvent: ((MediaCardData, KeyEvent) -> Boolean)? = null
) {
    val focusedScale = 1.1f
    val scaleWidth = VideoCardWidth * (focusedScale - 1f) / 2 + 5.dp
    val scaleHeight = VideoCardHeight * (focusedScale - 1f) / 2 + 5.dp
    Column(Modifier.fillMaxWidth()) {
        if (videoTypeId == null) {
            Text(text = title)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title)
                Text(text = " | ")
                Surface(
                    onClick = { onVideoTypeClick?.invoke() },
                    scale = ClickableSurfaceScale.None,
                    colors = ClickableSurfaceDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.border))
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.video_more),
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(scaleHeight))
        FocusGroup {
            TvLazyRow(content = {
                item {
                    Spacer(modifier = Modifier.width(scaleWidth))
                }
                items(items = videos, key = { it.id }) { video ->
                    VideoCard(
                        width = VideoCardWidth,
                        height = VideoCardHeight,
                        video = video,
                        modifier = Modifier.restorableFocus(),
                        focusedScale = focusedScale,
                        onVideoClick = onVideoClick,
                        onVideoKeyEvent = onVideoKeyEvent
                    )
                }
                item {
                    Spacer(modifier = Modifier.width(scaleWidth))
                }
            })
        }
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
    val videoCardContainerWidth = VideoCardWidth * 1.1f
    val videoCardContainerHeight = VideoCardHeight * 1.1f

    val gridState = rememberTvLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    TvLazyVerticalGrid(
        columns = TvGridCells.Adaptive(videoCardContainerWidth),
        state = gridState,
        contentPadding = PaddingValues(20.dp),
        content = {
            items(count = pagingItems.itemCount) {
                Box(
                    modifier = Modifier.size(videoCardContainerWidth, videoCardContainerHeight),
                    contentAlignment = Alignment.Center
                ) {
                    VideoCard(width = VideoCardWidth,
                        height = VideoCardHeight,
                        video = pagingItems[it]!!,
                        onVideoClick = onVideoClick,
                        onVideoKeyEvent = { _, event ->
                            when (event.key) {
                                Key.Back -> {
                                    coroutineScope.launch {
                                        gridState.scrollToItem(0)
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

                        })
                }
            }
        })

}