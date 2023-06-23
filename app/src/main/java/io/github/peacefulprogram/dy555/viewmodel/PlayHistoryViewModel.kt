package io.github.peacefulprogram.dy555.viewmodel

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.github.peacefulprogram.dy555.room.dao.VideoHistoryDao

class PlayHistoryViewModel(
    private val videoHistoryDao: VideoHistoryDao
) : ViewModel() {


    val pager = Pager(
        config = PagingConfig(20)
    ) {
        videoHistoryDao.queryHistory()
    }
        .flow

}