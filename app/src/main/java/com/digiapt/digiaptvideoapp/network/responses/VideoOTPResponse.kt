package com.digiapt.digiaptvideoapp.network.responses

import com.google.gson.annotations.SerializedName

data class VideoOTPResponse (
    @SerializedName("otp")
    val otp: String,
    @SerializedName("playbackInfo")
    val playbackInfo: String
)