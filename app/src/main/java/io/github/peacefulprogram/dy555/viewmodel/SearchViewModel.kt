package io.github.peacefulprogram.dy555.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.github.peacefulprogram.dy555.http.HttpDataRepository
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.room.dao.SearchHistoryDao
import io.github.peacefulprogram.dy555.room.entity.SearchHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(
    private val repository: HttpDataRepository,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _searchRecommend: MutableStateFlow<Resource<List<String>>> =
        MutableStateFlow(Resource.Loading)
    val searchRecommend: StateFlow<Resource<List<String>>>
        get() = _searchRecommend

    val searchHistoryPager = Pager(
        config = PagingConfig(pageSize = 10),
        initialKey = 1
    ) {
        searchHistoryDao.queryPaging()
    }
        .flow
//        .cachedIn(viewModelScope)

    init {
        loadSearchRecommend()
    }

    private fun loadSearchRecommend() {
        viewModelScope.launch(Dispatchers.IO) {
            _searchRecommend.emit(Resource.Loading)
            try {
                _searchRecommend.emit(Resource.Success(repository.querySearchRecommend()))
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                _searchRecommend.emit(Resource.Error("查询失败:${ex.message}", ex))
            }
        }
    }

    suspend fun deleteSearchHistory(history: SearchHistory) {
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteHistory(history)
        }
    }

    suspend fun deleteAllHistory() {
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteAllHistory()
        }
    }

    fun saveHistory(keyword: String) {
        if (keyword.isBlank()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            searchHistoryDao.saveHistory(
                history = SearchHistory(
                    keyword = keyword.trim(),
                    searchTime = System.currentTimeMillis()
                )
            )
        }
    }
}