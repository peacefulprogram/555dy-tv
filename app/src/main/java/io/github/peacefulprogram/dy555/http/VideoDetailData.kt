package io.github.peacefulprogram.dy555.http

import java.io.Serializable

data class VideoDetailData(
    val id: String,
    val title: String,
    val desc: String,
    val pic: String,
    val playLists: List<Pair<String, List<Episode>>>,
    val relatedVideos: List<MediaCardData> = emptyList(),
    val tags: List<VideoTag>,
    val infoLines: List<VideoInfoLine>
)

data class Episode(
    val id: String,
    val name: String
) : Serializable

data class VideoTag(
    val name: String,
    val url: String
)

sealed class VideoInfoLine {
    class PlainTextInfo(
        val name: String,
        val value: String
    ) : VideoInfoLine()

    class TagInfo(
        val name: String,
        val tags: List<VideoTag>
    ) : VideoInfoLine()
}