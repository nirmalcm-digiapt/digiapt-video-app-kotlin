package com.digiapt.digiaptvideoapp.network.responses

import com.digiapt.digiaptvideoapp.models.SectionTemp
import com.google.gson.annotations.SerializedName

data class SectionsResponseTemp (
    @SerializedName("data")
    val data: List<SectionTemp>
)