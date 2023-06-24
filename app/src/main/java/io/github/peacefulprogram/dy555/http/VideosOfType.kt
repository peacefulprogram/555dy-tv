package io.github.peacefulprogram.dy555.http

data class VideosOfType(
    val recommendVideos: List<MediaCardData>,
    val ranks: List<Pair<String, List<MediaCardData>>>,
    val videoGroups: List<Pair<String, List<MediaCardData>>>
)