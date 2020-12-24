package com.digiapt.digiaptvideoapp.network.responses

import com.digiapt.digiaptvideoapp.models.Section
import com.google.gson.annotations.SerializedName

data class SectionsResponse (
    @SerializedName("data")
    val data: List<Section>
)