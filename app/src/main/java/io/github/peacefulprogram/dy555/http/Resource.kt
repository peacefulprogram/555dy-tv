package io.github.peacefulprogram.dy555.http

sealed class Resource<in T> {
    data class Success<T>(val data: T) : Resource<T>()
    object Loading : Resource<Any>()
    data class Error<T>(val message: String, val error: Throwable? = null) : Resource<T>()
}
