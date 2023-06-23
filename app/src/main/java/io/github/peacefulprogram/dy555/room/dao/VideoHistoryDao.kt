package io.github.peacefulprogram.dy555.room.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.peacefulprogram.dy555.room.VideoEpisodeHistory
import io.github.peacefulprogram.dy555.room.entity.VideoHistory

@Dao
interface VideoHistoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun saveVideo(video: VideoHistory)

    @Query("update video_history set epId = :episodeId where id = :videoId")
    suspend fun updateLatestPlayedEpisode(videoId: String, episodeId: String)

    @Query(
        """
        select e.videoId,
               v.epId,
               v.title,
               v.pic,
               e.name epName,
               e.progress,
               e.duration
        from video_history v
        inner join episode_history e 
            on v.epId = e.id
        order by e.timestamp desc
    """
    )
    fun queryHistory(): PagingSource<Int, VideoEpisodeHistory>


    @Query("delete from video_history where id = :id")
    suspend fun deleteVideo(id: String)

}