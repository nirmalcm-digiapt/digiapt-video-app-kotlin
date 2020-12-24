package com.digiapt.digiaptvideoapp.network.responses

import com.digiapt.digiaptvideoapp.models.Video

data class VideoResponse (
    val status: Boolean,
    val message: String,
    val data: Video
)