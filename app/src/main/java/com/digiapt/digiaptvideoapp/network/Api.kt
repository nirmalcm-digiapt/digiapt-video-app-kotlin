package com.digiapt.digiaptvideoapp.network

import com.digiapt.digiaptvideoapp.network.responses.*
import com.digiapt.digiaptvideoapp.util.Constants
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface Api {

    @GET("{sub_category_id}")
    suspend fun getSections(@Path("sub_category_id",encoded = true) sub_category_id : String) : Response<SectionsResponse>

//    @GET("{sub_category_id}")
//    suspend fun getSectionsTemp(@Path("sub_category_id",encoded = true) sub_category_id : String) : Response<List<SectionTemp>>

    @GET("{sub_category_id}")
    suspend fun getVideoHighlight(@Path("sub_category_id",encoded = true) sub_category_id : String) : Response<VideoResponse>

    @GET("{sub_category_id}")
    suspend fun getHighlight(@Path("sub_category_id",encoded = true) sub_category_id : String) : Response<VideoResponse>

//    @GET("{sub_category_id}")
//    suspend fun getVideoHighlightTemp(@Path("sub_category_id",encoded = true) sub_category_id : String) : Response<VideoResponseTemp>

    companion object{
        operator fun invoke() : Api {
            return Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Constants.BASE_URL)
                .build()
                .create(Api::class.java)
        }
    }
}
