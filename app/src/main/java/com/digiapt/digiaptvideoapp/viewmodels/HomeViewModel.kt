package com.digiapt.digiaptvideoapp.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.digiapt.digiaptvideoapp.database.entities.OfflineVideo
import com.digiapt.digiaptvideoapp.models.Section
import com.digiapt.digiaptvideoapp.models.SectionTemp
import com.digiapt.digiaptvideoapp.models.Video
import com.digiapt.digiaptvideoapp.models.VideoTemp
import com.digiapt.digiaptvideoapp.repositories.HomeRepository
import com.example.mvvm.util.lazyDeferred
import com.loopj.android.http.AsyncHttpClient.log
import net.digiapt.util.Coroutines
import java.lang.Exception
import com.digiapt.digiaptvideoapp.network.NetworkSafeApiRequest
import com.digiapt.digiaptvideoapp.network.responses.VdocipherAPIVideosResponse
import com.digiapt.digiaptvideoapp.network.responses.VdocipherVideoResponse
import com.digiapt.digiaptvideoapp.network.responses.VdocipherVideosResponse
import com.digiapt.digiaptvideoapp.network.responses.VideoResponse
import com.digiapt.digiaptvideoapp.util.Constants
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import org.json.JSONObject

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private lateinit var coroutineJob: Job

    private val _sections = MutableLiveData<List<Section>>()
    val sections: LiveData<List<Section>> get() = _sections

    private val _videoHighlight = MutableLiveData<Video>()
    val videoHighlight: LiveData<Video> get() = _videoHighlight

    fun saveOfflineVideo(offlineVideo: OfflineVideo) = lazyDeferred{
        repository.saveOfflineVideo(offlineVideo)
    }

    //lateinit var offlineVideo:OfflineVideo
    fun getOfflineVideo(videoId:String) : OfflineVideo {
        lazyDeferred {
            repository.getOfflineVideo(videoId)
        }
        return OfflineVideo(mediaId = videoId)
    }

    fun getSections(subCategoryId : String){
        Log.d("ncm vdocipher videos","New videos")
        coroutineJob = Coroutines.ioThenMain(
            {
//                Log.d("ncm vdocipher videos","New videos")
//                val response : VdocipherAPIVideosResponse
//                try {
//                    response = repository.getSectionsVdocipher(subCategoryId)
//                    _sections.value = response?.toNewSections()
//                    Log.d("ncm vdocipher videos",response.toString())
//                } catch (e: Exception) {
//                    Log.d("ncm vdocipher videos",e.toString())
//                    e.printStackTrace()
//                }
                repository.getSectionsVdocipher(subCategoryId)
            },
            {
                Log.d("ncm vdocipher videos","New videos response "+it?.toString())
                _sections.value = it?.toNewSections()
            }
        )
    }

    fun VdocipherAPIVideosResponse.toNewSections() : List<Section>{
        var sections : List<Section> = listOf(

            Section("1","Trending",this.rows.convertToVideos()),
            Section("2","Movies",this.rows.convertToVideos())
        )
        Log.d("ncm Resp",this.toString())

        return sections
    }

    fun List<VdocipherVideoResponse>.convertToVideos() : List<Video> {
        return this.map {
            it.convertToVideo()
        }
    }

    fun VdocipherVideoResponse.convertToVideo() : Video {
        return Video(this.id,this.title,"",this.poster, listOf("Adventure,Drama"))
    }
    fun getVideoHighlight(subCategoryId : String){
        coroutineJob = Coroutines.ioThenMain(
            {
                Log.d("ncm CHECK","REACHING FIRST BLOCK")
                repository.getVideoHighlightVdocipher(subCategoryId)
            },
            {
                Log.d("ncm CHECK","REACHING SECOND BLOCK")
                _videoHighlight.value = it?.convertToHighlightVideo()
            }
        )
    }

    fun VdocipherAPIVideosResponse.convertToHighlightVideo() : Video {
        return this.rows.get(0).convertToVideo()
    }

    fun List<SectionTemp>.toSections() : List<Section>{
        return this.map {
            it.toSection()
        }
    }

    fun SectionTemp.toSection() : Section{
        return Section(this.id,this.section_name,this.section_videos.toVideos())
    }

    fun List<VideoTemp>.toVideos() : List<Video>{
        return this.map {
            it.toVideo()
        }
    }

    fun VideoTemp.toVideo() : Video{
        Log.d("movie",this.videoid)
        Log.d("movie",this.movie_name)
        Log.d("movie",this.movie_description)
        Log.d("movie",this.movie_image_path)

        val tags = listOf("Action", "Thriller", "Drama")

        return Video(this.videoid,this.movie_name,this.movie_description,this.movie_image_path,tags)
    }

    override fun onCleared() {
        super.onCleared()
        if(::coroutineJob.isInitialized) coroutineJob.cancel()
    }
}