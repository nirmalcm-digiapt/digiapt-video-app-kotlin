package com.digiapt.digiaptvideoapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.digiapt.digiaptvideoapp.models.Video
import kotlinx.coroutines.Job
import net.digiapt.util.Coroutines
import com.digiapt.digiaptvideoapp.network.responses.VideoOTPResponse
import com.digiapt.digiaptvideoapp.repositories.VdocipherRepository


class VdocipherViewModel(
    private val repository: VdocipherRepository
) : ViewModel() {

    private lateinit var coroutineJob: Job

    private val _videoInfo = MutableLiveData<VideoOTPResponse>()
    val videoInfo: LiveData<VideoOTPResponse> get() = _videoInfo

    private val _videos = MutableLiveData<List<Video>>()
    val videos: LiveData<List<Video>> get() = _videos

    fun getVideoOTP(videoId : String){
        coroutineJob = Coroutines.ioThenMain(
            { repository.getVideoOTP(videoId) },
            { _videoInfo.value = it }
        )
    }

    fun getVideoOTPOffline(videoId : String,postValue : String){
        coroutineJob = Coroutines.ioThenMain(
            { repository.getVideoOTPoffline(videoId,postValue) },
            { _videoInfo.value = it }
        )
    }

    fun getVideos(folderPath : String) {
//        coroutineJob = Coroutines.ioThenMain(
//            { repository.getVideos(folderPath) },
//            { _videos.value = it?.rows}
//        )
    }
}