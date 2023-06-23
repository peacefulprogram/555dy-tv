package io.github.peacefulprogram.dy555.viewmodel

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.github.peacefulprogram.dy555.http.BasicPagingSource
import io.github.peacefulprogram.dy555.http.HttpDataRepository

class SearchResultViewModel(
    searchParam: String,
    private val repository: HttpDataRepository
) : ViewModel() {

    private val searchParam = searchParam.split('-')

    val pager = Pager(
        config = PagingConfig(16)
    ) {
        BasicPagingSource {
            repository.searchVideo(this@SearchResultViewModel.searchParam, it)
        }
    }
        .flow
//        .cachedIn(viewModelScope)

}