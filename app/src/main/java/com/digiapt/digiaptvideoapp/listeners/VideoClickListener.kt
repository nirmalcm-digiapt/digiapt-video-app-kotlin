package com.digiapt.digiaptvideoapp.listeners

import android.view.View
import com.digiapt.digiaptvideoapp.models.Video

interface VideoClickListener {
    fun onVideoItemClick(view: View, video: Video)
}