package io.github.peacefulprogram.dy555.compose.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyListState
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.CompactCard
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import io.github.peacefulprogram.dy555.Constants
import io.github.peacefulprogram.dy555.R
import io.github.peacefulprogram.dy555.activity.CategoriesActivity
import io.github.peacefulprogram.dy555.activity.DetailActivity
import io.github.peacefulprogram.dy555.activity.PlaybackActivity
import io.github.peacefulprogram.dy555.activity.SearchResultActivity
import io.github.peacefulprogram.dy555.compose.common.ErrorTip
import io.github.peacefulprogram.dy555.compose.common.Loading
import io.github.peacefulprogram.dy555.compose.common.VideoCard
import io.github.peacefulprogram.dy555.compose.theme.Dy555Theme
import io.github.peacefulprogram.dy555.compose.util.FocusGroup
import io.github.peacefulprogram.dy555.ext.secondsToDuration
import io.github.peacefulprogram.dy555.http.Episode
import io.github.peacefulprogram.dy555.http.MediaCardData
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.http.VideoDetailData
import io.github.peacefulprogram.dy555.http.VideoInfoLine
import io.github.peacefulprogram.dy555.http.VideoTag
import io.github.peacefulprogram.dy555.room.entity.VideoHistory
import io.github.peacefulprogram.dy555.viewmodel.VideoDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(viewModel: VideoDetailViewModel) {
    val videoDetailResource = viewModel.videoDetail.collectAsState().value
    if (videoDetailResource == Resource.Loading) {
        Loading()
        return
    }
    if (videoDetailResource is Resource.Error) {
        ErrorTip(message = videoDetailResource.message) {
            viewModel.reloadVideoDetail()
        }
        return
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.fetchHistory()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val videoDetail = (videoDetailResource as Resource.Success<VideoDetailData>).data
    val context = LocalContext.current
    var reverseEpisode by remember {
        mutableStateOf(false)
    }

    val playlists = remember(reverseEpisode) {
        if (reverseEpisode) {
            videoDetail.playLists.map { it.copy(second = it.second.reversed()) }
        } else {
            videoDetail.playLists
        }
    }
    TvLazyColumn(
        modifier = Modifier.fillMaxSize(), content = {
            item {
                VideoInfoRow(videoDetail = videoDetail, viewModel = viewModel)
            }
            items(count = playlists.size, key = { playlists[it].first }) { playlistIndex ->
                val playlist = playlists[playlistIndex]
                val listState = rememberTvLazyListState()
                PlayListRow(episodes = playlist.second, listState = listState, title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = playlist.first, style = MaterialTheme.typography.titleMedium
                        )
                        if (playlistIndex == 0) {
                            Text(text = " | ")
                            Surface(
                                onClick = {
                                    reverseEpisode = !reverseEpisode
                                },
                                scale = ClickableSurfaceScale.None,
                                colors = ClickableSurfaceDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                                border = ClickableSurfaceDefaults.border(
                                    focusedBorder = Border(
                                        BorderStroke(
                                            2.dp, MaterialTheme.colorScheme.border
                                        )
                                    )
                                ),
                            ) {
                                Text(
                                    text = if (reverseEpisode) "倒序" else "正序",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(8.dp, 4.dp)
                                )
                            }
                        }
                    }
                }) {
                    viewModel.saveVideoHistory(
                        VideoHistory(
                            id = videoDetail.id, pic = videoDetail.pic, title = videoDetail.title
                        )
                    )
                    PlaybackActivity.startActivity(
                        videoId = videoDetail.id,
                        episode = it,
                        videoName = videoDetail.title,
                        context = context,
                        playlist = videoDetail.playLists[playlistIndex].second // 从原始数据中取未被排序的
                    )
                }
                LaunchedEffect(reverseEpisode) {
                    listState.scrollToItem(0)
                }
            }
            item {
                RelativeVideoRow(videoDetail.relatedVideos)
            }
        }, verticalArrangement = spacedBy(10.dp)
    )
}

@Composable
fun RelativeVideoRow(videos: List<MediaCardData>) {
    if (videos.isEmpty()) {
        return
    }
    val context = LocalContext.current
    Column {
        Text(text = stringResource(id = R.string.related_videos))
        Spacer(modifier = Modifier.height(5.dp))
        TvLazyRow(
            content = {
                items(items = videos, key = { it.id }) { video ->
                    VideoCard(
                        width = Constants.VideoCardWidth * 0.8f,
                        height = Constants.VideoCardHeight * 0.8f,
                        video = video,
                        onVideoClick = {
                            DetailActivity.startActivity(video.id, context)
                        }
                    )
                }
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 15.dp)
        )
    }
}

@OptIn(ExperimentalTvFoundationApi::class)
@Composable
fun PlayListRow(
    episodes: List<Episode>,
    title: @Composable () -> Unit,
    listState: TvLazyListState = rememberTvLazyListState(),
    onEpisodeClick: (Episode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        title()
        Spacer(modifier = Modifier.height(5.dp))
        FocusGroup {
            TvLazyRow(
                state = listState, content = {
                    items(items = episodes, key = { it.id }) { ep ->
                        VideoTag(
                            modifier = Modifier.restorableFocus(), tagName = ep.name
                        ) {
                            onEpisodeClick(ep)
                        }
                    }
                }, horizontalArrangement = spacedBy(5.dp)
            )
        }
    }
}

private fun jumpToByTag(url: String, context: Context) {

    if (url.startsWith("/vodshow")) {
        CategoriesActivity.startActivity(
            url.substring(
                url.lastIndexOf('/') + 1,
                url.lastIndexOf('.')
            ),
            context
        )
    } else if (url.startsWith("/vodsearch")) {
        SearchResultActivity.startActivity(
            context, url.substring(
                url.lastIndexOf('/') + 1, url.lastIndexOf('.')
            )
        )
    } else {
        Toast.makeText(context, "不支持的url:$url", Toast.LENGTH_LONG).show()
    }

}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoInfoRow(videoDetail: VideoDetailData, viewModel: VideoDetailViewModel) {
    val focusRequester = remember {
        FocusRequester()
    }
    var showDescDialog by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current

    Row(
        Modifier
            .fillMaxWidth()
            .height(Constants.VideoCardHeight * 1.3f + 10.dp)
    ) {
        CompactCard(
            onClick = {},
            image = {
                AsyncImage(
                    model = videoDetail.pic,
                    contentDescription = videoDetail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            },
            title = {},
            scale = CardDefaults.scale(focusedScale = 1f),
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(Constants.VideoCardWidth * 1.3f, Constants.VideoCardHeight * 1.3f)
        )
        Spacer(modifier = Modifier.width(15.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = spacedBy(10.dp),
        ) {
            Text(
                text = videoDetail.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            val playHistory = viewModel.latestProgress.collectAsState().value
            if (playHistory is Resource.Success) {
                val his = playHistory.data
                Text(
                    text = "上次播放到${his.name} ${(his.progress / 1000).secondsToDuration()}/${(his.duration / 1000).secondsToDuration()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                TvLazyColumn(
                    content = {
                        if (videoDetail.tags.isNotEmpty()) {
                            item {
                                TvLazyRow(
                                    content = {
                                        items(
                                            items = videoDetail.tags,
                                            key = VideoTag::url
                                        ) { tag ->
                                            VideoTag(tagName = tag.name) {
                                                jumpToByTag(tag.url, context)
                                            }
                                        }
                                    },
                                    horizontalArrangement = spacedBy(5.dp),
                                    pivotOffsets = PivotOffsets(parentFraction = 0.5f)
                                )
                            }
                        }
                        items(items = videoDetail.infoLines) { infoLine ->
                            if (infoLine is VideoInfoLine.PlainTextInfo) {
                                Text(text = "${infoLine.name} ${infoLine.value}")
                            }
                            if (infoLine is VideoInfoLine.TagInfo) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = infoLine.name)
                                    TvLazyRow(
                                        content = {
                                            items(
                                                items = infoLine.tags,
                                                key = VideoTag::url
                                            ) { tag ->
                                                VideoTag(tagName = tag.name) {
                                                    jumpToByTag(tag.url, context)
                                                }
                                            }
                                        },
                                        horizontalArrangement = spacedBy(10.dp)
                                    )
                                }
                            }
                        }
                        item {
                            Surface(
                                onClick = { showDescDialog = true },
                                scale = ClickableSurfaceScale.None,
                                colors = ClickableSurfaceDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = ClickableSurfaceDefaults.border(
                                    focusedBorder = Border(
                                        BorderStroke(
                                            2.dp,
                                            MaterialTheme.colorScheme.border
                                        )
                                    )
                                ),
                                shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.extraSmall)
                            ) {
                                Text(
                                    text = videoDetail.desc,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = 3.dp
                                    )
                                )
                            }
                        }
                    },
                    verticalArrangement = spacedBy(10.dp)
                )

            }

        }
    }

    // 在Dialog中显示视频简介
    AnimatedVisibility(visible = showDescDialog) {
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val longDescFocusRequester = remember {
            FocusRequester()
        }
        AlertDialog(
            onDismissRequest = { showDescDialog = false },
            confirmButton = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.6f),
            title = {
                Text(
                    text = stringResource(R.string.video_description),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            },
            text = {
                Text(
                    text = videoDetail.desc,
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .focusRequester(longDescFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent {
                            val step = 70f
                            when (it.key) {
                                Key.DirectionUp -> {
                                    if (it.type == KeyEventType.KeyDown) {
                                        coroutineScope.launch {
                                            scrollState.animateScrollBy(-step)
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }

                                Key.DirectionDown -> {
                                    if (it.type == KeyEventType.KeyDown) {
                                        coroutineScope.launch {
                                            scrollState.animateScrollBy(step)
                                        }
                                        true
                                    } else {
                                        false
                                    }

                                }

                                else -> false
                            }

                        }
                )
                LaunchedEffect(Unit) {
                    longDescFocusRequester.requestFocus()
                }
            }

        )

    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoTag(tagName: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Surface(
        modifier = modifier,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.2f),
            focusedContainerColor = Color.White.copy(alpha = 0.2f),
            pressedContainerColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.border
                ),
                shape = MaterialTheme.shapes.small
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        onClick = onClick
    ) {
        Text(
            modifier = Modifier.padding(6.dp, 3.dp),
            text = tagName,
            color = Color.White
        )
    }

}

@Preview
@Composable
fun VideoTagPreview() {
    Dy555Theme {
        VideoTag(tagName = "2023")
    }
}