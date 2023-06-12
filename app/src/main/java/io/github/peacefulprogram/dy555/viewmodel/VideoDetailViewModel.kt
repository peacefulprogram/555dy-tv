package io.github.peacefulprogram.dy555.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peacefulprogram.dy555.http.HttpDataRepository
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.http.VideoDetailData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VideoDetailViewModel(
    val videoId: String,
    private val repository: HttpDataRepository
) : ViewModel() {

    private val TAG = VideoDetailViewModel::class.java.simpleName

    private val _videoDetail: MutableStateFlow<Resource<VideoDetailData>> =
        MutableStateFlow(Resource.Loading)

    val videoDetail: StateFlow<Resource<VideoDetailData>>
        get() = _videoDetail

    init {
        reloadVideoDetail()
    }

    fun reloadVideoDetail() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _videoDetail.emit(Resource.Loading)
                _videoDetail.emit(Resource.Success(repository.getDetailPage(videoId)))
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                Log.e(TAG, "reloadVideoDetail: ${ex.message}", ex)
                _videoDetail.emit(Resource.Error("加载失败:${ex.message}", ex))
            }
        }
    }
}