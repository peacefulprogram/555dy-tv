package io.github.peacefulprogram.dy555.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.github.peacefulprogram.dy555.http.BasicPagingSource
import io.github.peacefulprogram.dy555.http.HttpDataRepository
import io.github.peacefulprogram.dy555.http.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: HttpDataRepository,
    defaultParam: String
) : ViewModel() {

    private val defaultParamArray = defaultParam.split('-').run {
        // 第三个参数是排序, 默认按时间排序
        if (this[2].isEmpty()) {
            val mutableList = this.toMutableList()
            mutableList[2] = "time"
            mutableList.toList()
        } else {
            this
        }
    }

    var paramArray = defaultParamArray

    private val _availableFilters =
        MutableStateFlow<Resource<List<Triple<Int, String, List<Pair<String, String>>>>>>(Resource.Loading)

    val availableFilters: StateFlow<Resource<List<Triple<Int, String, List<Pair<String, String>>>>>>
        get() = _availableFilters


    val pager = Pager(
        config = PagingConfig(
            pageSize = 40
        )
    ) {
        BasicPagingSource { page ->
            repository.queryCategory(
                paramArray,
                page
            )
        }
    }
        .flow
//        .cachedIn(viewModelScope)

    init {
        queryFilters()
    }

    fun resetFilter() {
        this.paramArray = defaultParamArray
    }

    fun applyNewFilter(param: List<String>) {
        this.paramArray = param
    }

    fun queryFilters() {
        viewModelScope.launch(Dispatchers.IO) {
            _availableFilters.emit(Resource.Loading)
            try {
                _availableFilters.emit(
                    Resource.Success(
                        repository.queryCategoryFilter(
                            defaultParamArray
                        )
                    )
                )
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                _availableFilters.emit(Resource.Error("查询筛选条件错误:${ex.message}", ex))
            }
        }
    }


}