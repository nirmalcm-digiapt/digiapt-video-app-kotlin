package com.digiapt.digiaptvideoapp.network.responses

import com.digiapt.digiaptvideoapp.models.VideoTemp

data class VideoResponseTemp (
    val status: Boolean,
    val message: String,
    val data: VideoTemp
)