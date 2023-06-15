package io.github.peacefulprogram.dy555.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.graphics.drawable.toDrawable
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.widget.Action
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.leanback.LeanbackPlayerAdapter
import com.jing.ddys.ext.showLongToast
import io.github.peacefulprogram.dy555.Constants
import io.github.peacefulprogram.dy555.ext.dpToPx
import io.github.peacefulprogram.dy555.fragment.playback.ChooseEpisodeDialog
import io.github.peacefulprogram.dy555.fragment.playback.GlueActionCallback
import io.github.peacefulprogram.dy555.fragment.playback.PlayListAction
import io.github.peacefulprogram.dy555.fragment.playback.ProgressTransportControlGlue
import io.github.peacefulprogram.dy555.fragment.playback.ReplayAction
import io.github.peacefulprogram.dy555.http.Resource
import io.github.peacefulprogram.dy555.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VideoPlaybackFragment(
    private val viewModel: PlaybackViewModel
) : VideoSupportFragment() {


    private var exoplayer: ExoPlayer? = null

    private var glue: ProgressTransportControlGlue<LeanbackPlayerAdapter>? = null

    private var resumeFrom = -1L

    private var backPressed = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.background = Color.BLACK.toDrawable()
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playbackEpisode.collectLatest { playbackEpisode ->
                    when (playbackEpisode) {
                        Resource.Loading -> {}
                        is Resource.Success -> {
                            glue?.subtitle = playbackEpisode.data.name
                            exoplayer?.run {
                                setMediaItem(MediaItem.fromUri(playbackEpisode.data.url))
                                prepare()
                                if (resumeFrom > 0) {
                                    seekTo(resumeFrom)
                                    resumeFrom = -1
                                }
                                play()
                            }
                        }

                        is Resource.Error -> requireContext().showLongToast(playbackEpisode.message)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        buildPlayer()
    }

    override fun onStop() {
        super.onStop()
        resumeFrom = exoplayer!!.currentPosition
        destroyPlayer()
    }

    @OptIn(UnstableApi::class)
    private fun buildPlayer() {
        val factory = DefaultHttpDataSource.Factory().apply {
            setUserAgent(Constants.USER_AGENT)
            setDefaultRequestProperties(mapOf("referer" to Constants.BASE_URL))
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(factory)
        exoplayer =
            ExoPlayer.Builder(requireContext()).setMediaSourceFactory(mediaSourceFactory).build()
                .apply {
                    prepareGlue(this)
                    playWhenReady = true
                }

    }


    private fun destroyPlayer() {
        exoplayer?.let {
            // Pause the player to notify listeners before it is released.
            it.pause()
            it.release()
            exoplayer = null
        }
    }

    @OptIn(UnstableApi::class)
    private fun prepareGlue(localExoplayer: ExoPlayer) {
        glue = ProgressTransportControlGlue(context = requireContext(),
            playerAdapter = LeanbackPlayerAdapter(
                requireContext(), localExoplayer, 200
            ),
            onCreatePrimaryAction = {
                it.add(PlayListAction(requireContext()))
                it.add(ReplayAction(requireContext()))
            },
            updateProgress = {}).apply {
            host = VideoSupportFragmentGlueHost(this@VideoPlaybackFragment)
            title = viewModel.videoTitle
            // Enable seek manually since PlaybackTransportControlGlue.getSeekProvider() is null,
            // so that PlayerAdapter.seekTo(long) will be called during user seeking.
            isSeekEnabled = true
            isControlsOverlayAutoHideEnabled = true
            addActionCallback(replayActionCallback)
            addActionCallback(changePlayVideoActionCallback)
            setKeyEventInterceptor { onKeyEvent(it) }
        }
    }


    private val replayActionCallback = object : GlueActionCallback {
        override fun support(action: Action): Boolean = action is ReplayAction

        override fun onAction(action: Action) {
            exoplayer?.seekTo(0L)
            exoplayer?.play()
            hideControlsOverlay(true)
        }

    }

    private val changePlayVideoActionCallback = object : GlueActionCallback {
        override fun support(action: Action): Boolean = action is PlayListAction

        override fun onAction(action: Action) {
            openPlayListDialogAndChoose()
        }

    }


    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
            if (isControlsOverlayVisible) {
                return false
            }
            if (exoplayer?.isPlaying != true) {
                backPressed = false
                return false
            }
            if (backPressed) {
                return false
            }
            backPressed = true
            Toast.makeText(requireContext(), "再按一次退出播放", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                delay(2000)
                backPressed = false
            }
            return true
        }
        if (keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER && !isControlsOverlayVisible) {
            if (exoplayer?.isPlaying == true) {
                exoplayer?.pause()
            } else {
                exoplayer?.play()
            }
            return true
        }

        if (keyEvent.keyCode == KeyEvent.KEYCODE_MENU) {
            openPlayListDialogAndChoose()
            return true
        }
        return false
    }

    private fun openPlayListDialogAndChoose() {
        val fragmentManager = requireActivity().supportFragmentManager
        val current = viewModel.episode
        val defaultSelectIndex = viewModel.playlist.indexOfFirst { it.id == current.id }
        ChooseEpisodeDialog(dataList = viewModel.playlist,
            defaultSelectIndex = defaultSelectIndex,
            viewWidth = 60.dpToPx.toInt(),
            getText = { _, item -> item.name }) { _, ep ->
            viewModel.changeEpisode(ep)
        }.apply {
            showNow(fragmentManager, "")
        }
    }


}