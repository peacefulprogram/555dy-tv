package io.github.peacefulprogram.dy555.room

data class VideoEpisodeHistory(
    val videoId: String,
    val epId: String,
    val title: String,
    val pic: String,
    val epName: String,
    val progress: Long,
    val duration: Long
)