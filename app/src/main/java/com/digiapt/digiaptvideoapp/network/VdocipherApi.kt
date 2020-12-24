package com.digiapt.digiaptvideoapp.network

import com.digiapt.digiaptvideoapp.network.responses.*
import com.digiapt.digiaptvideoapp.util.Constants
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface VdocipherApi {

    @GET("videos/{video_id}/otp")
    suspend fun getVideoOTP(@Path("video_id") videoId : String) : Response<VideoOTPResponse>

    @Headers("Content-Type: application/json")
    @POST("videos/{video_id}/otp")
    fun getVideoOTPOffline(@Path("video_id") videoId : String,@Body body: String): Response<VideoOTPResponse>

    @GET("videos{folder_path}")
    suspend fun getVideos(@Path("folder_path",encoded = true) folderPath : String) : Response<VdocipherAPIVideosResponse>

    companion object{
        operator fun invoke() : VdocipherApi {

            val httpClient = OkHttpClient.Builder()
            httpClient.addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()

                builder.addHeader(Constants.VDOCIPHER_AUTHORIZATION_KEY,Constants.VDOCIPHER_AUTHORIZATION_VALUE)

                val request = builder.build()
                chain.proceed(request)
            }

//            val interceptor = HttpLoggingInterceptor()
//            val logLevel = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
//            interceptor.level = logLevel//BODY->to show logs//NONE->viceversa
//
//            httpClient.addInterceptor(interceptor)
            httpClient.connectTimeout(20, TimeUnit.SECONDS)
            httpClient.readTimeout(30, TimeUnit.SECONDS)

            val client = httpClient.build()


            return Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Constants.BASE_URL_VDOCIPHER)
                .client(client)
                .build()
                .create(VdocipherApi::class.java)
        }
    }
}