package com.digiapt.digiaptvideoapp.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.digiapt.digiaptvideoapp.database.AppDatabase
import com.digiapt.digiaptvideoapp.database.entities.OfflineVideo
import com.digiapt.digiaptvideoapp.models.Video
import com.digiapt.digiaptvideoapp.network.Api
import com.digiapt.digiaptvideoapp.network.VdocipherApi
import com.digiapt.digiaptvideoapp.network.responses.VideoResponse
import com.digiapt.digiaptvideoapp.preferences.PreferenceProvider
import com.example.mvvm.data.network.SafeApiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

class HomeRepository(
    private val api: Api,
    private val vdocipherApi: VdocipherApi,
    private val db: AppDatabase,
    private val prefs: PreferenceProvider
) : SafeApiRequest() {

    suspend fun getSectionsVdocipher(subCategoryId : String) = apiRequest {
        vdocipherApi.getVideos(subCategoryId)
    }

    suspend fun getSections(subCategoryId : String) = apiRequest { api.getSections(subCategoryId) }

    suspend fun getVideoHighlight(subCategoryId : String) = apiRequest {
        Log.d("ncm CHECK","Inside video highlight API")
        api.getVideoHighlight(subCategoryId)
    }

    suspend fun getVideoHighlightVdocipher(subCategoryId : String) = apiRequest {
        vdocipherApi.getVideos(subCategoryId)
    }

    private val highlight = MutableLiveData<VideoResponse>()

    init {
        highlight.observeForever {
        }
    }

    suspend fun getHighlight(subCategoryId : String): LiveData<VideoResponse> {
        return withContext(Dispatchers.IO) {
            Log.d("ncm","repo getHighlight")
            getQuotes(subCategoryId)
        }
    }

    val highlightLive: LiveData<VideoResponse> get() = highlight

    private suspend fun getQuotes(subCategoryId: String): LiveData<VideoResponse> {

        try {
            val response = apiRequest { api.getHighlight(subCategoryId) }
            Log.d("ncm",response.toString())
            highlight.postValue(response)
        } catch (e: Exception) {
            Log.d("ncm",e.toString())
            e.printStackTrace()
        }
        return highlightLive
    }

    suspend fun getOfflineVideo(video_id:String) : OfflineVideo {
        return withContext(Dispatchers.Main) {
            db.getOfflineVideoDao().getOfflineVideo(video_id)
        }
    }

    suspend fun saveOfflineVideo(offlineVideo:OfflineVideo) {
        withContext(Dispatchers.IO) {
            db.getOfflineVideoDao().saveOfflineVideo(offlineVideo)
        }
    }

}