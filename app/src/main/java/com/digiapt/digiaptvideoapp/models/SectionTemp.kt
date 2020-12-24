package com.digiapt.digiaptvideoapp.models

import com.google.gson.annotations.SerializedName

data class SectionTemp(
    @SerializedName("_id")
    val id: String,
    @SerializedName("title")
    val section_name: String,
    @SerializedName("movie_list")
    val section_videos: List<VideoTemp>
)