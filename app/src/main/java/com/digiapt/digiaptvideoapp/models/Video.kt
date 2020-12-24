package com.digiapt.digiaptvideoapp.models

import com.google.gson.annotations.SerializedName

data class Video(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("poster")
    val poster: String,
    @SerializedName("tags")
    val tags: List<String>
)