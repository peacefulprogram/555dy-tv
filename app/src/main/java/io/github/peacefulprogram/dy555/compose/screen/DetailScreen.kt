package io.github.peacefulprogram.dy555.compose.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CompactCard
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import io.github.peacefulprogram.dy555.Constants
import io.github.peacefulprogram.dy555.compose.common.ErrorTip
import io.github.peacefulprogram.dy555.compose.common.Loading
import io.github.peacefulprogram.dy555.compose.theme.Dy555Theme
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.http.VideoDetailData
import io.github.peacefulprogram.dy555.http.VideoInfoLine
import io.github.peacefulprogram.dy555.http.VideoTag
import io.github.peacefulprogram.dy555.viewmodel.VideoDetailViewModel

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
    val videoDetail = (videoDetailResource as Resource.Success<VideoDetailData>).data
    TvLazyColumn(modifier = Modifier.fillMaxSize(), content = {
        item {
            VideoInfo(videoDetail = videoDetail)
        }
    }, contentPadding = PaddingValues(10.dp))
}

private fun jumpToByTag(url: String, context: Context) {

    if (url.startsWith("/vodshow")) {
        TODO("类型索引页")
    } else if (url.startsWith("/vodsearch")) {
        TODO("搜索页")
    } else {
        Toast.makeText(context, "不支持的url:$url", Toast.LENGTH_LONG).show()
    }

}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoInfo(videoDetail: VideoDetailData) {
    val focusRequester = remember {
        FocusRequester()
    }
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth()) {
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
        Spacer(modifier = Modifier.width(50.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f),
            verticalArrangement = spacedBy(10.dp),
        ) {
            Text(text = videoDetail.title, style = MaterialTheme.typography.titleLarge)
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                if (videoDetail.tags.isNotEmpty()) {
                    TvLazyRow(
                        content = {
                            items(items = videoDetail.tags, key = VideoTag::name) { tag ->
                                VideoTag(tagName = tag.name) {
                                    jumpToByTag(tag.url, context)
                                }
                            }
                        },
                        horizontalArrangement = spacedBy(10.dp),
                        pivotOffsets = PivotOffsets(parentFraction = 0.5f)
                    )
                }
                videoDetail.infoLines.forEach { infoLine ->
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
                                    items(items = infoLine.tags, key = VideoTag::name) { tag ->
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
            }

        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoTag(tagName: String, onClick: () -> Unit = {}) {
    Button(onClick = onClick) {
        Text(text = tagName)
    }
}

@Preview
@Composable
fun VideoTagPreview() {
    Dy555Theme {
        VideoTag(tagName = "2023")
    }
}