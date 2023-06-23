package io.github.peacefulprogram.dy555.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peacefulprogram.dy555.http.HttpDataRepository
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.http.VideoDetailData
import io.github.peacefulprogram.dy555.room.dao.EpisodeHistoryDao
import io.github.peacefulprogram.dy555.room.dao.VideoHistoryDao
import io.github.peacefulprogram.dy555.room.entity.EpisodeHistory
import io.github.peacefulprogram.dy555.room.entity.VideoHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VideoDetailViewModel(
    val videoId: String,
    private val repository: HttpDataRepository,
    private val videoHistoryDao: VideoHistoryDao,
    private val episodeHistoryDao: EpisodeHistoryDao
) : ViewModel() {

    private val TAG = VideoDetailViewModel::class.java.simpleName

    private val _videoDetail: MutableStateFlow<Resource<VideoDetailData>> =
        MutableStateFlow(Resource.Loading)

    val videoDetail: StateFlow<Resource<VideoDetailData>>
        get() = _videoDetail

    private val _latestProgress: MutableStateFlow<Resource<EpisodeHistory>> =
        MutableStateFlow(Resource.Loading)


    val latestProgress: StateFlow<Resource<EpisodeHistory>>
        get() = _latestProgress

    init {
        reloadVideoDetail()
    }

    fun fetchHistory() {
        viewModelScope.launch(Dispatchers.Default) {
            episodeHistoryDao.queryLatestProgress(videoId)?.let {
                _latestProgress.emit(Resource.Success(it))
            }
        }
    }

    fun saveVideoHistory(video: VideoHistory) {
        viewModelScope.launch(Dispatchers.IO) {
            videoHistoryDao.saveVideo(video)
        }
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