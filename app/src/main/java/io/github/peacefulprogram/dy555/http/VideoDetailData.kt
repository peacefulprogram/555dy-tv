package io.github.peacefulprogram.dy555.http

data class VideoDetailData(
    val id: String,
    val title: String,
    val desc: String,
    val pic: String,
    val playLists: List<Pair<String, List<Episode>>>,
    val relatedVideos: List<MediaCardData> = emptyList()
)

data class Episode(
    val id: String,
    val name: String
)