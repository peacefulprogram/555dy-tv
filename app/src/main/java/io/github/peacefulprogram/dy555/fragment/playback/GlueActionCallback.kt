package io.github.peacefulprogram.dy555.fragment.playback

import androidx.leanback.widget.Action

interface GlueActionCallback {
    fun support(action: Action): Boolean

    fun onAction(action: Action)
}