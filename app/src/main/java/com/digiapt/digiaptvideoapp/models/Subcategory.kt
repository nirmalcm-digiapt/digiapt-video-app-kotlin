package com.digiapt.digiaptvideoapp.models

import com.google.gson.annotations.SerializedName

data class Subcategory(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val subcategory_name: String
)