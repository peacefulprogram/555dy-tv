package io.github.peacefulprogram.dy555.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import io.github.peacefulprogram.dy555.R
import io.github.peacefulprogram.dy555.fragment.VideoPlaybackFragment
import io.github.peacefulprogram.dy555.http.Episode
import io.github.peacefulprogram.dy555.viewmodel.PlaybackViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlaybackActivity : FragmentActivity() {

    private val viewModel by viewModel<PlaybackViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
        val videoId = intent.getStringExtra("vid")!!
        val episodeId = intent.getStringExtra("id")!!
        val episodeName = intent.getStringExtra("name")!!
        val playlist = intent.getSerializableExtra("playlist") as Array<Episode>
        viewModel.videoId = videoId
        viewModel.videoTitle = intent.getStringExtra("title")!!
        viewModel.changeEpisode(Episode(id = episodeId, name = episodeName))
        viewModel.playlist = playlist.toList()
        supportFragmentManager.beginTransaction()
            .replace(R.id.playback_fragment, VideoPlaybackFragment(viewModel)).commit()

    }

    companion object {
        fun startActivity(
            videoId: String,
            episode: Episode,
            playlist: List<Episode>,
            videoName: String,
            context: Context
        ) {
            Intent(context, PlaybackActivity::class.java).apply {
                putExtra("vid", videoId)
                putExtra("id", episode.id)
                putExtra("name", episode.name)
                putExtra("playlist", playlist.toTypedArray())
                putExtra("title", videoName)
                context.startActivity(this)
            }
        }
    }


    override fun onStart() {

        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}