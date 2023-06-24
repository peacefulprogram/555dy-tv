package io.github.peacefulprogram.dy555.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peacefulprogram.dy555.http.Episode
import io.github.peacefulprogram.dy555.http.HttpDataRepository
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.room.dao.EpisodeHistoryDao
import io.github.peacefulprogram.dy555.room.dao.VideoHistoryDao
import io.github.peacefulprogram.dy555.room.entity.EpisodeHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PlaybackViewModel(
    private val repository: HttpDataRepository,
    private val episodeHistoryDao: EpisodeHistoryDao,
    private val videoHistoryDao: VideoHistoryDao
) : ViewModel() {

    lateinit var videoId: String
    lateinit var playlist: List<Episode>
    lateinit var videoTitle: String

    var currentPlayPosition: Long = 0L

    var videoDuration: Long = 0L

    private val videoUrlCache: MutableMap<String, String> = ConcurrentHashMap()

    private val _playbackEpisode: MutableStateFlow<Resource<PlaybackEpisode>> =
        MutableStateFlow(Resource.Loading)

    private var _saveHistoryJob: Job? = null

    val playbackEpisode: StateFlow<Resource<PlaybackEpisode>>
        get() = _playbackEpisode

    var fetchVideoUrlJob: Job? = null

    lateinit var episode: Episode
        private set

    fun changeEpisode(episode: Episode) {
        this.episode = episode
        fetchVideoUrlJob?.cancel()
        fetchVideoUrlJob = null
        val cacheUrl = videoUrlCache[episode.id]
        if (cacheUrl != null) {
            viewModelScope.launch(Dispatchers.Default) {
                val history = episodeHistoryDao.queryHistoryByEpisodeId(episode.id)
                _playbackEpisode.emit(
                    Resource.Success(
                        PlaybackEpisode(
                            id = episode.id,
                            name = episode.name,
                            url = cacheUrl,
                            lastPlayPosition = history?.progress ?: 0L,
                            videoDuration = history?.duration ?: 0L
                        )
                    )
                )
                videoHistoryDao.updateLatestPlayedEpisode(videoId, episode.id)

            }
            return
        }
        fetchVideoUrlJob = viewModelScope.launch(Dispatchers.IO) {
            _playbackEpisode.emit(Resource.Loading)
            try {
                val url = repository.queryVideoUrl(episode.id)
                if (!isActive) {
                    return@launch
                }
                if (url == null) {
                    _playbackEpisode.emit(Resource.Error("获取视频链接失败"))
                } else {
                    val history = episodeHistoryDao.queryHistoryByEpisodeId(episode.id)
                    _playbackEpisode.emit(
                        Resource.Success(
                            PlaybackEpisode(
                                id = episode.id,
                                name = episode.name,
                                url = url,
                                lastPlayPosition = history?.progress ?: 0L,
                                videoDuration = history?.duration ?: 0L
                            )
                        )
                    )
                    videoHistoryDao.updateLatestPlayedEpisode(videoId, episode.id)
                    episodeHistoryDao.save(
                        EpisodeHistory(
                            id = episode.id,
                            videoId = videoId,
                            name = episode.name,
                            progress = history?.progress ?: 0L,
                            duration = history?.duration ?: 0L,
                            timestamp = System.currentTimeMillis()
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

    fun playNextEpisodeIfExists() {
        val idx = playlist.indexOfFirst { it.id == episode.id }
        if (idx < playlist.size - 1) {
            changeEpisode(playlist[idx + 1])
        }
    }

    fun startSaveHistory() {
        stopSaveHistory()
        val epRes = playbackEpisode.value
        if (epRes !is Resource.Success) {
            return
        }
        val ep = epRes.data
        _saveHistoryJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                episodeHistoryDao.save(
                    EpisodeHistory(
                        id = ep.id,
                        videoId = videoId,
                        name = ep.name,
                        progress = currentPlayPosition,
                        duration = videoDuration,
                        timestamp = System.currentTimeMillis()
                    )
                )
                delay(5000L)
            }
        }
    }

    fun saveHistory() {
        val epRes = playbackEpisode.value
        if (epRes !is Resource.Success) {
            return
        }
        val ep = epRes.data
        viewModelScope.launch {
            episodeHistoryDao.save(
                EpisodeHistory(
                    id = ep.id,
                    videoId = videoId,
                    name = ep.name,
                    progress = currentPlayPosition,
                    duration = videoDuration,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun stopSaveHistory() {
        _saveHistoryJob?.cancel()
        _saveHistoryJob = null
    }
}

data class PlaybackEpisode(
    val id: String,
    val name: String,
    val url: String,
    val lastPlayPosition: Long,
    val videoDuration: Long
)
