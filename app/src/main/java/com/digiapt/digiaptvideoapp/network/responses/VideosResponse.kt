package com.digiapt.digiaptvideoapp.network.responses

import com.digiapt.digiaptvideoapp.models.Video
import com.google.gson.annotations.SerializedName

data class VideosResponse (
    @SerializedName("count")
    val count: String,
    @SerializedName("rows")
    val data: List<Video>
)