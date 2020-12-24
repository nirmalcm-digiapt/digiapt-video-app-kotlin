package com.digiapt.digiaptvideoapp.network.requests

import com.digiapt.digiaptvideoapp.models.Section
import com.google.gson.annotations.SerializedName

data class OtpOfflineRequest (
    @SerializedName("data")
    val data: List<Section>
)