package io.github.peacefulprogram.dy555.http

data class PageResult<T>(
    val data: List<T>,
    val page: Int,
    val hasNextPage: Boolean
)
