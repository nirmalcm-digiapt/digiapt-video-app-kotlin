package com.digiapt.digiaptvideoapp.repositories

import com.digiapt.digiaptvideoapp.network.VdocipherApi
import com.example.mvvm.data.network.SafeApiRequest

class VdocipherRepository(
    private val vdocipherApi : VdocipherApi
) : SafeApiRequest() {

    suspend fun getVideoOTP(videoId : String) = apiRequest { vdocipherApi.getVideoOTP(videoId)}

    suspend fun getVideoOTPoffline(videoId : String, postValue : String) = apiRequest { vdocipherApi.getVideoOTPOffline(videoId,postValue)}

    suspend fun getVideos(folderPath : String)= apiRequest { vdocipherApi.getVideos(folderPath)}
}