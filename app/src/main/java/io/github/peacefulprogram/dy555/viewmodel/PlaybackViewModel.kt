package io.github.peacefulprogram.dy555.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peacefulprogram.dy555.http.Episode
import io.github.peacefulprogram.dy555.http.HttpDataRepository
import io.github.peacefulprogram.dy555.http.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PlaybackViewModel(
    private val repository: HttpDataRepository
) : ViewModel() {

    lateinit var playlist: List<Episode>
    lateinit var videoTitle: String

    private val videoUrlCache: MutableMap<String, String> = ConcurrentHashMap()

    private val _playbackEpisode: MutableStateFlow<Resource<PlaybackEpisode>> =
        MutableStateFlow(Resource.Loading)

    val playbackEpisode: StateFlow<Resource<PlaybackEpisode>>
        get() = _playbackEpisode

    lateinit var episode: Episode
        private set

    private var lastJob: Job? = null

    fun changeEpisode(episode: Episode) {
        this.episode = episode
        cancelLastJob()
        val cacheUrl = videoUrlCache[episode.id]
        if (cacheUrl != null) {
            viewModelScope.launch {
                _playbackEpisode.emit(
                    Resource.Success(
                        PlaybackEpisode(
                            id = episode.id,
                            name = episode.name,
                            url = cacheUrl
                        )
                    )
                )
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _playbackEpisode.emit(Resource.Loading)
            try {
                val url = repository.queryVideoUrl(episode.id)
                if (!isActive) {
                    return@launch
                }
                if (url == null) {
                    _playbackEpisode.emit(Resource.Error("获取视频链接失败"))
                } else {
                    _playbackEpisode.emit(
                        Resource.Success(
                            PlaybackEpisode(
                                id = episode.id,
                                name = episode.name,
                                url = url
                            )
                        )
                    )
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                _playbackEpisode.emit(Resource.Error("获取视频链接失败:${ex.message}", ex))
            }
        }
    }

    fun cancelLastJob() {
        lastJob?.cancel()
        lastJob = null
    }

    fun reloadEpisode() {
        changeEpisode(episode)
    }
}

data class PlaybackEpisode(
    val id: String,
    val name: String,
    val url: String
)
