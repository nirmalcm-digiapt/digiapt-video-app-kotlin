package com.digiapt.digiaptvideoapp.models

import com.google.gson.annotations.SerializedName

data class VideoTemp(
    @SerializedName("id")
    val _id: String,
    @SerializedName("videoid")
    val videoid: String,
    @SerializedName("movie_id")
    val movie_id: String,
    @SerializedName("movie_name")
    val movie_name: String,
    @SerializedName("movie_image_path")
    val movie_image_path: String,
    @SerializedName("movie_video_path")
    val movie_video_path: String,
    @SerializedName("movie_match")
    val movie_match: String,
    @SerializedName("movie_year")
    val movie_year: String,
    @SerializedName("movie_certificate")
    val movie_certificate: String,
    @SerializedName("movie_duration")
    val movie_duration: String,
    @SerializedName("movie_description")
    val movie_description: String,
    @SerializedName("movie_starring")
    val movie_starring: String,
    @SerializedName("movie_director")
    val movie_director: String,
    @SerializedName("tags")
    val tags: List<String>
)