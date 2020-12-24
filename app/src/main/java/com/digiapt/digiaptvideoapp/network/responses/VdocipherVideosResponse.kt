package com.digiapt.digiaptvideoapp.network.responses

import com.digiapt.digiaptvideoapp.models.Section
import com.google.gson.annotations.SerializedName

data class VdocipherVideosResponse (
    @SerializedName("count")
    val count: String,
    @SerializedName("rows")
    val rows: List<VdocipherVideoResponse>
)