package com.digiapt.digiaptvideoapp.network.responses

import com.digiapt.digiaptvideoapp.models.Section
import com.google.gson.annotations.SerializedName

data class VdocipherVideoResponse (
    @SerializedName("id")
    val id : String,
    @SerializedName("title")
    val title : String,
    @SerializedName("poster")
    val poster : String
)