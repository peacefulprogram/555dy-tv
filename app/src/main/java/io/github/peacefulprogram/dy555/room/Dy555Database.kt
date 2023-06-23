package io.github.peacefulprogram.dy555.room

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.peacefulprogram.dy555.room.dao.EpisodeHistoryDao
import io.github.peacefulprogram.dy555.room.dao.SearchHistoryDao
import io.github.peacefulprogram.dy555.room.dao.VideoHistoryDao
import io.github.peacefulprogram.dy555.room.entity.EpisodeHistory
import io.github.peacefulprogram.dy555.room.entity.SearchHistory
import io.github.peacefulprogram.dy555.room.entity.VideoHistory

@Database(
    entities = [
        SearchHistory::class,
        VideoHistory::class,
        EpisodeHistory::class
    ], version = 1
)
abstract class Dy555Database : RoomDatabase() {

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun videoHistoryDao(): VideoHistoryDao

    abstract fun episodeHistoryDao(): EpisodeHistoryDao
}