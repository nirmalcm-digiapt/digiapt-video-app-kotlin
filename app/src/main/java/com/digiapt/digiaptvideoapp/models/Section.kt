package com.digiapt.digiaptvideoapp.models

import com.google.gson.annotations.SerializedName

data class Section(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val section_name: String,
    @SerializedName("rows")
    val section_videos: List<Video>
)