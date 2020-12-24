package com.digiapt.digiaptvideoapp.network.responses

import com.digiapt.digiaptvideoapp.models.Section
import com.google.gson.annotations.SerializedName

data class VdocipherAPIVideosResponse (
    @SerializedName("count")
    val count: String,
    @SerializedName("rows")
    val rows: List<VdocipherVideoResponse>
)