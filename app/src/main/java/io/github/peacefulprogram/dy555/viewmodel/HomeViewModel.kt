package io.github.peacefulprogram.dy555.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.github.peacefulprogram.dy555.http.BasicPagingSource
import io.github.peacefulprogram.dy555.http.HttpDataRepository
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.http.VideosOfType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class HomeViewModel(private val repository: HttpDataRepository) : ViewModel() {

    private val refreshTimeMap: MutableMap<String, Long> = ConcurrentHashMap()

    // 首页推荐
    private val _recommend: MutableStateFlow<Resource<VideosOfType>> =
        MutableStateFlow(Resource.Loading)

    val recommend: StateFlow<Resource<VideosOfType>>
        get() = _recommend


    // 电影
    private val _movies: MutableStateFlow<Resource<VideosOfType>> =
        MutableStateFlow(Resource.Loading)

    val movies: StateFlow<Resource<VideosOfType>>
        get() = _movies

    // 连续剧
    private val _serialDrama: MutableStateFlow<Resource<VideosOfType>> =
        MutableStateFlow(Resource.Loading)

    val serialDrama: StateFlow<Resource<VideosOfType>>
        get() = _serialDrama

    // 动漫
    private val _anime: MutableStateFlow<Resource<VideosOfType>> =
        MutableStateFlow(Resource.Loading)

    val anime: StateFlow<Resource<VideosOfType>>
        get() = _anime

    // 综艺
    private val _varietyShow: MutableStateFlow<Resource<VideosOfType>> =
        MutableStateFlow(Resource.Loading)

    val varietyShow: StateFlow<Resource<VideosOfType>>
        get() = _varietyShow

    val netflixPager = Pager(
        config = PagingConfig(16)
    ) {
        BasicPagingSource(repository::getNetflix)
    }
        .flow
//        .cachedIn(viewModelScope)

    fun refreshRecommend(autoRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            requestWithRateLimit(
                ignoreLimit = !autoRefresh,
                key = "recommend",
                state = _recommend
            ) {
                repository.getHomePage()
            }
        }
    }

    fun refreshMovies(autoRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            requestWithRateLimit(
                ignoreLimit = !autoRefresh,
                key = "movie",
                state = _movies
            ) {
                repository.getVideoPageByType(1)
            }
        }
    }

    fun refreshSerialDrama(autoRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            requestWithRateLimit(
                ignoreLimit = !autoRefresh,
                key = "serialDrama",
                state = _serialDrama
            ) {
                repository.getVideoPageByType(2)
            }
        }
    }

    fun refreshAnime(autoRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            requestWithRateLimit(
                ignoreLimit = !autoRefresh,
                key = "anime",
                state = _anime
            ) {
                repository.getVideoPageByType(4)
            }
        }
    }

    fun refreshVarietyShow(autoRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            requestWithRateLimit(
                ignoreLimit = !autoRefresh,
                key = "varietyShow",
                state = _varietyShow
            ) {
                repository.getVideoPageByType(3)
            }
        }
    }

    private suspend fun <T : Any> requestWithRateLimit(
        ignoreLimit: Boolean,
        key: String,
        state: MutableStateFlow<Resource<T>>,
        limit: Long = 120_000L,
        requestData: suspend () -> T,
    ) {
        if (!ignoreLimit) {
            val lastRefresh = refreshTimeMap[key]
            if (lastRefresh != null && System.currentTimeMillis() - lastRefresh < limit) {
                return
            }
        }
        state.emit(Resource.Loading)
        try {
            state.emit(Resource.Success(requestData()))
            refreshTimeMap[key] = System.currentTimeMillis()
        } catch (ex: Exception) {
            if (ex is CancellationException) {
                throw ex
            }
            state.emit(Resource.Error("获取数据失败:${ex.message}", ex))
        }

    }

}